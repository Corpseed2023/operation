package com.doc.impl.vendor;

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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementPaymentRequestServiceImpl
        implements ProcurementPaymentRequestService {

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

        ProcurementOrder order =
                purchaseOrderRepository.findById(procurementOrderId)
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

        if (requestDto.getAmount() == null
                || requestDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "Amount must be greater than zero",
                    "ERR_INVALID_AMOUNT"
            );
        }

        if (order.getFinalAmount() == null) {
            throw new ValidationException(
                    "Final amount is not configured for this procurement order",
                    "ERR_PO_FINAL_AMOUNT_MISSING"
            );
        }

        if (requestDto.getAmount().compareTo(order.getFinalAmount()) > 0) {
            throw new ValidationException(
                    "Payment request amount cannot be greater than procurement order final amount",
                    "ERR_PAYMENT_AMOUNT_EXCEEDS_PO_FINAL_AMOUNT"
            );
        }


        if (requestDto.getInvoiceAmount() == null
                || requestDto.getInvoiceAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "Invoice amount must be greater than zero",
                    "ERR_INVALID_INVOICE_AMOUNT"
            );
        }

        if (requestDto.getPayableAmount() == null
                || requestDto.getPayableAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "Payable amount must be greater than zero",
                    "ERR_INVALID_PAYABLE_AMOUNT"
            );
        }

        if (requestDto.getPayableAmount()
                .compareTo(requestDto.getInvoiceAmount()) > 0) {

            throw new ValidationException(
                    "Payable amount cannot be greater than invoice amount",
                    "ERR_PAYABLE_AMOUNT_EXCEEDS_INVOICE_AMOUNT"
            );
        }
        BigDecimal existingPaymentAmount =
                paymentRequestRepository
                        .sumAmountByProcurementOrderAndIsDeletedFalse(order);

        BigDecimal newPaymentAmount = requestDto.getAmount();

        if (newPaymentAmount == null
                || newPaymentAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "Payment amount must be greater than zero",
                    "ERR_INVALID_PAYMENT_AMOUNT"
            );
        }

        BigDecimal totalPaymentAmount =
                existingPaymentAmount.add(newPaymentAmount);

        if (totalPaymentAmount.compareTo(order.getFinalAmount()) > 0) {
            throw new ValidationException(
                    "Total payment request amount cannot exceed procurement order final amount",
                    "ERR_PAYMENT_REQUEST_AMOUNT_EXCEEDS_PO"
            );
        }

        ProcurementPaymentRequest paymentRequest =
                new ProcurementPaymentRequest();

        paymentRequest.setProcurementOrder(order);
        paymentRequest.setProject(order.getProject());
        paymentRequest.setVendor(order.getVendor());

        paymentRequest.setInvoiceAmount(
                requestDto.getInvoiceAmount()
        );

        paymentRequest.setPayableAmount(
                requestDto.getPayableAmount()
        );

        paymentRequest.setSubmissionDate(new Date());

        paymentRequest.setCompletionRemarks(
                requestDto.getCompletionRemarks()
        );

        if (requestDto.getProofAttachmentUrls() != null) {
            paymentRequest.setProofAttachmentUrls(
                    requestDto.getProofAttachmentUrls()
            );
        }

        paymentRequest.setStatus(
                PaymentRequestStatus.PENDING
        );

        paymentRequest.setCreatedBy(
                requestDto.getCreatedBy()
        );

        paymentRequest.setCreatedDate(new Date());
        paymentRequest.setUpdatedDate(new Date());
        paymentRequest.setDeleted(false);

        paymentRequest.setTdsActive(
                requestDto.getTdsActive()
        );

        paymentRequest.setTdsPercentage(
                requestDto.getTdsPercentage()
        );

        paymentRequest.setGstActive(
                requestDto.getGstActive()
        );

        paymentRequest.setGstStateCode(
                requestDto.getGstStateCode()
        );

        paymentRequest.setGstPercentage(
                requestDto.getGstPercentage()
        );

        paymentRequest.setCgstAmount(
                requestDto.getCgstAmount()
        );

        paymentRequest.setSgstAmount(
                requestDto.getSgstAmount()
        );

        paymentRequest.setIgstAmount(
                requestDto.getIgstAmount()
        );

        paymentRequest.setTotalGstAmount(
                requestDto.getTotalGstAmount()
        );

        paymentRequest.setAmount(
                requestDto.getAmount()
        );

        paymentRequest.setPaymentMode(
                requestDto.getPaymentMode()
        );

        paymentRequest.setBankLedgerId(
                requestDto.getBankLedgerId()
        );

        paymentRequest.setLedgerId(
                requestDto.getLedgerId()
        );

        paymentRequest.setLedgerType(
                requestDto.getLedgerType()
        );

        paymentRequest.setTransactionReference(
                requestDto.getTransactionReference()
        );

        paymentRequest.setPaymentProof(
                requestDto.getPaymentProof()
        );

        paymentRequest.setGstType(
                requestDto.getGstType()
        );

        paymentRequest.setTdsAmount(
                requestDto.getTdsAmount()
        );

        ProcurementPaymentRequest saved =
                paymentRequestRepository.save(paymentRequest);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementPaymentRequestResponseDto>
    getPaymentRequestsByStatus(
            PaymentRequestStatus status,
            int page,
            int size
    ) {
        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdDate"
                        )
                );

        Page<ProcurementPaymentRequest> requests;

        if (status == null) {
            requests =
                    paymentRequestRepository
                            .findByIsDeletedFalse(pageable);
        } else {
            requests =
                    paymentRequestRepository
                            .findByStatusAndIsDeletedFalse(
                                    status,
                                    pageable
                            );
        }

        return requests.map(this::mapToResponse);
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
            throw new ValidationException(
                    "Procurement order id is required",
                    "ERR_PROCUREMENT_ORDER_ID_REQUIRED"
            );
        }

        ProcurementOrder order =
                purchaseOrderRepository.findById(procurementOrderId)
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

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdDate"
                        )
                );

        Page<ProcurementPaymentRequest> requests =
                paymentRequestRepository
                        .findByProcurementOrder_IdAndIsDeletedFalse(
                                procurementOrderId,
                                pageable
                        );

        return requests.map(this::mapToResponse);
    }

    /*
     * ================================================================
     * APPROVE
     * ================================================================
     *
     * IMPORTANT:
     *
     * Accounts Service is NOT called here.
     *
     * Approval only changes the Operation-side status.
     *
     * Accounts Service will be called only during RELEASE.
     */
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

        Date currentDate = new Date();

        /*
         * Invoice details can still be captured during approval.
         * They are stored in Operation only.
         */
        if (request != null
                && hasText(request.getInvoiceNumber())) {

            paymentRequest.setInvoiceNumber(
                    request.getInvoiceNumber().trim()
            );
        }

        if (request != null
                && request.getInvoiceDate() != null) {

            paymentRequest.setInvoiceDate(
                    request.getInvoiceDate()
            );
        }

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
         * NO ACCOUNT SERVICE CALL HERE.
         *
         * The Accounts Service is called only after the payment
         * is actually released.
         */

        log.info(
                "Payment request approved in Operation Service only. "
                        + "Accounts synchronization will happen on release. "
                        + "paymentRequestId={}, vendorId={}",
                saved.getId(),
                saved.getVendor() != null
                        ? saved.getVendor().getId()
                        : null
        );

        return mapToResponse(saved);
    }

    /*
     * ================================================================
     * ACCOUNT SERVICE VENDOR SYNC
     * ================================================================
     */
    private AccountVendorSyncResponseDto syncVendorWithAccountService(
            ProcurementPaymentRequest paymentRequest,
            Long operationUserId,
            ProcurementPaymentActionRequestDto actionRequest
    ) {
        if (paymentRequest == null || paymentRequest.getId() == null) {
            throw new ValidationException(
                    "Payment request is required for Account Service synchronization",
                    "ERR_PAYMENT_REQUEST_REQUIRED"
            );
        }

        Vendor vendor = paymentRequest.getVendor();

        if (vendor == null || vendor.getId() == null) {
            throw new ValidationException(
                    "Vendor is not available against payment request ID: "
                            + paymentRequest.getId(),
                    "ERR_PAYMENT_REQUEST_VENDOR_NOT_FOUND"
            );
        }

        Long vendorId = vendor.getId();

        VendorAccountsSubmission accountsSubmission =
                vendorAccountsSubmissionRepository
                        .findFirstByVendor_IdOrderByIdDesc(vendorId)
                        .orElseThrow(() ->
                                new ValidationException(
                                        "Vendor accounts submission not found for vendor ID: "
                                                + vendorId,
                                        "ERR_VENDOR_ACCOUNTS_SUBMISSION_NOT_FOUND"
                                )
                        );

        VendorFinalization vendorFinalization =
                vendorFinalizationRepository
                        .findFirstByVendor_IdOrderByIdDesc(vendorId)
                        .orElseThrow(() ->
                                new ValidationException(
                                        "Vendor finalization not found for vendor ID: "
                                                + vendorId,
                                        "ERR_VENDOR_FINALIZATION_NOT_FOUND"
                                )
                        );

        String gstRegistrationType =
                accountsSubmission.getGstRegistrationType() != null
                        ? accountsSubmission.getGstRegistrationType().name()
                        : vendor.getGstRegistrationType() != null
                        ? vendor.getGstRegistrationType().name()
                        : null;

        String gstNumber =
                hasText(accountsSubmission.getGstNumber())
                        ? accountsSubmission.getGstNumber().trim()
                        : clean(vendor.getGstNumber());

        VendorPaymentApprovalRequestDto paymentApproval =
                buildPaymentReleaseRequest(
                        paymentRequest,
                        operationUserId,
                        actionRequest
                );

        LocalDateTime currentDateTime = LocalDateTime.now();

        AccountVendorSyncRequestDto syncRequest =
                AccountVendorSyncRequestDto.builder()
                        .operationVendorId(vendorId)
                        .vendorAccountsSubmissionId(accountsSubmission.getId())
                        .vendorFinalizationId(vendorFinalization.getId())

                        .vendorName(vendor.getName())
                        .email(vendor.getEmail())
                        .mobile(vendor.getMobile())
                        .pan(vendor.getPanNumber())
                        .gstNumber(gstNumber)
                        .gstRegistrationType(gstRegistrationType)

                        .accountHolderName(
                                accountsSubmission.getAccountHolderName()
                        )
                        .bankAccountNumber(
                                accountsSubmission.getAccountNumber()
                        )
                        .ifscCode(accountsSubmission.getIfsc())

                        /*
                         * Keep null until bankName is added to
                         * VendorAccountsSubmission.
                         */
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
                        .approvedByOperationUserId(operationUserId)
                        .approvedAt(currentDateTime)
                        .operationUpdatedAt(currentDateTime)

                        /*
                         * Non-null during payment release.
                         */
                        .paymentApproval(paymentApproval)
                        .build();

        try {
            log.info(
                    "Calling Account Service. paymentRequestId={}, vendorId={}, "
                            + "gstActive={}, tdsActive={}, bankLedgerId={}, "
                            + "bankPaymentAmount={}",
                    paymentRequest.getId(),
                    vendorId,
                    paymentApproval.getGstActive(),
                    paymentApproval.getTdsActive(),
                    paymentApproval.getBankLedgerId(),
                    paymentApproval.getBankPaymentAmount()
            );

            AccountVendorSyncResponseDto response =
                    accountFeignClient.syncVendor(syncRequest);

            if (response == null) {
                throw new ValidationException(
                        "Account Service returned an empty response for vendor ID: "
                                + vendorId,
                        "ERR_EMPTY_ACCOUNT_VENDOR_SYNC_RESPONSE"
                );
            }

            if (!"SUCCESS".equalsIgnoreCase(response.getSyncStatus())) {
                throw new ValidationException(
                        "Account Service vendor synchronization failed: "
                                + response.getMessage(),
                        "ERR_ACCOUNT_VENDOR_SYNC_UNSUCCESSFUL"
                );
            }

            log.info(
                    "Account Service synchronization successful. "
                            + "paymentRequestId={}, vendorId={}, ledgerId={}, "
                            + "invoiceVoucherId={}, paymentVoucherId={}",
                    paymentRequest.getId(),
                    vendorId,
                    response.getLedgerId(),
                    response.getVoucherId(),
                    response.getPaymentVoucherId()
            );

            return response;

        } catch (FeignException exception) {
            log.error(
                    "Account Service synchronization failed. "
                            + "paymentRequestId={}, vendorId={}, status={}, response={}",
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

        ProcurementPaymentRequest paymentRequest =
                getActivePaymentRequest(paymentRequestId);

        if (paymentRequest.getStatus() != PaymentRequestStatus.PENDING
                && paymentRequest.getStatus()
                != PaymentRequestStatus.UNDER_REVIEW) {

            throw new ValidationException(
                    "Only PENDING or UNDER_REVIEW payment request can be rejected. "
                            + "Current status: "
                            + paymentRequest.getStatus(),
                    "ERR_INVALID_PAYMENT_REQUEST_STATUS"
            );
        }

        String reason =
                request != null
                        ? request.getReason()
                        : null;

        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException(
                    "Rejection reason is required",
                    "ERR_REJECTION_REASON_REQUIRED"
            );
        }

        paymentRequest.setStatus(
                PaymentRequestStatus.REJECTED
        );

        paymentRequest.setApprovedBy(userId);
        paymentRequest.setUpdatedDate(new Date());

        paymentRequest.setCompletionRemarks(
                reason.trim()
        );

        ProcurementPaymentRequest saved =
                paymentRequestRepository.save(paymentRequest);

        return mapToResponse(saved);
    }

    /*
     * ================================================================
     * RELEASE PAYMENT
     * ================================================================
     *
     * THIS is now where Accounts Service is called.
     *
     * Flow:
     *
     * APPROVED
     *     ↓
     * Operation captures payment details
     *     ↓
     * AccountVendorSyncRequestDto
     *     ↓
     * Account Service
     *     ↓
     * Invoice + Payment accounting
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
            throw new ValidationException(
                    "Payment release request is required",
                    "ERR_PAYMENT_RELEASE_REQUEST_REQUIRED"
            );
        }

        ProcurementPaymentRequest paymentRequest =
                getActivePaymentRequest(paymentRequestId);

        if (paymentRequest.getStatus() != PaymentRequestStatus.APPROVED
                && paymentRequest.getStatus()
                != PaymentRequestStatus.PAYMENT_PROCESSING) {

            throw new ValidationException(
                    "Only APPROVED or PAYMENT_PROCESSING payment request "
                            + "can be released. Current status: "
                            + paymentRequest.getStatus(),
                    "ERR_INVALID_PAYMENT_REQUEST_STATUS"
            );
        }

        if (request.getBankPaymentAmount() == null
                || request.getBankPaymentAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "Bank payment amount must be greater than zero",
                    "ERR_INVALID_BANK_PAYMENT_AMOUNT"
            );
        }

        if (request.getBankLedgerId() == null
                || request.getBankLedgerId() <= 0) {

            throw new ValidationException(
                    "Bank/Cash ledger id is required for payment release",
                    "ERR_BANK_LEDGER_REQUIRED"
            );
        }

        Date currentDate = new Date();

        if (hasText(request.getInvoiceNumber())) {
            paymentRequest.setInvoiceNumber(
                    request.getInvoiceNumber().trim()
            );
        }

        if (request.getInvoiceDate() != null) {
            paymentRequest.setInvoiceDate(request.getInvoiceDate());
        }

        if (hasText(request.getTransactionReference())) {
            paymentRequest.setTransactionReference(
                    request.getTransactionReference().trim()
            );
        }

        if (hasText(request.getPaymentProof())) {
            paymentRequest.setPaymentProof(
                    request.getPaymentProof().trim()
            );
        }

        if (request.getPaymentMode() != null) {
            paymentRequest.setPaymentMode(request.getPaymentMode());
        }

        paymentRequest.setBankLedgerId(request.getBankLedgerId());

        /*
         * Vendor ledger ID is not required from Operation Service.
         * Account Service resolves the vendor ledger from operationVendorId.
         * Retain this only as optional backward-compatible metadata.
         */
        if (request.getLedgerId() != null) {
            paymentRequest.setLedgerId(request.getLedgerId());
        }

        if (hasText(request.getLedgerType())) {
            paymentRequest.setLedgerType(
                    request.getLedgerType().trim()
            );
        }

        /*
         * This is only the actual amount paid through Bank/Cash.
         * It must not be used as the purchase invoice taxable value.
         */
        paymentRequest.setAmount(
                money(request.getBankPaymentAmount())
        );

        if (request.getTdsActive() != null) {
            paymentRequest.setTdsActive(request.getTdsActive());
        }

        if (request.getTdsPercentage() != null) {
            paymentRequest.setTdsPercentage(
                    request.getTdsPercentage()
            );
        }

        if (request.getTdsAmount() != null) {
            paymentRequest.setTdsAmount(
                    money(request.getTdsAmount())
            );
        }

        if (hasText(request.getComment())) {
            paymentRequest.setCompletionRemarks(
                    request.getComment().trim()
            );
        } else if (hasText(request.getRemarks())) {
            paymentRequest.setCompletionRemarks(
                    request.getRemarks().trim()
            );
        }

        if (request.getProofAttachmentUrls() != null) {
            paymentRequest.setProofAttachmentUrls(
                    request.getProofAttachmentUrls()
            );
        }

        paymentRequest.setPaymentReleasedBy(userId);
        paymentRequest.setPaymentReleasedDate(currentDate);
        paymentRequest.setUpdatedDate(currentDate);
        paymentRequest.setStatus(PaymentRequestStatus.PAYMENT_RELEASED);

        ProcurementPaymentRequest saved =
                paymentRequestRepository.saveAndFlush(paymentRequest);

        /*
         * The Account Service creates both:
         * 1. PURCHASE_INVOICE voucher
         * 2. PAYMENT voucher
         */
        AccountVendorSyncResponseDto accountResponse =
                syncVendorWithAccountService(
                        saved,
                        userId,
                        request
                );

        log.info(
                "Payment released and Account Service synchronized. "
                        + "paymentRequestId={}, vendorId={}, "
                        + "invoiceVoucherId={}, paymentVoucherId={}",
                saved.getId(),
                saved.getVendor() != null
                        ? saved.getVendor().getId()
                        : null,
                accountResponse.getVoucherId(),
                accountResponse.getPaymentVoucherId()
        );

        return mapToResponse(saved);
    }

    /*
     * ================================================================
     * BUILD RELEASE REQUEST FOR ACCOUNTS
     * ================================================================
     */
    private VendorPaymentApprovalRequestDto buildPaymentReleaseRequest(
            ProcurementPaymentRequest paymentRequest,
            Long operationUserId,
            ProcurementPaymentActionRequestDto actionRequest
    ) {
        Vendor vendor = paymentRequest.getVendor();

        if (vendor == null) {
            throw new ValidationException(
                    "Vendor is required for payment release synchronization",
                    "ERR_PAYMENT_REQUEST_VENDOR_NOT_FOUND"
            );
        }

        GstPayload gstPayload =
                resolveGstPayload(
                        paymentRequest,
                        vendor
                );

        BigDecimal basicPrice =
                resolveBasicPrice(
                        paymentRequest,
                        gstPayload.gstActive(),
                        gstPayload.gstPercentage()
                );

        String invoiceNumber =
                hasText(actionRequest.getInvoiceNumber())
                        ? actionRequest.getInvoiceNumber().trim()
                        : paymentRequest.getInvoiceNumber();

        LocalDate invoiceDate =
                actionRequest.getInvoiceDate() != null
                        ? actionRequest.getInvoiceDate()
                        : paymentRequest.getInvoiceDate();

        LocalDate paymentDate =
                actionRequest.getPaymentDate() != null
                        ? actionRequest.getPaymentDate()
                        : LocalDate.now();

        BigDecimal bankPaymentAmount =
                money(actionRequest.getBankPaymentAmount());

        String paymentMode =
                hasText(actionRequest.getPaymentMode())
                        ? actionRequest.getPaymentMode().trim()
                        : paymentRequest.getPaymentMode();

        Long bankLedgerId =
                actionRequest.getBankLedgerId() != null
                        ? actionRequest.getBankLedgerId()
                        : paymentRequest.getBankLedgerId();

        Long ledgerId =
                actionRequest.getLedgerId() != null
                        ? actionRequest.getLedgerId()
                        : paymentRequest.getLedgerId();

        String ledgerType =
                hasText(actionRequest.getLedgerType())
                        ? actionRequest.getLedgerType().trim()
                        : paymentRequest.getLedgerType();

        String transactionReference =
                hasText(actionRequest.getTransactionReference())
                        ? actionRequest.getTransactionReference().trim()
                        : paymentRequest.getTransactionReference();

        String paymentProof =
                hasText(actionRequest.getPaymentProof())
                        ? actionRequest.getPaymentProof().trim()
                        : paymentRequest.getPaymentProof();

        BigDecimal tdsAmount =
                actionRequest.getTdsAmount() != null
                        ? money(actionRequest.getTdsAmount())
                        : money(paymentRequest.getTdsAmount());

        Long tdsPayableLedgerId =
                actionRequest.getTdsPayableLedgerId();

        String releaseComment =
                hasText(actionRequest.getComment())
                        ? actionRequest.getComment().trim()
                        : clean(paymentRequest.getCompletionRemarks());

        return VendorPaymentApprovalRequestDto.builder()
                .procurementPaymentRequestId(
                        paymentRequest.getId()
                )
                .procurementOrderId(
                        paymentRequest.getProcurementOrder() != null
                                ? paymentRequest.getProcurementOrder().getId()
                                : null
                )
                .purchaseOrderNumber(
                        paymentRequest.getProcurementOrder() != null
                                ? paymentRequest.getProcurementOrder().getPoNumber()
                                : null
                )
                .invoiceNumber(invoiceNumber)
                .invoiceDate(invoiceDate)
                .price(basicPrice)

                .gstRegistrationType(
                        gstPayload.gstRegistrationType()
                )
                .gstActive(
                        gstPayload.gstActive()
                )
                .gstSupplyType(
                        gstPayload.gstSupplyType()
                )
                .gstStateCode(
                        gstPayload.gstStateCode()
                )
                .gstPercentage(
                        gstPayload.gstPercentage()
                )

                .tdsActive(
                        Boolean.TRUE.equals(
                                paymentRequest.getTdsActive()
                        )
                )
                .tdsPercentage(
                        defaultAmount(
                                paymentRequest.getTdsPercentage()
                        )
                )
                .tdsAmount(tdsAmount)
                .tdsPayableLedgerId(tdsPayableLedgerId)

                .paymentDate(paymentDate)
                .bankPaymentAmount(bankPaymentAmount)
                .paymentMode(paymentMode)
                .bankLedgerId(bankLedgerId)
                .ledgerId(ledgerId)
                .ledgerType(ledgerType)
                .transactionReference(transactionReference)
                .paymentProof(paymentProof)
                .proofAttachmentUrls(
                        actionRequest.getProofAttachmentUrls() != null
                                ? actionRequest.getProofAttachmentUrls()
                                : paymentRequest.getProofAttachmentUrls()
                )

                .approvedByOperationUserId(
                        paymentRequest.getApprovedBy()
                )
                .approvedDate(
                        toLocalDate(
                                paymentRequest.getApprovedDate()
                        )
                )
                .approvalComment(
                        clean(
                                paymentRequest.getCompletionRemarks()
                        )
                )
                .paymentReleasedByOperationUserId(
                        operationUserId
                )
                .paymentReleasedDate(paymentDate)
                .releaseComment(releaseComment)
                .build();
    }

    private String resolveGstSupplyType(
            ProcurementPaymentRequest paymentRequest
    ) {
        if (!Boolean.TRUE.equals(paymentRequest.getGstActive())) {
            return null;
        }

        if (defaultAmount(paymentRequest.getGstPercentage())
                .compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        if (hasText(paymentRequest.getGstType())) {
            String gstType = paymentRequest.getGstType()
                    .trim()
                    .toUpperCase()
                    .replace("-", "_")
                    .replace(" ", "_");

            if ("INTRA_STATE".equals(gstType)
                    || "CGST_SGST".equals(gstType)
                    || "CGST+SGST".equals(gstType)
                    || "CGST_AND_SGST".equals(gstType)) {
                return "INTRA_STATE";
            }

            if ("INTER_STATE".equals(gstType)
                    || "IGST".equals(gstType)) {
                return "INTER_STATE";
            }
        }


        if (defaultAmount(paymentRequest.getIgstAmount())
                .compareTo(BigDecimal.ZERO) > 0) {
            return "INTER_STATE";
        }

        boolean hasCgst = defaultAmount(paymentRequest.getCgstAmount())
                .compareTo(BigDecimal.ZERO) > 0;

        boolean hasSgst = defaultAmount(paymentRequest.getSgstAmount())
                .compareTo(BigDecimal.ZERO) > 0;

        if (hasCgst || hasSgst) {
            return "INTRA_STATE";
        }

        throw new ValidationException(
                "GST supply type could not be determined. "
                        + "Use INTRA_STATE/CGST_SGST for CGST and SGST, "
                        + "or INTER_STATE/IGST for IGST",
                "ERR_GST_SUPPLY_TYPE_REQUIRED"
        );
    }

    private BigDecimal resolveBasicPrice(
            ProcurementPaymentRequest paymentRequest,
            boolean gstActive,
            BigDecimal gstPercentage
    ) {
        BigDecimal invoiceAmount =
                paymentRequest.getInvoiceAmount();

        if (invoiceAmount == null
                || invoiceAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "Invoice amount must be greater than zero",
                    "ERR_INVALID_INVOICE_AMOUNT"
            );
        }

        BigDecimal finalAmount =
                money(invoiceAmount);

        if (!gstActive) {
            return finalAmount;
        }

        BigDecimal safeGstPercentage =
                defaultAmount(gstPercentage);

        if (safeGstPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return finalAmount;
        }

        BigDecimal divisor =
                BigDecimal.ONE.add(
                        safeGstPercentage.divide(
                                BigDecimal.valueOf(100),
                                6,
                                RoundingMode.HALF_UP
                        )
                );

        return finalAmount.divide(
                divisor,
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal defaultAmount(
            BigDecimal value
    ) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    private BigDecimal resolveInvoiceGrossAmount(
            ProcurementPaymentRequest paymentRequest
    ) {
        /*
         * Never use paymentRequest.amount here. That field is the actual
         * Bank/Cash payment captured during release.
         */
        if (paymentRequest.getInvoiceAmount() != null
                && paymentRequest.getInvoiceAmount()
                .compareTo(BigDecimal.ZERO) > 0) {

            return money(paymentRequest.getInvoiceAmount());
        }

        if (paymentRequest.getPayableAmount() != null
                && paymentRequest.getPayableAmount()
                .compareTo(BigDecimal.ZERO) > 0) {

            return money(paymentRequest.getPayableAmount());
        }

        throw new ValidationException(
                "Invoice amount is required for Account Service posting",
                "ERR_INVOICE_AMOUNT_REQUIRED"
        );
    }

    private BigDecimal money(
            BigDecimal value
    ) {
        return value == null
                ? BigDecimal.ZERO
                : value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private Boolean parseBoolean(
            String value
    ) {
        if (!hasText(value)) {
            return false;
        }

        String normalized =
                value.trim().toUpperCase();

        return "YES".equals(normalized)
                || "TRUE".equals(normalized)
                || "Y".equals(normalized)
                || "1".equals(normalized);
    }

    private LocalDate toLocalDate(
            Date value
    ) {
        return value == null
                ? null
                : value.toInstant()
                .atZone(
                        java.time.ZoneId.systemDefault()
                )
                .toLocalDate();
    }

    private ProcurementPaymentRequest getActivePaymentRequest(
            Long paymentRequestId
    ) {
        if (paymentRequestId == null) {
            throw new ValidationException(
                    "Payment request id is required",
                    "ERR_PAYMENT_REQUEST_ID_REQUIRED"
            );
        }

        ProcurementPaymentRequest paymentRequest =
                paymentRequestRepository.findById(
                                paymentRequestId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Procurement payment request not found",
                                        "ERR_PAYMENT_REQUEST_NOT_FOUND"
                                )
                        );

        if (paymentRequest.isDeleted()) {
            throw new ValidationException(
                    "Deleted payment request cannot be processed",
                    "ERR_DELETED_PAYMENT_REQUEST"
            );
        }

        return paymentRequest;
    }

    private void validateUser(
            Long userId
    ) {
        if (userId == null) {
            throw new ValidationException(
                    "User id is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        userRepository.findActiveUserById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found",
                                "ERR_USER_NOT_FOUND"
                        )
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

    private ProcurementPaymentRequestResponseDto mapToResponse(
            ProcurementPaymentRequest request
    ) {
        ProcurementOrder order =
                request.getProcurementOrder();

        Project project =
                request.getProject();

        Vendor vendor =
                request.getVendor();

        return ProcurementPaymentRequestResponseDto.builder()
                .id(request.getId())

                .procurementOrderId(
                        order != null
                                ? order.getId()
                                : null
                )

                .poNumber(
                        order != null
                                ? order.getPoNumber()
                                : null
                )

                .projectId(
                        project != null
                                ? project.getId()
                                : null
                )

                .projectName(
                        project != null
                                ? project.getName()
                                : null
                )

                .projectNo(
                        project != null
                                ? project.getProjectNo()
                                : null
                )

                .vendorId(
                        vendor != null
                                ? vendor.getId()
                                : null
                )

                .vendorName(
                        vendor != null
                                ? vendor.getName()
                                : null
                )

                .invoiceAmount(
                        request.getInvoiceAmount()
                )

                .payableAmount(
                        request.getPayableAmount()
                )

                .invoiceNumber(
                        request.getInvoiceNumber()
                )

                .invoiceDate(
                        request.getInvoiceDate()
                )

                .submissionDate(
                        request.getSubmissionDate()
                )

                .completionRemarks(
                        request.getCompletionRemarks()
                )

                .proofAttachmentUrls(
                        request.getProofAttachmentUrls()
                )

                .status(
                        request.getStatus()
                )

                .approvedDate(
                        request.getApprovedDate()
                )

                .paymentReleasedDate(
                        request.getPaymentReleasedDate()
                )

                .createdBy(
                        request.getCreatedBy()
                )

                .approvedBy(
                        request.getApprovedBy()
                )

                .paymentReleasedBy(
                        request.getPaymentReleasedBy()
                )

                .createdDate(
                        request.getCreatedDate()
                )

                .updatedDate(
                        request.getUpdatedDate()
                )

                .tdsActive(
                        request.getTdsActive()
                )

                .tdsPercentage(
                        request.getTdsPercentage()
                )

                .gstActive(
                        request.getGstActive()
                )

                .gstStateCode(
                        request.getGstStateCode()
                )

                .gstPercentage(
                        request.getGstPercentage()
                )

                .cgstAmount(
                        request.getCgstAmount()
                )

                .sgstAmount(
                        request.getSgstAmount()
                )

                .igstAmount(
                        request.getIgstAmount()
                )

                .totalGstAmount(
                        request.getTotalGstAmount()
                )

                .amount(
                        request.getAmount()
                )

                .paymentMode(
                        request.getPaymentMode()
                )

                .bankLedgerId(
                        request.getBankLedgerId()
                )

                .ledgerId(
                        request.getLedgerId()
                )

                .ledgerType(
                        request.getLedgerType()
                )

                .transactionReference(
                        request.getTransactionReference()
                )

                .paymentProof(
                        request.getPaymentProof()
                )

                .tdsAmount(
                        request.getTdsAmount()
                )

                .gstType(
                        request.getGstType()
                )

                .build();
    }

    private record GstPayload(
            Boolean gstActive,
            String gstRegistrationType,
            String gstSupplyType,
            String gstStateCode,
            BigDecimal gstPercentage
    ) {
    }


    private GstPayload resolveGstPayload(
            ProcurementPaymentRequest paymentRequest,
            Vendor vendor
    ) {
        boolean gstActive =
                Boolean.TRUE.equals(
                        paymentRequest.getGstActive()
                );

        String registrationType =
                vendor.getGstRegistrationType() != null
                        ? vendor.getGstRegistrationType()
                        .name()
                        : null;

        BigDecimal gstPercentage =
                defaultAmount(
                        paymentRequest.getGstPercentage()
                );

        /*
         * INTERNATIONAL and SEZ are zero-rated in Account Service.
         */
        boolean zeroRated =
                "INTERNATIONAL".equalsIgnoreCase(
                        registrationType
                )
                        || "SEZ".equalsIgnoreCase(
                        registrationType
                );

        if (!gstActive || zeroRated) {
            return new GstPayload(
                    false,
                    registrationType,
                    null,
                    clean(
                            paymentRequest.getGstStateCode()
                    ),
                    BigDecimal.ZERO.setScale(
                            2,
                            RoundingMode.HALF_UP
                    )
            );
        }

        if (gstPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "GST percentage must be greater than zero when GST is active",
                    "ERR_GST_PERCENTAGE_REQUIRED"
            );
        }

        String supplyType =
                resolveGstSupplyType(
                        paymentRequest
                );

        return new GstPayload(
                true,
                registrationType,
                supplyType,
                clean(
                        paymentRequest.getGstStateCode()
                ),
                gstPercentage
        );
    }
}

