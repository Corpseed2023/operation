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
                && paymentRequest.getStatus() != PaymentRequestStatus.UNDER_REVIEW) {

            throw new ValidationException(
                    "Only PENDING or UNDER_REVIEW payment request can be approved. Current status: "
                            + paymentRequest.getStatus(),
                    "ERR_INVALID_PAYMENT_REQUEST_STATUS"
            );
        }

        Date currentDate = new Date();

        paymentRequest.setStatus(PaymentRequestStatus.APPROVED);
        paymentRequest.setApprovedBy(userId);
        paymentRequest.setApprovedDate(currentDate);
        paymentRequest.setUpdatedDate(currentDate);

        if (request != null
                && request.getComment() != null
                && !request.getComment().trim().isEmpty()) {

            paymentRequest.setCompletionRemarks(
                    request.getComment().trim()
            );
        }

        /*
         * saveAndFlush ensures that the payment request update is executed
         * before calling Account Service.
         */
        ProcurementPaymentRequest saved =
                paymentRequestRepository.saveAndFlush(paymentRequest);

        /*
         * Synchronise the vendor with Account Service after approval.
         */
        syncVendorWithAccountService(saved, userId);

        return mapToResponse(saved);
    }

    private void syncVendorWithAccountService(
            ProcurementPaymentRequest paymentRequest,
            Long approvedByUserId
    ) {
        if (paymentRequest.getVendor() == null) {
            throw new ValidationException(
                    "Vendor is not available against payment request ID: "
                            + paymentRequest.getId(),
                    "ERR_PAYMENT_REQUEST_VENDOR_NOT_FOUND"
            );
        }

        Vendor vendor = paymentRequest.getVendor();
        Long vendorId = vendor.getId();

        VendorAccountsSubmission accountsSubmission =
                vendorAccountsSubmissionRepository
                        .findFirstByVendor_IdOrderByIdDesc(vendorId)
                        .orElseThrow(() -> new ValidationException(
                                "Vendor accounts submission not found for vendor ID: "
                                        + vendorId,
                                "ERR_VENDOR_ACCOUNTS_SUBMISSION_NOT_FOUND"
                        ));

        VendorFinalization vendorFinalization =
                vendorFinalizationRepository
                        .findFirstByVendor_IdOrderByIdDesc(vendorId)
                        .orElseThrow(() -> new ValidationException(
                                "Vendor finalization not found for vendor ID: "
                                        + vendorId,
                                "ERR_VENDOR_FINALIZATION_NOT_FOUND"
                        ));

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
                        .gstNumber(vendor.getGstNumber())

                        .gstRegistrationType(
                                vendor.getGstRegistrationType() != null
                                        ? vendor.getGstRegistrationType().name()
                                        : null
                        )

                        .accountHolderName(
                                accountsSubmission.getAccountHolderName()
                        )
                        .bankAccountNumber(
                                accountsSubmission.getAccountNumber()
                        )
                        .ifscCode(
                                accountsSubmission.getIfsc()
                        )
//                        .bankName(
//                                accountsSubmission.getBankName()
//                        )
                        .branchAddress(
                                accountsSubmission.getBranchAddress()
                        )

                        .fullAddress(accountsSubmission.getBranchAddress())
                        .city(vendor.getCity())
                        .state(vendor.getState())
                        .country(vendor.getCountry())

                        .active(
                                vendor.getStatus() != VendorStatus.ACTIVE
                                        ? Boolean.FALSE
                                        : Boolean.TRUE
                        )

                        .approvedByOperationUserId(approvedByUserId)
                        .approvedAt(currentDateTime)
                        .operationUpdatedAt(currentDateTime)
                        .build();

        try {
            AccountVendorSyncResponseDto response =
                    accountFeignClient.syncVendor(syncRequest);

            if (response == null) {
                throw new ValidationException(
                        "Account Service returned an empty response for vendor ID: "
                                + vendorId,
                        "ERR_EMPTY_ACCOUNT_VENDOR_SYNC_RESPONSE"
                );
            }

            log.info(
                    "Vendor synced with Account Service successfully. " +
                            "paymentRequestId={}, vendorId={}, accountsSubmissionId={}, vendorFinalizationId={}",
                    paymentRequest.getId(),
                    vendorId,
                    accountsSubmission.getId(),
                    vendorFinalization.getId()
            );

        } catch (FeignException exception) {
            log.error(
                    "Vendor sync failed. paymentRequestId={}, vendorId={}, status={}, response={}",
                    paymentRequest.getId(),
                    vendorId,
                    exception.status(),
                    exception.contentUTF8(),
                    exception
            );

            throw new ValidationException(
                    "Unable to synchronise vendor with Account Service",
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

        ProcurementPaymentRequest paymentRequest = getActivePaymentRequest(paymentRequestId);

        if (paymentRequest.getStatus() != PaymentRequestStatus.APPROVED
                && paymentRequest.getStatus() != PaymentRequestStatus.PAYMENT_PROCESSING) {
            throw new ValidationException(
                    "Only APPROVED or PAYMENT_PROCESSING payment request can be released. Current status: "
                            + paymentRequest.getStatus(),
                    "ERR_INVALID_PAYMENT_REQUEST_STATUS"
            );
        }

        paymentRequest.setStatus(PaymentRequestStatus.PAYMENT_RELEASED);
        paymentRequest.setInvoiceNumber(request.getInvoiceNumber());
        paymentRequest.setInvoiceDate(request.getInvoiceDate());
        paymentRequest.setPaymentReleasedBy(userId);
        paymentRequest.setPaymentReleasedDate(new Date());
        paymentRequest.setUpdatedDate(new Date());

        if (request != null && request.getComment() != null && !request.getComment().trim().isEmpty()) {
            paymentRequest.setCompletionRemarks(request.getComment().trim());
        }

        ProcurementPaymentRequest saved = paymentRequestRepository.save(paymentRequest);

        return mapToResponse(saved);
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