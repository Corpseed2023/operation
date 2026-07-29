package com.doc.impl.vendor;

import com.doc.dto.account.vendor.*;
import com.doc.dto.vendor.*;
import com.doc.dto.vendor.AccountVendorSyncRequestDto;
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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementPaymentRequestServiceImpl implements ProcurementPaymentRequestService {

    private final ProcurementPaymentRequestRepository paymentRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final VendorAccountsSubmissionRepository vendorAccountsSubmissionRepository;
    private final VendorFinalizationRepository vendorFinalizationRepository;
    private final AccountFeignClient accountFeignClient;

    @Override
    @Transactional
    public ProcurementPaymentRequestResponseDto createPaymentRequest(
            Long procurementOrderId,
            ProcurementPaymentRequestDto requestDto
    ) {
        if (procurementOrderId == null) {
            throw new ValidationException(
                    "Procurement order id is required",
                    "ERR_PROCUREMENT_ORDER_ID_REQUIRED"
            );
        }

        if (requestDto == null) {
            throw new ValidationException(
                    "Payment request body is required",
                    "ERR_PAYMENT_REQUEST_BODY_REQUIRED"
            );
        }

        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException(
                    "Created by user id is required",
                    "ERR_CREATED_BY_REQUIRED"
            );
        }

        userRepository.findActiveUserById(requestDto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CreatedBy user not found",
                        "ERR_USER_NOT_FOUND"
                ));

        ProcurementOrder order = purchaseOrderRepository.findById(procurementOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Procurement order not found",
                        "ERR_PROCUREMENT_ORDER_NOT_FOUND"
                ));

        if (order.isDeleted()) {
            throw new ValidationException(
                    "Deleted procurement order cannot be used for payment request",
                    "ERR_DELETED_PROCUREMENT_ORDER"
            );
        }



        paymentRequestRepository.findByProcurementOrderAndIsDeletedFalse(order)
                .ifPresent(existing -> {
                    throw new ValidationException(
                            "Payment request already exists for this procurement order",
                            "ERR_PAYMENT_REQUEST_ALREADY_EXISTS"
                    );
                });

        if (requestDto.getInvoiceAmount() == null
                || requestDto.getInvoiceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "Invoice amount must be greater than zero",
                    "ERR_INVALID_INVOICE_AMOUNT"
            );
        }

        if (requestDto.getPayableAmount() == null
                || requestDto.getPayableAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "Payable amount must be greater than zero",
                    "ERR_INVALID_PAYABLE_AMOUNT"
            );
        }

        if (requestDto.getPayableAmount().compareTo(requestDto.getInvoiceAmount()) > 0) {
            throw new ValidationException(
                    "Payable amount cannot be greater than invoice amount",
                    "ERR_PAYABLE_AMOUNT_EXCEEDS_INVOICE_AMOUNT"
            );
        }

        ProcurementPaymentRequest paymentRequest = new ProcurementPaymentRequest();
        paymentRequest.setProcurementOrder(order);
        paymentRequest.setProject(order.getProject());
        paymentRequest.setVendor(order.getVendor());

        paymentRequest.setInvoiceAmount(requestDto.getInvoiceAmount());
        paymentRequest.setPayableAmount(requestDto.getPayableAmount());
        paymentRequest.setSubmissionDate(new Date());

        paymentRequest.setCompletionRemarks(requestDto.getCompletionRemarks());

        if (requestDto.getProofAttachmentUrls() != null) {
            paymentRequest.setProofAttachmentUrls(requestDto.getProofAttachmentUrls());
        }

        paymentRequest.setStatus(PaymentRequestStatus.PENDING);
        paymentRequest.setCreatedBy(requestDto.getCreatedBy());
        paymentRequest.setCreatedDate(new Date());
        paymentRequest.setUpdatedDate(new Date());
        paymentRequest.setDeleted(false);

        paymentRequest.setTdsActive(requestDto.getTdsActive());
        paymentRequest.setTdsPercentage(requestDto.getTdsPercentage());

        paymentRequest.setGstActive(requestDto.getGstActive());
        paymentRequest.setGstStateCode(requestDto.getGstStateCode());
        paymentRequest.setGstPercentage(requestDto.getGstPercentage());

        paymentRequest.setCgstAmount(requestDto.getCgstAmount());
        paymentRequest.setSgstAmount(requestDto.getSgstAmount());
        paymentRequest.setIgstAmount(requestDto.getIgstAmount());
        paymentRequest.setTotalGstAmount(requestDto.getTotalGstAmount());

        ProcurementPaymentRequest saved = paymentRequestRepository.save(paymentRequest);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementPaymentRequestResponseDto> getPaymentRequestsByStatus(
            PaymentRequestStatus status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Page<ProcurementPaymentRequest> requests;

        if (status == null) {
            requests = paymentRequestRepository.findByIsDeletedFalse(pageable);
        } else {
            requests = paymentRequestRepository.findByStatusAndIsDeletedFalse(status, pageable);
        }

        return requests.map(this::mapToResponse);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementPaymentRequestResponseDto> getPaymentRequestsByProcurementOrderId(
            Long procurementOrderId,
            int page,
            int size
    ) {
        if (procurementOrderId == null) {
            throw new ValidationException(
                    "Procurement order id is required",
                    "ERR_PROCUREMENT_ORDER_ID_REQUIRED"
            );
        }

        ProcurementOrder order = purchaseOrderRepository.findById(procurementOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Procurement order not found",
                        "ERR_PROCUREMENT_ORDER_NOT_FOUND"
                ));

        if (order.isDeleted()) {
            throw new ValidationException(
                    "Deleted procurement order cannot be used for fetching payment requests",
                    "ERR_DELETED_PROCUREMENT_ORDER"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Page<ProcurementPaymentRequest> requests =
                paymentRequestRepository.findByProcurementOrder_IdAndIsDeletedFalse(
                        procurementOrderId,
                        pageable
                );

        return requests.map(this::mapToResponse);
    }


    @Override
    @Transactional
    public ProcurementPaymentRequestResponseDto approvePaymentRequest(
            Long paymentRequestId,
            Long userId,
            ProcurementPaymentActionRequestDto request
    ) {
        validateUser(userId);

        ProcurementPaymentRequest paymentRequest =
                getActivePaymentRequest(paymentRequestId);

        if (paymentRequest.getStatus() != PaymentRequestStatus.PENDING
                && paymentRequest.getStatus()
                != PaymentRequestStatus.UNDER_REVIEW) {

            throw new ValidationException(
                    "Only PENDING or UNDER_REVIEW payment request can be approved. "
                            + "Current status: "
                            + paymentRequest.getStatus(),
                    "ERR_INVALID_PAYMENT_REQUEST_STATUS"
            );
        }

        if (paymentRequest.getVendor() == null) {
            throw new ValidationException(
                    "Vendor is not available against payment request ID: "
                            + paymentRequestId,
                    "ERR_PAYMENT_REQUEST_VENDOR_NOT_FOUND"
            );
        }

        if (paymentRequest.getVendor().getStatus()
                != VendorStatus.ACTIVE) {

            throw new ValidationException(
                    "Only an ACTIVE vendor payment request can be approved",
                    "ERR_VENDOR_NOT_ACTIVE"
            );
        }

        Date currentDate =
                new Date();

        paymentRequest.setStatus(
                PaymentRequestStatus.APPROVED
        );

        paymentRequest.setApprovedBy(userId);
        paymentRequest.setApprovedDate(currentDate);
        paymentRequest.setUpdatedDate(currentDate);

        if (request != null
                && hasText(request.getComment())) {

            paymentRequest.setCompletionRemarks(
                    request.getComment().trim()
            );
        }

        ProcurementPaymentRequest saved =
                paymentRequestRepository.saveAndFlush(
                        paymentRequest
                );

        /*
         * Approval creates/updates the vendor and vendor ledger.
         *
         * It does not create PAYMENT voucher because payment
         * has not yet been released.
         */
        AccountVendorSyncResponseDto accountResponse =
                syncVendorWithAccountService(
                        saved,
                        userId,
                        null
                );

        log.info(
                "Payment request approved and vendor synchronized. "
                        + "paymentRequestId={}, vendorId={}, "
                        + "externalVendorId={}, vendorLedgerId={}, "
                        + "voucherCreated={}",
                saved.getId(),
                saved.getVendor().getId(),
                accountResponse.getExternalVendorId(),
                accountResponse.getLedgerId(),
                accountResponse.getVoucherCreated()
        );

        return mapToResponse(saved);
    }

    private AccountVendorSyncResponseDto syncVendorWithAccountService(
            ProcurementPaymentRequest paymentRequest,
            Long operationUserId,
            VendorVoucherRequestDto voucherDetails
    ) {
        if (paymentRequest == null
                || paymentRequest.getId() == null) {

            throw new ValidationException(
                    "Payment request is required for Account Service synchronization",
                    "ERR_PAYMENT_REQUEST_REQUIRED"
            );
        }

        if (paymentRequest.getVendor() == null) {
            throw new ValidationException(
                    "Vendor is not available against payment request ID: "
                            + paymentRequest.getId(),
                    "ERR_PAYMENT_REQUEST_VENDOR_NOT_FOUND"
            );
        }

        Vendor vendor =
                paymentRequest.getVendor();

        Long vendorId =
                vendor.getId();

        VendorAccountsSubmission accountsSubmission =
                vendorAccountsSubmissionRepository
                        .findFirstByVendor_IdOrderByIdDesc(
                                vendorId
                        )
                        .orElseThrow(() ->
                                new ValidationException(
                                        "Vendor accounts submission not found "
                                                + "for vendor ID: "
                                                + vendorId,
                                        "ERR_VENDOR_ACCOUNTS_SUBMISSION_NOT_FOUND"
                                )
                        );

        VendorFinalization vendorFinalization =
                vendorFinalizationRepository
                        .findFirstByVendor_IdOrderByIdDesc(
                                vendorId
                        )
                        .orElseThrow(() ->
                                new ValidationException(
                                        "Vendor finalization not found "
                                                + "for vendor ID: "
                                                + vendorId,
                                        "ERR_VENDOR_FINALIZATION_NOT_FOUND"
                                )
                        );

        LocalDateTime currentDateTime =
                LocalDateTime.now();

        String gstRegistrationType =
                accountsSubmission.getGstRegistrationType() != null
                        ? accountsSubmission
                        .getGstRegistrationType()
                        .name()
                        : vendor.getGstRegistrationType() != null
                        ? vendor.getGstRegistrationType().name()
                        : null;

        String gstNumber =
                hasText(accountsSubmission.getGstNumber())
                        ? accountsSubmission.getGstNumber()
                        : vendor.getGstNumber();

        AccountVendorSyncRequestDto syncRequest =
                AccountVendorSyncRequestDto.builder()
                        .operationVendorId(vendorId)

                        .vendorAccountsSubmissionId(
                                accountsSubmission.getId()
                        )

                        .vendorFinalizationId(
                                vendorFinalization.getId()
                        )

                        .vendorName(vendor.getName())
                        .email(vendor.getEmail())
                        .mobile(vendor.getMobile())
                        .pan(vendor.getPanNumber())
                        .gstNumber(gstNumber)

                        .gstRegistrationType(
                                gstRegistrationType
                        )

                        .accountHolderName(
                                accountsSubmission
                                        .getAccountHolderName()
                        )

                        .bankAccountNumber(
                                accountsSubmission.getAccountNumber()
                        )

                        .ifscCode(
                                accountsSubmission.getIfsc()
                        )

                        /*
                         * Bank name is currently not available
                         * in VendorAccountsSubmission.
                         */
                        .bankName(null)

                        .branchAddress(
                                accountsSubmission.getBranchAddress()
                        )

                        .fullAddress(
                                accountsSubmission.getBranchAddress()
                        )

                        .city(vendor.getCity())
                        .state(vendor.getState())
                        .country(vendor.getCountry())

                        .active(
                                vendor.getStatus()
                                        == VendorStatus.ACTIVE
                        )

                        .approvedByOperationUserId(
                                operationUserId
                        )

                        .approvedAt(currentDateTime)
                        .operationUpdatedAt(currentDateTime)

                        /*
                         * Null during approval.
                         * PAYMENT voucher during release.
                         */
                        .voucherDetails(voucherDetails)

                        .build();

        try {
            AccountVendorSyncResponseDto response =
                    accountFeignClient.syncVendor(
                            syncRequest
                    );

            if (response == null) {
                throw new ValidationException(
                        "Account Service returned an empty response "
                                + "for vendor ID: "
                                + vendorId,
                        "ERR_EMPTY_ACCOUNT_VENDOR_SYNC_RESPONSE"
                );
            }

            if (!"SUCCESS".equalsIgnoreCase(
                    response.getSyncStatus()
            )) {
                throw new ValidationException(
                        "Account Service vendor synchronization failed: "
                                + response.getMessage(),
                        "ERR_ACCOUNT_VENDOR_SYNC_UNSUCCESSFUL"
                );
            }

            log.info(
                    "Vendor synchronized with Account Service. "
                            + "paymentRequestId={}, vendorId={}, "
                            + "externalVendorId={}, ledgerId={}, "
                            + "voucherRequested={}, voucherCreated={}, "
                            + "voucherId={}",
                    paymentRequest.getId(),
                    vendorId,
                    response.getExternalVendorId(),
                    response.getLedgerId(),
                    voucherDetails != null,
                    response.getVoucherCreated(),
                    response.getVoucherId()
            );

            return response;

        } catch (FeignException exception) {
            log.error(
                    "Vendor synchronization failed. "
                            + "paymentRequestId={}, vendorId={}, "
                            + "status={}, response={}",
                    paymentRequest.getId(),
                    vendorId,
                    exception.status(),
                    exception.contentUTF8(),
                    exception
            );

            throw new ValidationException(
                    extractAccountServiceError(exception),
                    "ERR_ACCOUNT_VENDOR_SYNC_FAILED"
            );
        }
    }

    @Override
    @Transactional
    public ProcurementPaymentRequestResponseDto rejectPaymentRequest(
            Long paymentRequestId,
            Long userId,
            ProcurementPaymentActionRequestDto request
    ) {
        validateUser(userId);

        ProcurementPaymentRequest paymentRequest = getActivePaymentRequest(paymentRequestId);

        if (paymentRequest.getStatus() != PaymentRequestStatus.PENDING
                && paymentRequest.getStatus() != PaymentRequestStatus.UNDER_REVIEW) {
            throw new ValidationException(
                    "Only PENDING or UNDER_REVIEW payment request can be rejected. Current status: "
                            + paymentRequest.getStatus(),
                    "ERR_INVALID_PAYMENT_REQUEST_STATUS"
            );
        }

        String reason = request != null ? request.getReason() : null;

        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException(
                    "Rejection reason is required",
                    "ERR_REJECTION_REASON_REQUIRED"
            );
        }

        paymentRequest.setStatus(PaymentRequestStatus.REJECTED);
        paymentRequest.setApprovedBy(userId);
        paymentRequest.setUpdatedDate(new Date());
        paymentRequest.setCompletionRemarks(reason.trim());

        ProcurementPaymentRequest saved = paymentRequestRepository.save(paymentRequest);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ProcurementPaymentRequestResponseDto releasePayment(
            Long paymentRequestId,
            Long userId,
            ProcurementPaymentActionRequestDto request
    ) {
        validateUser(userId);

        if (request == null) {
            throw new ValidationException(
                    "Payment release request is required",
                    "ERR_PAYMENT_RELEASE_REQUEST_REQUIRED"
            );
        }

        ProcurementPaymentRequest paymentRequest =
                getActivePaymentRequest(paymentRequestId);

        if (paymentRequest.getStatus()
                != PaymentRequestStatus.APPROVED
                && paymentRequest.getStatus()
                != PaymentRequestStatus.PAYMENT_PROCESSING) {

            throw new ValidationException(
                    "Only APPROVED or PAYMENT_PROCESSING payment request "
                            + "can be released. Current status: "
                            + paymentRequest.getStatus(),
                    "ERR_INVALID_PAYMENT_REQUEST_STATUS"
            );
        }

        validatePaymentVoucherDetails(
                paymentRequest,
                request
        );

        Date currentDate =
                new Date();

        paymentRequest.setStatus(
                PaymentRequestStatus.PAYMENT_RELEASED
        );

        paymentRequest.setInvoiceNumber(
                clean(request.getInvoiceNumber())
        );

        paymentRequest.setInvoiceDate(
                request.getInvoiceDate()
        );

        paymentRequest.setPaymentReleasedBy(userId);
        paymentRequest.setPaymentReleasedDate(currentDate);
        paymentRequest.setUpdatedDate(currentDate);

        if (hasText(request.getComment())) {
            paymentRequest.setCompletionRemarks(
                    request.getComment().trim()
            );
        }

        ProcurementPaymentRequest saved =
                paymentRequestRepository.saveAndFlush(
                        paymentRequest
                );

        VendorVoucherRequestDto voucherDetails =
                buildVendorPaymentVoucher(
                        saved,
                        request
                );

        AccountVendorSyncResponseDto accountResponse =
                syncVendorWithAccountService(
                        saved,
                        userId,
                        voucherDetails
                );

        if (!Boolean.TRUE.equals(
                accountResponse.getVoucherCreated()
        )) {
            throw new ValidationException(
                    "Account Service did not create the vendor payment voucher",
                    "ERR_VENDOR_PAYMENT_VOUCHER_NOT_CREATED"
            );
        }

        log.info(
                "Vendor payment released and voucher posted. "
                        + "paymentRequestId={}, vendorId={}, "
                        + "voucherId={}, voucherNumber={}, "
                        + "totalDebit={}, totalCredit={}",
                saved.getId(),
                saved.getVendor() != null
                        ? saved.getVendor().getId()
                        : null,
                accountResponse.getVoucherId(),
                accountResponse.getVoucherNumber(),
                accountResponse.getTotalDebit(),
                accountResponse.getTotalCredit()
        );

        return mapToResponse(saved);
    }

    private void validatePaymentVoucherDetails(
            ProcurementPaymentRequest paymentRequest,
            ProcurementPaymentActionRequestDto request
    ) {
        if (paymentRequest.getPayableAmount() == null
                || paymentRequest.getPayableAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "Valid payable amount is required",
                    "ERR_INVALID_PAYABLE_AMOUNT"
            );
        }

        if (request.getBankLedgerId() == null
                || request.getBankLedgerId() <= 0) {

            throw new ValidationException(
                    "Valid Account Service bank ledger ID is required",
                    "ERR_BANK_LEDGER_ID_REQUIRED"
            );
        }

        BigDecimal grossSettlementAmount =
                money(paymentRequest.getPayableAmount());

        BigDecimal bankPaymentAmount =
                money(request.getBankPaymentAmount());

        BigDecimal tdsAmount =
                money(request.getTdsAmount());

        if (bankPaymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "Bank payment amount must be greater than zero",
                    "ERR_INVALID_BANK_PAYMENT_AMOUNT"
            );
        }

        if (tdsAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(
                    "TDS amount cannot be negative",
                    "ERR_INVALID_TDS_AMOUNT"
            );
        }

        if (tdsAmount.compareTo(BigDecimal.ZERO) > 0
                && (request.getTdsPayableLedgerId() == null
                || request.getTdsPayableLedgerId() <= 0)) {

            throw new ValidationException(
                    "TDS Payable ledger ID is required "
                            + "when TDS amount is greater than zero",
                    "ERR_TDS_PAYABLE_LEDGER_ID_REQUIRED"
            );
        }

        BigDecimal totalCredit =
                bankPaymentAmount
                        .add(tdsAmount)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        if (grossSettlementAmount.compareTo(totalCredit) != 0) {
            throw new ValidationException(
                    "Payable amount must equal bank payment amount "
                            + "plus TDS amount. Payable amount: "
                            + grossSettlementAmount
                            + ", bank amount: "
                            + bankPaymentAmount
                            + ", TDS amount: "
                            + tdsAmount,
                    "ERR_VENDOR_PAYMENT_AMOUNT_MISMATCH"
            );
        }

        if (Boolean.TRUE.equals(
                paymentRequest.getTdsActive()
        ) && tdsAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "TDS amount is required because TDS is active",
                    "ERR_TDS_AMOUNT_REQUIRED"
            );
        }

        if (!Boolean.TRUE.equals(
                paymentRequest.getTdsActive()
        ) && tdsAmount.compareTo(BigDecimal.ZERO) > 0) {

            throw new ValidationException(
                    "TDS amount cannot be supplied because TDS is inactive",
                    "ERR_TDS_NOT_ACTIVE"
            );
        }
    }



    private ProcurementPaymentRequest getActivePaymentRequest(Long paymentRequestId) {
        if (paymentRequestId == null) {
            throw new ValidationException(
                    "Payment request id is required",
                    "ERR_PAYMENT_REQUEST_ID_REQUIRED"
            );
        }

        ProcurementPaymentRequest paymentRequest = paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Procurement payment request not found",
                        "ERR_PAYMENT_REQUEST_NOT_FOUND"
                ));

        if (paymentRequest.isDeleted()) {
            throw new ValidationException(
                    "Deleted payment request cannot be processed",
                    "ERR_DELETED_PAYMENT_REQUEST"
            );
        }

        return paymentRequest;
    }

    private void validateUser(Long userId) {
        if (userId == null) {
            throw new ValidationException(
                    "User id is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));
    }

    private VendorVoucherRequestDto buildVendorPaymentVoucher(
            ProcurementPaymentRequest paymentRequest,
            ProcurementPaymentActionRequestDto request
    ) {
        BigDecimal grossSettlementAmount =
                money(paymentRequest.getPayableAmount());

        BigDecimal bankPaymentAmount =
                money(request.getBankPaymentAmount());

        BigDecimal tdsAmount =
                money(request.getTdsAmount());

        List<VendorVoucherEntryRequestDto> entries =
                new ArrayList<>();

        /*
         * Dr Vendor Ledger
         *
         * Vendor liability is reduced.
         */
        entries.add(
                VendorVoucherEntryRequestDto.builder()
                        .ledgerSource(
                                VendorVoucherLedgerSource
                                        .VENDOR_LEDGER
                        )
                        .ledgerId(null)
                        .debitAmount(
                                grossSettlementAmount
                        )
                        .creditAmount(zero())
                        .narration(
                                "Vendor payable settled against payment request "
                                        + paymentRequest.getId()
                        )
                        .build()
        );

        /*
         * Cr Bank Ledger
         *
         * Money leaves company bank account.
         */
        entries.add(
                VendorVoucherEntryRequestDto.builder()
                        .ledgerSource(
                                VendorVoucherLedgerSource
                                        .EXISTING_LEDGER
                        )
                        .ledgerId(
                                request.getBankLedgerId()
                        )
                        .debitAmount(zero())
                        .creditAmount(
                                bankPaymentAmount
                        )
                        .narration(
                                buildBankNarration(request)
                        )
                        .build()
        );

        /*
         * Cr TDS Payable only when TDS is deducted.
         */
        if (tdsAmount.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(
                    VendorVoucherEntryRequestDto.builder()
                            .ledgerSource(
                                    VendorVoucherLedgerSource
                                            .EXISTING_LEDGER
                            )
                            .ledgerId(
                                    request.getTdsPayableLedgerId()
                            )
                            .debitAmount(zero())
                            .creditAmount(tdsAmount)
                            .narration(
                                    "TDS deducted from vendor payment"
                            )
                            .build()
            );
        }

        return VendorVoucherRequestDto.builder()
                .voucherType(
                        AccountVoucherType.PAYMENT
                )
                .sourceType(
                        AccountVoucherSourceType
                                .PROCUREMENT_VENDOR_PAYMENT
                )
                .sourceId(
                        paymentRequest.getId()
                )
                .voucherDate(
                        LocalDate.now()
                )
                .narration(
                        buildVoucherNarration(
                                paymentRequest,
                                request
                        )
                )
                .entries(entries)
                .build();
    }


    private String buildBankNarration(
            ProcurementPaymentActionRequestDto request
    ) {
        String narration =
                "Vendor payment released through bank";

        if (hasText(request.getTransactionReference())) {
            narration += ", transaction reference: "
                    + request.getTransactionReference().trim();
        }

        return narration;
    }

    private String buildVoucherNarration(
            ProcurementPaymentRequest paymentRequest,
            ProcurementPaymentActionRequestDto request
    ) {
        String vendorName =
                paymentRequest.getVendor() != null
                        && hasText(paymentRequest.getVendor().getName())
                        ? paymentRequest.getVendor().getName().trim()
                        : "Vendor";

        String narration =
                "Payment released to "
                        + vendorName
                        + " against payment request "
                        + paymentRequest.getId();

        if (hasText(request.getInvoiceNumber())) {
            narration += ", invoice: "
                    + request.getInvoiceNumber().trim();
        }

        if (hasText(request.getTransactionReference())) {
            narration += ", transaction reference: "
                    + request.getTransactionReference().trim();
        }

        return narration;
    }

    private BigDecimal money(
            BigDecimal value
    ) {
        return value == null
                ? zero()
                : value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.trim().isEmpty();
    }

    private String clean(
            String value
    ) {
        return hasText(value)
                ? value.trim()
                : null;
    }

    private String extractAccountServiceError(
            FeignException exception
    ) {
        if (exception == null) {
            return "Unable to synchronise vendor with Account Service";
        }

        String responseBody =
                exception.contentUTF8();

        if (hasText(responseBody)) {
            return "Unable to synchronise vendor with Account Service. "
                    + "Account Service response: "
                    + responseBody;
        }

        return "Unable to synchronise vendor with Account Service. "
                + "HTTP status: "
                + exception.status();
    }

    private ProcurementPaymentRequestResponseDto mapToResponse(ProcurementPaymentRequest request) {
        ProcurementOrder order = request.getProcurementOrder();
        Project project = request.getProject();
        Vendor vendor = request.getVendor();

        return ProcurementPaymentRequestResponseDto.builder()
                .id(request.getId())

                .procurementOrderId(order != null ? order.getId() : null)
                .poNumber(order != null ? order.getPoNumber() : null)

                .projectId(project != null ? project.getId() : null)
                .projectName(project != null ? project.getName() : null)
                .projectNo(project != null ? project.getProjectNo() : null)

                .vendorId(vendor != null ? vendor.getId() : null)
                .vendorName(vendor != null ? vendor.getName() : null)

                .invoiceAmount(request.getInvoiceAmount())
                .payableAmount(request.getPayableAmount())

                .invoiceNumber(request.getInvoiceNumber())
                .invoiceDate(request.getInvoiceDate())
                .submissionDate(request.getSubmissionDate())

                .completionRemarks(request.getCompletionRemarks())
                .proofAttachmentUrls(request.getProofAttachmentUrls())

                .status(request.getStatus())

                .approvedDate(request.getApprovedDate())
                .paymentReleasedDate(request.getPaymentReleasedDate())

                .createdBy(request.getCreatedBy())
                .approvedBy(request.getApprovedBy())
                .paymentReleasedBy(request.getPaymentReleasedBy())

                .createdDate(request.getCreatedDate())
                .updatedDate(request.getUpdatedDate())
                .tdsActive(request.getTdsActive())
                .tdsPercentage(request.getTdsPercentage())

                .gstActive(request.getGstActive())
                .gstStateCode(request.getGstStateCode())
                .gstPercentage(request.getGstPercentage())

                .cgstAmount(request.getCgstAmount())
                .sgstAmount(request.getSgstAmount())
                .igstAmount(request.getIgstAmount())
                .totalGstAmount(request.getTotalGstAmount())
                .build();
    }
}