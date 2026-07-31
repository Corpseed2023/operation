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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

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

        paymentRequest.setAmount(requestDto.getAmount());
        paymentRequest.setPaymentMode(requestDto.getPaymentMode());

        paymentRequest.setBankLedgerId(requestDto.getBankLedgerId());
        paymentRequest.setLedgerId(requestDto.getLedgerId());
        paymentRequest.setLedgerType(requestDto.getLedgerType());

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
         * Operation sends only raw commercial inputs.
         * Account Service calculates GST/TDS and creates
         * the PURCHASE_INVOICE voucher and voucher entries.
         */
        VendorPaymentApprovalRequestDto paymentApproval =
                buildPaymentApprovalRequest(
                        saved,
                        userId,
                        request
                );

        AccountVendorSyncResponseDto accountResponse =
                syncVendorWithAccountService(
                        saved,
                        userId,
                        paymentApproval
                );

        log.info(
                "Payment request approved and Account voucher synchronized. "
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
            VendorPaymentApprovalRequestDto paymentApproval
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
                        .paymentApproval(paymentApproval)
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
                            + "paymentApprovalSent={}, voucherCreated={}, "
                            + "voucherId={}",
                    paymentRequest.getId(),
                    vendorId,
                    response.getExternalVendorId(),
                    response.getLedgerId(),
                    paymentApproval != null,
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

        Date currentDate = new Date();

        paymentRequest.setStatus(
                PaymentRequestStatus.PAYMENT_RELEASED
        );

        if (hasText(request.getInvoiceNumber())) {
            paymentRequest.setInvoiceNumber(
                    request.getInvoiceNumber().trim()
            );
        }

        if (request.getInvoiceDate() != null) {
            paymentRequest.setInvoiceDate(
                    request.getInvoiceDate()
            );
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

        paymentRequest.setPaymentReleasedBy(userId);
        paymentRequest.setPaymentReleasedDate(currentDate);
        paymentRequest.setUpdatedDate(currentDate);

        if (hasText(request.getComment())) {
            paymentRequest.setCompletionRemarks(
                    request.getComment().trim()
            );
        }

        /*
         * This method updates only the Operation-side payment status.
         *
         * The current Account vendor-sync contract creates the
         * PURCHASE_INVOICE voucher during approval. A separate
         * Account Service bank-payment endpoint is required later
         * for:
         *
         * Vendor Ledger Dr
         *      To Bank Ledger
         */
        ProcurementPaymentRequest saved =
                paymentRequestRepository.saveAndFlush(
                        paymentRequest
                );

        log.info(
                "Payment released in Operation Service. "
                        + "paymentRequestId={}, vendorId={}, "
                        + "transactionReference={}",
                saved.getId(),
                saved.getVendor() != null
                        ? saved.getVendor().getId()
                        : null,
                saved.getTransactionReference()
        );

        return mapToResponse(saved);
    }

    private VendorPaymentApprovalRequestDto buildPaymentApprovalRequest(
            ProcurementPaymentRequest paymentRequest,
            Long operationUserId,
            ProcurementPaymentActionRequestDto actionRequest
    ) {
        Vendor vendor = paymentRequest.getVendor();

        if (vendor == null) {
            throw new ValidationException(
                    "Vendor is required for payment approval synchronization",
                    "ERR_PAYMENT_REQUEST_VENDOR_NOT_FOUND"
            );
        }

        String gstRegistrationType =
                vendor.getGstRegistrationType() != null
                        ? vendor.getGstRegistrationType().name()
                        : null;

        BigDecimal basicPrice =
                resolveBasicPrice(paymentRequest);

        String invoiceNumber =
                paymentRequest.getInvoiceNumber();

        LocalDate invoiceDate =
                paymentRequest.getInvoiceDate();

        if (actionRequest != null) {
            if (hasText(actionRequest.getInvoiceNumber())) {
                invoiceNumber =
                        actionRequest.getInvoiceNumber().trim();
            }

            if (actionRequest.getInvoiceDate() != null) {
                invoiceDate =
                        actionRequest.getInvoiceDate();
            }
        }

        if (actionRequest != null
                && actionRequest.getInvoiceDate() != null) {

            invoiceDate =
                    actionRequest.getInvoiceDate();
        }



        return VendorPaymentApprovalRequestDto.builder()
                .procurementPaymentRequestId(
                        paymentRequest.getId()
                )
                .procurementOrderId(
                        paymentRequest.getProcurementOrder() != null
                                ? paymentRequest
                                .getProcurementOrder()
                                .getId()
                                : null
                )
                .purchaseOrderNumber(
                        paymentRequest.getProcurementOrder() != null
                                ? paymentRequest
                                .getProcurementOrder()
                                .getPoNumber()
                                : null
                )
                .invoiceNumber(invoiceNumber)
                .invoiceDate(invoiceDate)
                .price(basicPrice)
                .gstRegistrationType(
                        gstRegistrationType
                )
                /*
                 * Legacy Operation field gstType now represents
                 * supply type: INTRA_STATE or INTER_STATE.
                 */
                .gstSupplyType(
                        resolveGstSupplyType(paymentRequest)
                )
                .gstStateCode(
                        paymentRequest.getGstStateCode()
                )
                .gstPercentage(
                        defaultAmount(
                                paymentRequest.getGstPercentage()
                        )
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
                .approvedByOperationUserId(
                        operationUserId
                )
                .approvedDate(
                        toLocalDate(
                                paymentRequest.getApprovedDate()
                        )
                )
                .approvalComment(
                        actionRequest != null
                                && hasText(actionRequest.getComment())
                                ? actionRequest .getComment().trim()
                                : clean(
                                paymentRequest.getCompletionRemarks()
                        )
                )
                .build();
    }
    private String resolveGstSupplyType(
            ProcurementPaymentRequest paymentRequest
    ) {
        if (!Boolean.TRUE.equals(paymentRequest.getGstActive())) {
            return null;
        }

        if (paymentRequest.getIgstAmount() != null
                && paymentRequest.getIgstAmount()
                .compareTo(BigDecimal.ZERO) > 0) {

            return "INTER_STATE";
        }

        boolean hasCgst =
                paymentRequest.getCgstAmount() != null
                        && paymentRequest.getCgstAmount()
                        .compareTo(BigDecimal.ZERO) > 0;

        boolean hasSgst =
                paymentRequest.getSgstAmount() != null
                        && paymentRequest.getSgstAmount()
                        .compareTo(BigDecimal.ZERO) > 0;

        if (hasCgst || hasSgst) {
            return "INTRA_STATE";
        }

        if (paymentRequest.getGstType() != null) {
            String gstType = paymentRequest.getGstType()
                    .toString()
                    .trim()
                    .toUpperCase();

            if ("INTRA_STATE".equals(gstType)
                    || "INTER_STATE".equals(gstType)) {
                return gstType;
            }
        }

        throw new ValidationException(
                "GST supply type could not be determined. "
                        + "Provide IGST for inter-state or CGST/SGST for intra-state",
                "ERR_GST_SUPPLY_TYPE_REQUIRED"
        );
    }


    private BigDecimal resolveBasicPrice(
            ProcurementPaymentRequest paymentRequest
    ) {
        if (paymentRequest.getAmount() != null
                && paymentRequest.getAmount()
                .compareTo(BigDecimal.ZERO) > 0) {

            return paymentRequest.getAmount();
        }

        if (paymentRequest.getInvoiceAmount() != null
                && paymentRequest.getInvoiceAmount()
                .compareTo(BigDecimal.ZERO) > 0) {

            return paymentRequest.getInvoiceAmount();
        }

        throw new ValidationException(
                "Basic vendor price is required for Account Service accounting",
                "ERR_VENDOR_BASIC_PRICE_REQUIRED"
        );
    }

    private BigDecimal defaultAmount(
            BigDecimal value
    ) {
        return value == null
                ? BigDecimal.ZERO
                : value;
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
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode())

                .bankLedgerId(request.getBankLedgerId())
                .ledgerId(request.getLedgerId())
                .ledgerType(request.getLedgerType())

                .transactionReference(request.getTransactionReference())
                .paymentProof(request.getPaymentProof())

                .tdsAmount(request.getTdsAmount())
                .gstType(request.getGstType())
                .build();
    }
}