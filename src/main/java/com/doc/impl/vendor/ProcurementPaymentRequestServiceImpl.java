package com.doc.impl.vendor;

import com.doc.calculation.vendor.ProcurementPaymentCalculation;
import com.doc.calculation.vendor.ProcurementPaymentCalculator;
import com.doc.dto.vendor.*;
import com.doc.entity.project.Project;
import com.doc.entity.vendor.*;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.feign.AccountFeignClient;
import com.doc.repository.UserRepository;
import com.doc.repository.vendor.ProcurementPaymentRequestRepository;
import com.doc.repository.vendor.PurchaseOrderRepository;
import com.doc.repository.vendor.VendorAccountsSubmissionRepository;
import com.doc.repository.vendor.VendorFinalizationRepository;
import com.doc.service.vendor.ProcurementPaymentRequestService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static com.doc.dto.vendor.ProcurementPaymentRequestResponseDto.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementPaymentRequestServiceImpl
        implements ProcurementPaymentRequestService {

    private static final int MAX_PAGE_SIZE = 200;

    private final ProcurementPaymentRequestRepository paymentRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final VendorAccountsSubmissionRepository vendorAccountsSubmissionRepository;
    private final VendorFinalizationRepository vendorFinalizationRepository;
    private final AccountFeignClient accountFeignClient;
    private final ProcurementPaymentCalculator calculator;

    /**
     * SERIALIZABLE is intentional here: two concurrent requests must not both
     * pass the PO remaining-taxable-value check.
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ProcurementPaymentRequestResponseDto createPaymentRequest(
            Long procurementOrderId,
            ProcurementPaymentRequestDto request
    ) {
        if (procurementOrderId == null) {
            throw validation(
                    "Procurement order id is required",
                    "ERR_PROCUREMENT_ORDER_ID_REQUIRED"
            );
        }
        if (request == null) {
            throw validation(
                    "Payment request body is required",
                    "ERR_PAYMENT_REQUEST_BODY_REQUIRED"
            );
        }
        validateUser(request.getCreatedBy());

        ProcurementOrder order = getActiveOrder(procurementOrderId);
        validateOrderEligibleForPaymentRequest(order);

        Vendor vendor = order.getVendor();
        validateActiveVendor(vendor);

        validateTaxRatesAgainstApprovedPo(order, request);

        ProcurementPaymentCalculation calculation =
                calculator.calculateFromInvoiceGross(
                        request.getInvoiceAmount(),
                        request.getGstActive(),
                        request.getGstType(),
                        request.getGstPercentage(),
                        request.getTdsActive(),
                        request.getTdsPercentage(),
                        vendor.getGstRegistrationType()
                );

        validatePoRemainingTaxableAmount(order, calculation.getTaxableAmount());
        logIgnoredClientCalculatedValues(request, calculation);

        ProcurementPaymentRequest entity = new ProcurementPaymentRequest();
        entity.setProcurementOrder(order);
        entity.setProject(order.getProject());
        entity.setVendor(vendor);
        entity.setCreatedBy(request.getCreatedBy());
        entity.setSubmissionDate(new Date());
        entity.setStatus(PaymentRequestStatus.PENDING);
        entity.setDeleted(false);
        entity.setCompletionRemarks(clean(request.getCompletionRemarks()));
        entity.setGstStateCode(clean(request.getGstStateCode()));

        if (request.getProofAttachmentUrls() != null) {
            entity.setProofAttachmentUrls(request.getProofAttachmentUrls());
        }

        if (hasText(request.getInvoiceNumber())) {
            String invoiceNumber = request.getInvoiceNumber().trim();
            validateInvoiceNumberUnique(vendor.getId(), invoiceNumber, null);
            entity.setInvoiceNumber(invoiceNumber);
        }
        entity.setInvoiceDate(request.getInvoiceDate());

        applyCalculation(entity, calculation);

        ProcurementPaymentRequest saved = paymentRequestRepository.save(entity);

        log.info(
                "[PROCUREMENT-PAYMENT-CREATED] requestId={} | poId={} | vendorId={} | "
                        + "taxable={} | gst={} | invoiceGross={} | tds={} | netPayable={} | version={}",
                saved.getId(),
                order.getId(),
                vendor.getId(),
                saved.getAmount(),
                saved.getTotalGstAmount(),
                saved.getInvoiceAmount(),
                saved.getTdsAmount(),
                saved.getPayableAmount(),
                saved.getCalculationVersion()
        );

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementPaymentRequestResponseDto> getPaymentRequestsByStatus(
            PaymentRequestStatus status,
            int page,
            int size
    ) {
        Pageable pageable = pageable(page, size);

        Page<ProcurementPaymentRequest> result = status == null
                ? paymentRequestRepository.findByIsDeletedFalse(pageable)
                : paymentRequestRepository.findByStatusAndIsDeletedFalse(
                status,
                pageable
        );

        return result.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementPaymentRequestResponseDto>
    getPaymentRequestsByProcurementOrderId(
            Long procurementOrderId,
            int page,
            int size
    ) {
        if (procurementOrderId == null) {
            throw validation(
                    "Procurement order id is required",
                    "ERR_PROCUREMENT_ORDER_ID_REQUIRED"
            );
        }

        getActiveOrder(procurementOrderId);

        return paymentRequestRepository
                .findByProcurementOrder_IdAndIsDeletedFalse(
                        procurementOrderId,
                        pageable(page, size)
                )
                .map(this::mapToResponse);
    }

    /** Approval is Operation-only. No Account Service call occurs here. */
    @Override
    @Transactional
    public ProcurementPaymentRequestResponseDto approvePaymentRequest(
            Long paymentRequestId,
            Long userId,
            ProcurementPaymentActionRequestDto request
    ) {
        validateUser(userId);
        ProcurementPaymentRequest entity = getActivePaymentRequestForUpdate(
                paymentRequestId
        );

        if (entity.getStatus() != PaymentRequestStatus.PENDING
                && entity.getStatus() != PaymentRequestStatus.UNDER_REVIEW) {
            throw invalidStatus(
                    "Only PENDING or UNDER_REVIEW payment request can be approved",
                    entity.getStatus()
            );
        }

        validateActiveVendor(entity.getVendor());
        refreshCalculation(entity);

        if (request != null) {
            updateInvoiceDetailsIfSupplied(entity, request);
            String comment = firstNonBlank(request.getRemarks(), request.getComment());
            if (hasText(comment)) {
                entity.setCompletionRemarks(comment.trim());
            }
        }

        Date now = new Date();
        entity.setStatus(PaymentRequestStatus.APPROVED);
        entity.setApprovedBy(userId);
        entity.setApprovedDate(now);
        entity.setUpdatedDate(now);

        ProcurementPaymentRequest saved = paymentRequestRepository.save(entity);

        log.info(
                "[PROCUREMENT-PAYMENT-APPROVED] requestId={} | approvedBy={} | netPayable={}",
                saved.getId(),
                userId,
                saved.getPayableAmount()
        );

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ProcurementPaymentRequestResponseDto rejectPaymentRequest(
            Long paymentRequestId,
            Long userId,
            ProcurementPaymentActionRequestDto request
    ) {
        validateUser(userId);
        ProcurementPaymentRequest entity = getActivePaymentRequestForUpdate(
                paymentRequestId
        );

        if (entity.getStatus() != PaymentRequestStatus.PENDING
                && entity.getStatus() != PaymentRequestStatus.UNDER_REVIEW) {
            throw invalidStatus(
                    "Only PENDING or UNDER_REVIEW payment request can be rejected",
                    entity.getStatus()
            );
        }

        String reason = request != null ? request.getReason() : null;
        if (!hasText(reason)) {
            throw validation(
                    "Rejection reason is required",
                    "ERR_REJECTION_REASON_REQUIRED"
            );
        }

        Date now = new Date();
        entity.setStatus(PaymentRequestStatus.REJECTED);
        entity.setApprovedBy(userId);
        entity.setApprovedDate(now);
        entity.setCompletionRemarks(reason.trim());
        entity.setUpdatedDate(now);

        ProcurementPaymentRequest saved = paymentRequestRepository.save(entity);

        log.info(
                "[PROCUREMENT-PAYMENT-REJECTED] requestId={} | rejectedBy={}",
                saved.getId(),
                userId
        );

        return mapToResponse(saved);
    }

    /**
     * Full-settlement release.
     *
     * Operation Service recalculates and freezes the snapshot, validates any UI
     * confirmation, sends the canonical snapshot to Account Service, and marks
     * PAYMENT_RELEASED only after both vouchers are confirmed.
     */
    @Override
    @Transactional
    public ProcurementPaymentRequestResponseDto releasePayment(
            Long paymentRequestId,
            Long userId,
            ProcurementPaymentActionRequestDto request
    ) {
        validateUser(userId);

        if (request == null) {
            throw validation(
                    "Payment release request is required",
                    "ERR_PAYMENT_RELEASE_REQUEST_REQUIRED"
            );
        }

        ProcurementPaymentRequest entity = getActivePaymentRequestForUpdate(
                paymentRequestId
        );

        if (entity.getStatus() != PaymentRequestStatus.APPROVED
                && entity.getStatus() != PaymentRequestStatus.PAYMENT_PROCESSING) {
            throw invalidStatus(
                    "Only APPROVED or PAYMENT_PROCESSING payment request can be released",
                    entity.getStatus()
            );
        }

        validateActiveVendor(entity.getVendor());
        updateAndRequireInvoiceDetails(entity, request);

        ProcurementPaymentCalculation calculation = refreshCalculation(entity);
        validateReleaseTaxConfirmation(request, calculation);

        BigDecimal calculatedBankPayment =
                calculation.getVendorNetPayableAmount();

        calculator.assertOptionalMoneyMatches(
                "Bank payment amount",
                request.getBankPaymentAmount(),
                calculatedBankPayment,
                "ERR_BANK_PAYMENT_AMOUNT_MISMATCH"
        );

        if (request.getBankLedgerId() == null
                || request.getBankLedgerId() <= 0) {
            throw validation(
                    "Bank/Cash ledger id is required for payment release",
                    "ERR_BANK_LEDGER_REQUIRED"
            );
        }

        String paymentMode = normalizePaymentMode(request.getPaymentMode());
        String transactionReference = clean(request.getTransactionReference());

        if (!"CASH".equals(paymentMode) && !hasText(transactionReference)) {
            throw validation(
                    "Transaction reference is required for non-cash payment",
                    "ERR_TRANSACTION_REFERENCE_REQUIRED"
            );
        }

        LocalDate paymentDate = request.getPaymentDate() != null
                ? request.getPaymentDate()
                : LocalDate.now();

        entity.setBankPaymentAmount(calculatedBankPayment);
        entity.setPaymentDate(paymentDate);
        entity.setPaymentMode(paymentMode);
        entity.setBankLedgerId(request.getBankLedgerId());
        entity.setTransactionReference(transactionReference);
        entity.setPaymentProof(clean(request.getPaymentProof()));

        if (request.getLedgerId() != null) {
            entity.setLedgerId(request.getLedgerId());
        }
        if (hasText(request.getLedgerType())) {
            entity.setLedgerType(request.getLedgerType().trim());
        }
        if (request.getProofAttachmentUrls() != null) {
            entity.setProofAttachmentUrls(request.getProofAttachmentUrls());
        }

        String releaseComment = firstNonBlank(
                request.getRemarks(),
                request.getComment()
        );
        if (hasText(releaseComment)) {
            entity.setCompletionRemarks(releaseComment.trim());
        }

        Date now = new Date();
        entity.setPaymentReleasedBy(userId);
        entity.setUpdatedDate(now);
        entity.setStatus(PaymentRequestStatus.PAYMENT_PROCESSING);

        ProcurementPaymentRequest processing =
                paymentRequestRepository.saveAndFlush(entity);

        AccountVendorSyncResponseDto accountResponse =
                syncVendorWithAccountService(
                        processing,
                        userId,
                        request
                );

        validateAccountingResult(accountResponse);

        processing.setStatus(PaymentRequestStatus.PAYMENT_RELEASED);
        processing.setPaymentReleasedDate(new Date());
        processing.setUpdatedDate(new Date());

        ProcurementPaymentRequest released =
                paymentRequestRepository.save(processing);

        log.info(
                "[PROCUREMENT-PAYMENT-RELEASED] requestId={} | invoiceGross={} | "
                        + "bankPayment={} | tds={} | settlement={} | "
                        + "invoiceVoucherId={} | paymentVoucherId={}",
                released.getId(),
                released.getInvoiceAmount(),
                released.getBankPaymentAmount(),
                released.getTdsAmount(),
                released.getBankPaymentAmount().add(released.getTdsAmount()),
                accountResponse.getVoucherId(),
                accountResponse.getPaymentVoucherId()
        );

        return mapToResponse(released);
    }

    private AccountVendorSyncResponseDto syncVendorWithAccountService(
            ProcurementPaymentRequest paymentRequest,
            Long operationUserId,
            ProcurementPaymentActionRequestDto actionRequest
    ) {
        Vendor vendor = paymentRequest.getVendor();
        if (vendor == null || vendor.getId() == null) {
            throw validation(
                    "Vendor is not available against payment request",
                    "ERR_PAYMENT_REQUEST_VENDOR_NOT_FOUND"
            );
        }

        VendorAccountsSubmission accountsSubmission =
                vendorAccountsSubmissionRepository
                        .findFirstByVendor_IdOrderByIdDesc(vendor.getId())
                        .orElseThrow(() -> validation(
                                "Vendor accounts submission not found for vendor ID: "
                                        + vendor.getId(),
                                "ERR_VENDOR_ACCOUNTS_SUBMISSION_NOT_FOUND"
                        ));

        if (accountsSubmission.getStatus()
                != VendorAccountsSubmissionStatus.APPROVED) {
            throw validation(
                    "Latest vendor accounts submission must be APPROVED",
                    "ERR_VENDOR_ACCOUNTS_NOT_APPROVED"
            );
        }

        if (accountsSubmission.isDeleted()) {
            throw validation(
                    "Latest vendor accounts submission is deleted",
                    "ERR_VENDOR_ACCOUNTS_SUBMISSION_DELETED"
            );
        }

        VendorGSTRegistrationType accountsRegistrationType =
                accountsSubmission.getGstRegistrationType();
        if (accountsRegistrationType != null
                && vendor.getGstRegistrationType() != null
                && accountsRegistrationType != vendor.getGstRegistrationType()) {
            throw validation(
                    "Vendor GST registration type differs from the approved accounts submission",
                    "ERR_VENDOR_GST_REGISTRATION_TYPE_MISMATCH"
            );
        }

        VendorFinalization finalization =
                accountsSubmission.getVendorFinalization() != null
                        ? accountsSubmission.getVendorFinalization()
                        : vendorFinalizationRepository
                        .findFirstByVendor_IdOrderByIdDesc(vendor.getId())
                        .orElseThrow(() -> validation(
                                "Vendor finalization not found for vendor ID: "
                                        + vendor.getId(),
                                "ERR_VENDOR_FINALIZATION_NOT_FOUND"
                        ));

        if (finalization.isDeleted()) {
            throw validation(
                    "Vendor finalization is deleted",
                    "ERR_VENDOR_FINALIZATION_DELETED"
            );
        }

        String gstRegistrationType =
                accountsSubmission.getGstRegistrationType() != null
                        ? accountsSubmission.getGstRegistrationType().name()
                        : vendor.getGstRegistrationType() != null
                        ? vendor.getGstRegistrationType().name()
                        : null;

        VendorPaymentApprovalRequestDto paymentApproval =
                buildPaymentReleaseRequest(
                        paymentRequest,
                        operationUserId,
                        actionRequest
                );
        paymentApproval.setGstRegistrationType(gstRegistrationType);

        String gstNumber = hasText(accountsSubmission.getGstNumber())
                ? accountsSubmission.getGstNumber().trim()
                : clean(vendor.getGstNumber());

        LocalDateTime now = LocalDateTime.now();

        AccountVendorSyncRequestDto syncRequest =
                AccountVendorSyncRequestDto.builder()
                        .operationVendorId(vendor.getId())
                        .vendorAccountsSubmissionId(accountsSubmission.getId())
                        .vendorFinalizationId(finalization.getId())
                        .vendorName(vendor.getName())
                        .email(vendor.getEmail())
                        .mobile(vendor.getMobile())
                        .pan(vendor.getPanNumber())
                        .gstNumber(gstNumber)
                        .gstRegistrationType(gstRegistrationType)
                        .accountHolderName(accountsSubmission.getAccountHolderName())
                        .bankAccountNumber(accountsSubmission.getAccountNumber())
                        .ifscCode(accountsSubmission.getIfsc())
                        .bankName(null)
                        .branchAddress(accountsSubmission.getBranchAddress())
                        .fullAddress(
                                hasText(vendor.getFullAddress())
                                        ? vendor.getFullAddress().trim()
                                        : clean(accountsSubmission.getBranchAddress())
                        )
                        .city(vendor.getCity())
                        .state(vendor.getState())
                        .country(vendor.getCountry())
                        .active(vendor.getStatus() == VendorStatus.ACTIVE)
                        .approvedByOperationUserId(
                                paymentRequest.getApprovedBy() != null
                                        ? paymentRequest.getApprovedBy()
                                        : operationUserId
                        )
                        .approvedAt(
                                paymentRequest.getApprovedDate() != null
                                        ? toLocalDateTime(paymentRequest.getApprovedDate())
                                        : now
                        )
                        .operationUpdatedAt(now)
                        .paymentApproval(paymentApproval)
                        .build();

        try {
            AccountVendorSyncResponseDto response =
                    accountFeignClient.syncVendor(syncRequest);

            if (response == null) {
                throw validation(
                        "Account Service returned an empty response",
                        "ERR_EMPTY_ACCOUNT_VENDOR_SYNC_RESPONSE"
                );
            }

            if (!"SUCCESS".equalsIgnoreCase(response.getSyncStatus())) {
                throw validation(
                        "Account Service synchronization failed: "
                                + response.getMessage(),
                        "ERR_ACCOUNT_VENDOR_SYNC_UNSUCCESSFUL"
                );
            }

            return response;
        } catch (FeignException exception) {
            log.error(
                    "[ACCOUNT-SYNC-FAILED] requestId={} | status={} | response={}",
                    paymentRequest.getId(),
                    exception.status(),
                    exception.contentUTF8(),
                    exception
            );
            throw validation(
                    extractAccountServiceError(exception),
                    "ERR_ACCOUNT_VENDOR_SYNC_FAILED"
            );
        }
    }

    private VendorPaymentApprovalRequestDto buildPaymentReleaseRequest(
            ProcurementPaymentRequest entity,
            Long operationUserId,
            ProcurementPaymentActionRequestDto actionRequest
    ) {
        BigDecimal settlementAmount = entity.getBankPaymentAmount()
                .add(entity.getTdsAmount())
                .setScale(2, java.math.RoundingMode.HALF_UP);

        return VendorPaymentApprovalRequestDto.builder()
                .procurementPaymentRequestId(entity.getId())
                .procurementOrderId(
                        entity.getProcurementOrder() != null
                                ? entity.getProcurementOrder().getId()
                                : null
                )
                .purchaseOrderNumber(
                        entity.getProcurementOrder() != null
                                ? entity.getProcurementOrder().getPoNumber()
                                : null
                )
                .invoiceNumber(entity.getInvoiceNumber())
                .invoiceDate(entity.getInvoiceDate())
                .price(entity.getAmount())
                .taxableAmount(entity.getAmount())
                .gstRegistrationType(
                        entity.getVendor() != null
                                && entity.getVendor().getGstRegistrationType() != null
                                ? entity.getVendor().getGstRegistrationType().name()
                                : null
                )
                .gstActive(entity.getGstActive())
                .gstSupplyType(entity.getGstType())
                .gstStateCode(entity.getGstStateCode())
                .gstPercentage(entity.getGstPercentage())
                .cgstAmount(entity.getCgstAmount())
                .sgstAmount(entity.getSgstAmount())
                .igstAmount(entity.getIgstAmount())
                .totalGstAmount(entity.getTotalGstAmount())
                .invoiceGrossAmount(entity.getInvoiceAmount())
                .tdsActive(entity.getTdsActive())
                .tdsBaseAmount(entity.getAmount())
                .tdsPercentage(entity.getTdsPercentage())
                .tdsAmount(entity.getTdsAmount())
                .tdsPayableLedgerId(actionRequest.getTdsPayableLedgerId())
                .vendorNetPayableAmount(entity.getPayableAmount())
                .settlementAmount(settlementAmount)
                .paymentDate(entity.getPaymentDate())
                .bankPaymentAmount(entity.getBankPaymentAmount())
                .paymentMode(entity.getPaymentMode())
                .bankLedgerId(entity.getBankLedgerId())
                .ledgerId(entity.getLedgerId())
                .ledgerType(entity.getLedgerType())
                .transactionReference(entity.getTransactionReference())
                .paymentProof(entity.getPaymentProof())
                .proofAttachmentUrls(entity.getProofAttachmentUrls())
                .approvedByOperationUserId(entity.getApprovedBy())
                .approvedDate(toLocalDate(entity.getApprovedDate()))
                .approvalComment(clean(entity.getCompletionRemarks()))
                .paymentReleasedByOperationUserId(operationUserId)
                .paymentReleasedDate(entity.getPaymentDate())
                .releaseComment(clean(entity.getCompletionRemarks()))
                .calculationVersion(entity.getCalculationVersion())
                .build();
    }

    private ProcurementPaymentCalculation refreshCalculation(
            ProcurementPaymentRequest entity
    ) {
        ProcurementPaymentCalculation calculation =
                calculator.calculateFromInvoiceGross(
                        entity.getInvoiceAmount(),
                        entity.getGstActive(),
                        entity.getGstType(),
                        entity.getGstPercentage(),
                        entity.getTdsActive(),
                        entity.getTdsPercentage(),
                        entity.getVendor() != null
                                ? entity.getVendor().getGstRegistrationType()
                                : null
                );

        applyCalculation(entity, calculation);
        return calculation;
    }

    private void applyCalculation(
            ProcurementPaymentRequest entity,
            ProcurementPaymentCalculation calculation
    ) {
        entity.setAmount(calculation.getTaxableAmount());
        entity.setInvoiceAmount(calculation.getInvoiceGrossAmount());
        entity.setPayableAmount(calculation.getVendorNetPayableAmount());

        entity.setGstActive(calculation.getGstActive());
        entity.setGstType(calculation.getGstSupplyType());
        entity.setGstPercentage(calculation.getGstPercentage());
        entity.setCgstAmount(calculation.getCgstAmount());
        entity.setSgstAmount(calculation.getSgstAmount());
        entity.setIgstAmount(calculation.getIgstAmount());
        entity.setTotalGstAmount(calculation.getTotalGstAmount());

        entity.setTdsActive(calculation.getTdsActive());
        entity.setTdsPercentage(calculation.getTdsPercentage());
        entity.setTdsAmount(calculation.getTdsAmount());
        entity.setCalculationVersion(calculation.getCalculationVersion());
    }

    private void validateReleaseTaxConfirmation(
            ProcurementPaymentActionRequestDto request,
            ProcurementPaymentCalculation calculation
    ) {
        if (request.getTdsActive() != null
                && !Objects.equals(
                request.getTdsActive(),
                calculation.getTdsActive()
        )) {
            throw validation(
                    "TDS applicability cannot be changed during payment release",
                    "ERR_TDS_ACTIVE_MISMATCH"
            );
        }

        calculator.assertOptionalRateMatches(
                "TDS percentage",
                request.getTdsPercentage(),
                calculation.getTdsPercentage(),
                "ERR_TDS_PERCENTAGE_MISMATCH"
        );
        calculator.assertOptionalMoneyMatches(
                "TDS amount",
                request.getTdsAmount(),
                calculation.getTdsAmount(),
                "ERR_TDS_AMOUNT_MISMATCH"
        );
    }


    private void validateAccountingResult(
            AccountVendorSyncResponseDto response
    ) {
        if (response.getVoucherId() == null) {
            throw validation(
                    "Account Service did not return a purchase invoice voucher",
                    "ERR_PURCHASE_INVOICE_VOUCHER_NOT_CREATED"
            );
        }

        if (response.getPaymentVoucherId() == null) {
            throw validation(
                    "Account Service did not return a payment voucher",
                    "ERR_PAYMENT_VOUCHER_NOT_CREATED"
            );
        }
    }

    private void validateOrderEligibleForPaymentRequest(ProcurementOrder order) {
        if (order.getStatus() != ProcurementOrderStatus.APPROVED
                && order.getStatus() != ProcurementOrderStatus.COMPLETED) {
            throw validation(
                    "Only APPROVED or COMPLETED procurement order can be used for a payment request. Current status: "
                            + order.getStatus(),
                    "ERR_PROCUREMENT_ORDER_NOT_APPROVED"
            );
        }

        if (order.getFinalAmount() == null
                || order.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw validation(
                    "PO taxable/final amount must be greater than zero",
                    "ERR_PO_FINAL_AMOUNT_MISSING"
            );
        }
    }

    private void validatePoRemainingTaxableAmount(
            ProcurementOrder order,
            BigDecimal newTaxableAmount
    ) {
        BigDecimal reserved = calculator.money(
                paymentRequestRepository.sumReservedTaxableAmountByOrder(order)
        );
        BigDecimal poTaxable = calculator.money(order.getFinalAmount());
        BigDecimal after = calculator.money(reserved.add(newTaxableAmount));

        if (after.compareTo(poTaxable) > 0) {
            throw validation(
                    "Payment request taxable amount exceeds PO balance. PO taxable: "
                            + poTaxable.toPlainString()
                            + ", already reserved: "
                            + reserved.toPlainString()
                            + ", new taxable: "
                            + newTaxableAmount.toPlainString(),
                    "ERR_PAYMENT_REQUEST_TAXABLE_AMOUNT_EXCEEDS_PO"
            );
        }
    }

    private void validateTaxRatesAgainstApprovedPo(
            ProcurementOrder order,
            ProcurementPaymentRequestDto request
    ) {
        if (Boolean.TRUE.equals(request.getGstActive())
                && order.getGstRate() != null
                && order.getGstRate().compareTo(BigDecimal.ZERO) > 0) {
            calculator.assertOptionalRateMatches(
                    "GST percentage against approved PO",
                    request.getGstPercentage(),
                    order.getGstRate(),
                    "ERR_GST_RATE_DIFFERS_FROM_PO"
            );
        }

        if (Boolean.TRUE.equals(request.getTdsActive())
                && order.getTdsPercentage() != null
                && order.getTdsPercentage().compareTo(BigDecimal.ZERO) > 0) {
            calculator.assertOptionalRateMatches(
                    "TDS percentage against approved PO",
                    request.getTdsPercentage(),
                    order.getTdsPercentage(),
                    "ERR_TDS_RATE_DIFFERS_FROM_PO"
            );
        }
    }

    private void logIgnoredClientCalculatedValues(
            ProcurementPaymentRequestDto request,
            ProcurementPaymentCalculation calculation
    ) {
        logClientMismatch("amount", request.getAmount(), calculation.getTaxableAmount());
        logClientMismatch("payableAmount", request.getPayableAmount(), calculation.getVendorNetPayableAmount());
        logClientMismatch("cgstAmount", request.getCgstAmount(), calculation.getCgstAmount());
        logClientMismatch("sgstAmount", request.getSgstAmount(), calculation.getSgstAmount());
        logClientMismatch("igstAmount", request.getIgstAmount(), calculation.getIgstAmount());
        logClientMismatch("totalGstAmount", request.getTotalGstAmount(), calculation.getTotalGstAmount());
        logClientMismatch("tdsAmount", request.getTdsAmount(), calculation.getTdsAmount());
    }

    private void logClientMismatch(
            String field,
            BigDecimal supplied,
            BigDecimal calculated
    ) {
        if (supplied == null) {
            return;
        }
        if (calculator.money(supplied).compareTo(calculator.money(calculated)) != 0) {
            log.warn(
                    "[CLIENT-CALCULATION-IGNORED] field={} | supplied={} | backendCalculated={}",
                    field,
                    supplied,
                    calculated
            );
        }
    }

    private void updateInvoiceDetailsIfSupplied(
            ProcurementPaymentRequest entity,
            ProcurementPaymentActionRequestDto request
    ) {
        if (hasText(request.getInvoiceNumber())) {
            String invoiceNumber = request.getInvoiceNumber().trim();
            validateInvoiceNumberUnique(
                    entity.getVendor().getId(),
                    invoiceNumber,
                    entity.getId()
            );
            entity.setInvoiceNumber(invoiceNumber);
        }
        if (request.getInvoiceDate() != null) {
            entity.setInvoiceDate(request.getInvoiceDate());
        }
    }

    private void updateAndRequireInvoiceDetails(
            ProcurementPaymentRequest entity,
            ProcurementPaymentActionRequestDto request
    ) {
        updateInvoiceDetailsIfSupplied(entity, request);

        if (!hasText(entity.getInvoiceNumber())) {
            throw validation(
                    "Invoice number is required for payment release",
                    "ERR_INVOICE_NUMBER_REQUIRED"
            );
        }
        if (entity.getInvoiceDate() == null) {
            throw validation(
                    "Invoice date is required for payment release",
                    "ERR_INVOICE_DATE_REQUIRED"
            );
        }
    }

    private void validateInvoiceNumberUnique(
            Long vendorId,
            String invoiceNumber,
            Long currentRequestId
    ) {
        if (vendorId == null || !hasText(invoiceNumber)) {
            return;
        }

        boolean duplicate = currentRequestId == null
                ? paymentRequestRepository
                .existsByVendor_IdAndInvoiceNumberIgnoreCaseAndIsDeletedFalse(
                        vendorId,
                        invoiceNumber
                )
                : paymentRequestRepository
                .existsByVendor_IdAndInvoiceNumberIgnoreCaseAndIdNotAndIsDeletedFalse(
                        vendorId,
                        invoiceNumber,
                        currentRequestId
                );

        if (duplicate) {
            throw validation(
                    "Invoice number already exists for this vendor: " + invoiceNumber,
                    "ERR_DUPLICATE_VENDOR_INVOICE_NUMBER"
            );
        }
    }

    private ProcurementOrder getActiveOrder(Long orderId) {
        ProcurementOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Procurement order not found",
                        "ERR_PROCUREMENT_ORDER_NOT_FOUND"
                ));
        if (order.isDeleted()) {
            throw validation(
                    "Deleted procurement order cannot be used",
                    "ERR_DELETED_PROCUREMENT_ORDER"
            );
        }
        return order;
    }

    private ProcurementPaymentRequest getActivePaymentRequestForUpdate(
            Long paymentRequestId
    ) {
        if (paymentRequestId == null) {
            throw validation(
                    "Payment request id is required",
                    "ERR_PAYMENT_REQUEST_ID_REQUIRED"
            );
        }

        return paymentRequestRepository
                .findActiveByIdForUpdate(paymentRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Procurement payment request not found",
                        "ERR_PAYMENT_REQUEST_NOT_FOUND"
                ));
    }

    private void validateUser(Long userId) {
        if (userId == null) {
            throw validation("User id is required", "ERR_USER_ID_REQUIRED");
        }
        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));
    }

    private void validateActiveVendor(Vendor vendor) {
        if (vendor == null || vendor.getId() == null) {
            throw validation(
                    "Vendor is required for procurement payment",
                    "ERR_PAYMENT_REQUEST_VENDOR_NOT_FOUND"
            );
        }
        if (vendor.isDeleted() || vendor.getStatus() != VendorStatus.ACTIVE) {
            throw validation(
                    "Only an ACTIVE vendor can be used for procurement payment",
                    "ERR_VENDOR_NOT_ACTIVE"
            );
        }
    }

    private Pageable pageable(int page, int size) {
        if (page < 0) {
            throw validation("Page cannot be negative", "ERR_INVALID_PAGE");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw validation(
                    "Page size must be between 1 and " + MAX_PAGE_SIZE,
                    "ERR_INVALID_PAGE_SIZE"
            );
        }
        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );
    }

    private ProcurementPaymentRequestResponseDto mapToResponse(
            ProcurementPaymentRequest entity
    ) {
        ProcurementOrder order = entity.getProcurementOrder();
        Project project = entity.getProject();
        Vendor vendor = entity.getVendor();

        BigDecimal settlementAmount = entity.getBankPaymentAmount() != null
                ? calculator.money(entity.getBankPaymentAmount())
                .add(calculator.money(entity.getTdsAmount()))
                .setScale(2, java.math.RoundingMode.HALF_UP)
                : null;

        return builder()
                .id(entity.getId())
                .procurementOrderId(order != null ? order.getId() : null)
                .poNumber(order != null ? order.getPoNumber() : null)
                .projectId(project != null ? project.getId() : null)
                .projectName(project != null ? project.getName() : null)
                .projectNo(project != null ? project.getProjectNo() : null)
                .vendorId(vendor != null ? vendor.getId() : null)
                .vendorName(vendor != null ? vendor.getName() : null)
                .invoiceAmount(entity.getInvoiceAmount())
                .amount(entity.getAmount())
                .payableAmount(entity.getPayableAmount())
                .bankPaymentAmount(entity.getBankPaymentAmount())
                .settlementAmount(settlementAmount)
                .invoiceNumber(entity.getInvoiceNumber())
                .invoiceDate(entity.getInvoiceDate())
                .paymentDate(entity.getPaymentDate())
                .submissionDate(entity.getSubmissionDate())
                .completionRemarks(entity.getCompletionRemarks())
                .proofAttachmentUrls(entity.getProofAttachmentUrls())
                .status(entity.getStatus())
                .approvedDate(entity.getApprovedDate())
                .paymentReleasedDate(entity.getPaymentReleasedDate())
                .createdBy(entity.getCreatedBy())
                .approvedBy(entity.getApprovedBy())
                .paymentReleasedBy(entity.getPaymentReleasedBy())
                .createdDate(entity.getCreatedDate())
                .updatedDate(entity.getUpdatedDate())
                .tdsActive(entity.getTdsActive())
                .tdsPercentage(entity.getTdsPercentage())
                .tdsAmount(entity.getTdsAmount())
                .gstActive(entity.getGstActive())
                .gstType(entity.getGstType())
                .gstStateCode(entity.getGstStateCode())
                // Important: use the payment-request snapshot, not order.getGstRate().
                .gstPercentage(entity.getGstPercentage())
                .cgstAmount(entity.getCgstAmount())
                .sgstAmount(entity.getSgstAmount())
                .igstAmount(entity.getIgstAmount())
                .totalGstAmount(entity.getTotalGstAmount())
                .paymentMode(entity.getPaymentMode())
                .bankLedgerId(entity.getBankLedgerId())
                .ledgerId(entity.getLedgerId())
                .ledgerType(entity.getLedgerType())
                .transactionReference(entity.getTransactionReference())
                .paymentProof(entity.getPaymentProof())
                .calculationVersion(entity.getCalculationVersion())
                .build();
    }

    private String normalizePaymentMode(String value) {
        if (!hasText(value)) {
            throw validation(
                    "Payment mode is required",
                    "ERR_PAYMENT_MODE_REQUIRED"
            );
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDate toLocalDate(Date value) {
        return value == null
                ? null
                : value.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Date value) {
        return value == null
                ? null
                : value.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private String extractAccountServiceError(FeignException exception) {
        if (exception == null) {
            return "Unable to synchronize procurement payment with Account Service";
        }
        String responseBody = exception.contentUTF8();
        return hasText(responseBody)
                ? "Account Service response: " + responseBody
                : "Account Service HTTP status: " + exception.status();
    }

    private ValidationException invalidStatus(
            String prefix,
            PaymentRequestStatus currentStatus
    ) {
        return validation(
                prefix + ". Current status: " + currentStatus,
                "ERR_INVALID_PAYMENT_REQUEST_STATUS"
        );
    }

    private ValidationException validation(String message, String code) {
        return new ValidationException(message, code);
    }

    private String firstNonBlank(String first, String second) {
        return hasText(first) ? first : second;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
