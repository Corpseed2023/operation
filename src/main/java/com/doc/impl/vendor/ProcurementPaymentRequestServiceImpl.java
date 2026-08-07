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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementPaymentRequestServiceImpl
        implements ProcurementPaymentRequestService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final ProcurementPaymentRequestRepository paymentRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final VendorAccountsSubmissionRepository vendorAccountsSubmissionRepository;
    private final VendorFinalizationRepository vendorFinalizationRepository;
    private final AccountFeignClient accountFeignClient;


    // ================================================================
    // CREATE PAYMENT REQUEST
    // ================================================================

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
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "CreatedBy user not found",
                                "ERR_USER_NOT_FOUND"
                        )
                );

        ProcurementOrder order =
                purchaseOrderRepository.findById(procurementOrderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Procurement order not found",
                                        "ERR_PROCUREMENT_ORDER_NOT_FOUND"
                                )
                        );

        if (order.isDeleted()) {
            throw new ValidationException(
                    "Deleted procurement order cannot be used for payment request",
                    "ERR_DELETED_PROCUREMENT_ORDER"
            );
        }

        if (order.getFinalAmount() == null
                || order.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "Final amount is not configured for this procurement order",
                    "ERR_PO_FINAL_AMOUNT_MISSING"
            );
        }

        if (requestDto.getAmount() == null
                || requestDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "Payment amount must be greater than zero",
                    "ERR_INVALID_PAYMENT_AMOUNT"
            );
        }

        BigDecimal newPaymentAmount =
                money(requestDto.getAmount());

        if (newPaymentAmount.compareTo(
                money(order.getFinalAmount())
        ) > 0) {

            throw new ValidationException(
                    "Payment request amount cannot be greater than procurement order final amount",
                    "ERR_PAYMENT_AMOUNT_EXCEEDS_PO_FINAL_AMOUNT"
            );
        }

        BigDecimal existingPaymentAmount =
                paymentRequestRepository
                        .sumAmountByProcurementOrderAndIsDeletedFalse(order);

        existingPaymentAmount =
                existingPaymentAmount == null
                        ? zeroMoney()
                        : money(existingPaymentAmount);

        BigDecimal totalPaymentAmount =
                money(
                        existingPaymentAmount.add(
                                newPaymentAmount
                        )
                );

        if (totalPaymentAmount.compareTo(
                money(order.getFinalAmount())
        ) > 0) {

            throw new ValidationException(
                    "Total payment request amount cannot exceed procurement order final amount",
                    "ERR_PAYMENT_REQUEST_AMOUNT_EXCEEDS_PO"
            );
        }

        /*
         * ================================================================
         * GST CONFIGURATION
         * ================================================================
         */

        Boolean effectiveGstActive =
                Boolean.TRUE.equals(
                        requestDto.getGstActive()
                );

        BigDecimal effectiveGstPercentage =
                requestDto.getGstPercentage();

        String effectiveGstStateCode =
                clean(
                        requestDto.getGstStateCode()
                );

        String effectiveGstType =
                clean(
                        requestDto.getGstType()
                );

        Optional<ProcurementPaymentRequest> firstPaymentRequestOptional =
                paymentRequestRepository
                        .findFirstByProcurementOrderAndIsDeletedFalseOrderByCreatedDateAsc(
                                order
                        );

        /*
         * Keep the same GST configuration across payment requests
         * of the same Procurement Order.
         */
        if (firstPaymentRequestOptional.isPresent()) {

            ProcurementPaymentRequest firstPaymentRequest =
                    firstPaymentRequestOptional.get();

            effectiveGstActive =
                    Boolean.TRUE.equals(
                            firstPaymentRequest.getGstActive()
                    );

            effectiveGstPercentage =
                    firstPaymentRequest.getGstPercentage();

            effectiveGstStateCode =
                    clean(
                            firstPaymentRequest.getGstStateCode()
                    );

            effectiveGstType =
                    clean(
                            firstPaymentRequest.getGstType()
                    );
        }

        if (Boolean.TRUE.equals(effectiveGstActive)) {

            validatePercentage(
                    effectiveGstPercentage,
                    "GST percentage",
                    "ERR_GST_PERCENTAGE_REQUIRED"
            );

            effectiveGstPercentage =
                    money(effectiveGstPercentage);

            if (!hasText(effectiveGstType)) {

                throw new ValidationException(
                        "GST type is required when GST is active",
                        "ERR_GST_TYPE_REQUIRED"
                );
            }

        } else {

            effectiveGstPercentage = null;
            effectiveGstStateCode = null;
            effectiveGstType = null;
        }

        /*
         * ================================================================
         * TDS CONFIGURATION
         * ================================================================
         */

        boolean tdsActive =
                Boolean.TRUE.equals(
                        requestDto.getTdsActive()
                );

        BigDecimal tdsPercentage = null;

        if (tdsActive) {

            validatePercentage(
                    requestDto.getTdsPercentage(),
                    "TDS percentage",
                    "ERR_TDS_PERCENTAGE_REQUIRED"
            );

            tdsPercentage =
                    money(
                            requestDto.getTdsPercentage()
                    );
        }

        /*
         * ================================================================
         * CREATE ENTITY
         * ================================================================
         */

        ProcurementPaymentRequest paymentRequest =
                new ProcurementPaymentRequest();

        paymentRequest.setProcurementOrder(order);
        paymentRequest.setProject(order.getProject());
        paymentRequest.setVendor(order.getVendor());

        /*
         * amount = taxable/basic procurement amount.
         *
         * NEVER replace this with the final bank payment.
         */
        paymentRequest.setAmount(
                newPaymentAmount
        );

        /*
         * Existing create contract may still supply these.
         *
         * Final authoritative values are recalculated during release.
         */
        if (requestDto.getInvoiceAmount() != null) {
            paymentRequest.setInvoiceAmount(
                    money(requestDto.getInvoiceAmount())
            );
        }

        if (requestDto.getPayableAmount() != null) {
            paymentRequest.setPayableAmount(
                    money(requestDto.getPayableAmount())
            );
        }

        paymentRequest.setSubmissionDate(
                new Date()
        );

        paymentRequest.setCompletionRemarks(
                clean(
                        requestDto.getCompletionRemarks()
                )
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

        Date currentDate =
                new Date();

        paymentRequest.setCreatedDate(currentDate);
        paymentRequest.setUpdatedDate(currentDate);
        paymentRequest.setDeleted(false);

        /*
         * Store configuration only.
         */
        paymentRequest.setTdsActive(
                tdsActive
        );

        paymentRequest.setTdsPercentage(
                tdsPercentage
        );

        paymentRequest.setTdsAmount(
                null
        );

        paymentRequest.setGstActive(
                effectiveGstActive
        );

        paymentRequest.setGstStateCode(
                effectiveGstStateCode
        );

        paymentRequest.setGstPercentage(
                effectiveGstPercentage
        );

        paymentRequest.setGstType(
                effectiveGstType
        );

        /*
         * Final GST amounts are calculated during release.
         */
        paymentRequest.setCgstAmount(null);
        paymentRequest.setSgstAmount(null);
        paymentRequest.setIgstAmount(null);
        paymentRequest.setTotalGstAmount(null);

        ProcurementPaymentRequest saved =
                paymentRequestRepository.save(
                        paymentRequest
                );

        log.info(
                "[PROCUREMENT-PAYMENT-CREATED] "
                        + "paymentRequestId={} | procurementOrderId={} | "
                        + "basicAmount={} | gstActive={} | gstPercentage={} | "
                        + "gstType={} | tdsActive={} | tdsPercentage={}",
                saved.getId(),
                order.getId(),
                saved.getAmount(),
                saved.getGstActive(),
                saved.getGstPercentage(),
                saved.getGstType(),
                saved.getTdsActive(),
                saved.getTdsPercentage()
        );

        return mapToResponse(saved);
    }


    // ================================================================
    // GET BY STATUS
    // ================================================================

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
                        Math.max(page, 0),
                        size <= 0 ? 20 : size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdDate"
                        )
                );

        Page<ProcurementPaymentRequest> requests;

        if (status == null) {

            requests =
                    paymentRequestRepository
                            .findByIsDeletedFalse(
                                    pageable
                            );

        } else {

            requests =
                    paymentRequestRepository
                            .findByStatusAndIsDeletedFalse(
                                    status,
                                    pageable
                            );
        }

        return requests.map(
                this::mapToResponse
        );
    }


    // ================================================================
    // GET BY PROCUREMENT ORDER
    // ================================================================

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
                purchaseOrderRepository
                        .findById(
                                procurementOrderId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Procurement order not found",
                                        "ERR_PROCUREMENT_ORDER_NOT_FOUND"
                                )
                        );

        if (order.isDeleted()) {

            throw new ValidationException(
                    "Deleted procurement order cannot be used for fetching payment requests",
                    "ERR_DELETED_PROCUREMENT_ORDER"
            );
        }

        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        size <= 0 ? 20 : size,
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

        return requests.map(
                this::mapToResponse
        );
    }


    // ================================================================
    // APPROVE
    // ================================================================

    @Override
    @Transactional
    public ProcurementPaymentRequestResponseDto approvePaymentRequest(
            Long paymentRequestId,
            Long userId,
            ProcurementPaymentActionRequestDto request
    ) {

        validateUser(userId);

        ProcurementPaymentRequest paymentRequest =
                getActivePaymentRequest(
                        paymentRequestId
                );

        if (paymentRequest.getStatus()
                != PaymentRequestStatus.PENDING
                &&
                paymentRequest.getStatus()
                        != PaymentRequestStatus.UNDER_REVIEW) {

            throw new ValidationException(
                    "Only PENDING or UNDER_REVIEW payment request can be approved. "
                            + "Current status: "
                            + paymentRequest.getStatus(),
                    "ERR_INVALID_PAYMENT_REQUEST_STATUS"
            );
        }

        Vendor vendor =
                paymentRequest.getVendor();

        if (vendor == null
                || vendor.getId() == null) {

            throw new ValidationException(
                    "Vendor is not available against payment request ID: "
                            + paymentRequestId,
                    "ERR_PAYMENT_REQUEST_VENDOR_NOT_FOUND"
            );
        }

        if (vendor.getStatus()
                != VendorStatus.ACTIVE) {

            throw new ValidationException(
                    "Only an ACTIVE vendor payment request can be approved",
                    "ERR_VENDOR_NOT_ACTIVE"
            );
        }

        if (request != null) {


            if (hasText(
                    request.getComment()
            )) {

                paymentRequest.setCompletionRemarks(
                        request.getComment()
                                .trim()
                );
            }
        }

        Date currentDate =
                new Date();

        paymentRequest.setStatus(
                PaymentRequestStatus.APPROVED
        );

        paymentRequest.setApprovedBy(
                userId
        );

        paymentRequest.setApprovedDate(
                currentDate
        );

        paymentRequest.setUpdatedDate(
                currentDate
        );

        ProcurementPaymentRequest saved =
                paymentRequestRepository
                        .saveAndFlush(
                                paymentRequest
                        );

        log.info(
                "[PROCUREMENT-PAYMENT-APPROVED] "
                        + "paymentRequestId={} | vendorId={}",
                saved.getId(),
                vendor.getId()
        );

        /*
         * Do NOT call Account Service here.
         */
        return mapToResponse(saved);
    }


    // ================================================================
    // REJECT
    // ================================================================

    @Override
    @Transactional
    public ProcurementPaymentRequestResponseDto rejectPaymentRequest(
            Long paymentRequestId,
            Long userId,
            ProcurementPaymentActionRequestDto request
    ) {

        validateUser(userId);

        ProcurementPaymentRequest paymentRequest =
                getActivePaymentRequest(
                        paymentRequestId
                );

        if (paymentRequest.getStatus()
                != PaymentRequestStatus.PENDING
                &&
                paymentRequest.getStatus()
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
                        ? clean(
                        request.getReason()
                )
                        : null;

        if (!hasText(reason)) {

            throw new ValidationException(
                    "Rejection reason is required",
                    "ERR_REJECTION_REASON_REQUIRED"
            );
        }

        paymentRequest.setStatus(
                PaymentRequestStatus.REJECTED
        );

        paymentRequest.setUpdatedDate(
                new Date()
        );

        paymentRequest.setCompletionRemarks(
                reason
        );

        ProcurementPaymentRequest saved =
                paymentRequestRepository
                        .save(
                                paymentRequest
                        );

        return mapToResponse(saved);
    }


    // ================================================================
    // RELEASE PAYMENT
    // ================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        /*
         * PESSIMISTIC LOCK:
         *
         * Prevents two users/requests releasing the same vendor payment
         * at the same time.
         */
        ProcurementPaymentRequest paymentRequest =
                getActivePaymentRequestForUpdate(
                        paymentRequestId
                );

        if (paymentRequest.getStatus()
                != PaymentRequestStatus.APPROVED
                &&
                paymentRequest.getStatus()
                        != PaymentRequestStatus.PAYMENT_PROCESSING) {

            throw new ValidationException(
                    "Only APPROVED or PAYMENT_PROCESSING payment request can be released. "
                            + "Current status: "
                            + paymentRequest.getStatus(),
                    "ERR_INVALID_PAYMENT_REQUEST_STATUS"
            );
        }

        VendorSyncContext vendorContext =
                resolveVendorSyncContext(
                        paymentRequest
                );

        Vendor vendor =
                vendorContext.vendor();

        if (vendor.getStatus()
                != VendorStatus.ACTIVE) {

            throw new ValidationException(
                    "Payment can be released only for an ACTIVE vendor",
                    "ERR_VENDOR_NOT_ACTIVE"
            );
        }

        /*
         * ================================================================
         * RELEASE INPUT VALIDATION
         * ================================================================
         */

        if (request.getBankLedgerId() == null
                || request.getBankLedgerId() <= 0) {

            throw new ValidationException(
                    "Bank/Cash ledger id is required for payment release",
                    "ERR_BANK_LEDGER_REQUIRED"
            );
        }

        if (!hasText(
                request.getPaymentMode()
        )) {

            throw new ValidationException(
                    "Payment mode is required for payment release",
                    "ERR_PAYMENT_MODE_REQUIRED"
            );
        }

        /*
         * ================================================================
         * BACKEND TAX CALCULATION
         * ================================================================
         *
         * Frontend sends NO:
         *
         * bankPaymentAmount
         * GST amounts
         * TDS amount
         * TDS percentage
         * vendor ledger
         * TDS ledger
         */

        GstPayload gstPayload =
                resolveGstPayload(
                        paymentRequest,
                        vendorContext.gstRegistrationType()
                );

        PaymentCalculation calculation =
                calculatePayment(
                        paymentRequest,
                        gstPayload
                );

        /*
         * Persist Operation-side calculation.
         */
        applyOperationCalculation(
                paymentRequest,
                calculation
        );


        paymentRequest.setBankLedgerId(
                request.getBankLedgerId()
        );

        paymentRequest.setPaymentMode(
                request.getPaymentMode()
                        .trim()
        );

        if (hasText(
                request.getTransactionReference()
        )) {

            paymentRequest.setTransactionReference(
                    request.getTransactionReference()
                            .trim()
            );
        }

        if (hasText(
                request.getPaymentProof()
        )) {

            paymentRequest.setPaymentProof(
                    request.getPaymentProof()
                            .trim()
            );
        }

        if (request.getProofAttachmentUrls() != null) {

            paymentRequest.setProofAttachmentUrls(
                    request.getProofAttachmentUrls()
            );
        }

        if (hasText(
                request.getComment()
        )) {

            paymentRequest.setCompletionRemarks(
                    request.getComment()
                            .trim()
            );
        }

        /*
         * Operation does not own vendor ledger selection.
         */
        paymentRequest.setLedgerId(null);
        paymentRequest.setLedgerType(null);

        paymentRequest.setStatus(
                PaymentRequestStatus.PAYMENT_PROCESSING
        );

        paymentRequest.setUpdatedDate(
                new Date()
        );

        ProcurementPaymentRequest processing =
                paymentRequestRepository
                        .saveAndFlush(
                                paymentRequest
                        );

        log.info(
                "[VENDOR-PAYMENT-CALCULATED] "
                        + "paymentRequestId={} | vendorId={} | "
                        + "basic={} | cgst={} | sgst={} | igst={} | "
                        + "totalGst={} | grossInvoice={} | "
                        + "tds={} | bankPayment={}",
                processing.getId(),
                vendor.getId(),
                calculation.basicAmount(),
                calculation.cgstAmount(),
                calculation.sgstAmount(),
                calculation.igstAmount(),
                calculation.totalGstAmount(),
                calculation.grossInvoiceAmount(),
                calculation.tdsAmount(),
                calculation.bankPaymentAmount()
        );

        /*
         * ================================================================
         * ACCOUNT SERVICE
         * ================================================================
         *
         * Existing Account Service is not changed.
         */

        AccountVendorSyncResponseDto accountResponse =
                syncVendorWithAccountService(
                        processing,
                        userId,
                        request,
                        vendorContext,
                        gstPayload,
                        calculation
                );

        /*
         * Optional but important:
         *
         * Account also calculates the accounting amounts.
         * Reject the transaction if its calculation differs from Operation.
         */
        validateAccountCalculation(
                calculation,
                accountResponse
        );

        /*
         * Use Account response as final reconciliation result.
         */
        applyAccountCalculatedAmounts(
                processing,
                accountResponse
        );

        /*
         * Operation-calculated bank amount remains authoritative
         * for the actual payment being released.
         */
        processing.setBankPaymentAmount(
                calculation.bankPaymentAmount()
        );

        Date releasedAt =
                new Date();

        processing.setPaymentReleasedBy(
                userId
        );

        processing.setPaymentReleasedDate(
                releasedAt
        );

        processing.setUpdatedDate(
                releasedAt
        );

        processing.setStatus(
                PaymentRequestStatus.PAYMENT_RELEASED
        );

        ProcurementPaymentRequest saved =
                paymentRequestRepository
                        .saveAndFlush(
                                processing
                        );

        log.info(
                "[VENDOR-PAYMENT-RELEASED] "
                        + "paymentRequestId={} | vendorId={} | "
                        + "basicAmount={} | grossInvoice={} | "
                        + "totalGst={} | tds={} | bankPayment={} | "
                        + "invoiceVoucherId={} | paymentVoucherId={}",
                saved.getId(),
                vendor.getId(),
                saved.getAmount(),
                saved.getInvoiceAmount(),
                saved.getTotalGstAmount(),
                saved.getTdsAmount(),
                saved.getBankPaymentAmount(),
                accountResponse.getVoucherId(),
                accountResponse.getPaymentVoucherId()
        );

        return mapToResponse(saved);
    }


    // ================================================================
    // OPERATION PAYMENT CALCULATION
    // ================================================================

    private PaymentCalculation calculatePayment(
            ProcurementPaymentRequest paymentRequest,
            GstPayload gstPayload
    ) {

        if (paymentRequest == null) {

            throw new ValidationException(
                    "Payment request is required for calculation",
                    "ERR_PAYMENT_REQUEST_REQUIRED"
            );
        }

        /*
         * IMPORTANT:
         *
         * amount = taxable/basic procurement amount.
         *
         * Do not reverse-calculate it from invoiceAmount.
         */
        BigDecimal basicAmount =
                money(
                        paymentRequest.getAmount()
                );

        if (basicAmount.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new ValidationException(
                    "Taxable/basic payment amount must be greater than zero",
                    "ERR_INVALID_PAYMENT_AMOUNT"
            );
        }

        BigDecimal cgstAmount =
                zeroMoney();

        BigDecimal sgstAmount =
                zeroMoney();

        BigDecimal igstAmount =
                zeroMoney();

        BigDecimal totalGstAmount =
                zeroMoney();

        if (Boolean.TRUE.equals(
                gstPayload.gstActive()
        )) {

            validatePercentage(
                    gstPayload.gstPercentage(),
                    "GST percentage",
                    "ERR_GST_PERCENTAGE_REQUIRED"
            );

            totalGstAmount =
                    percentageOf(
                            basicAmount,
                            gstPayload.gstPercentage()
                    );

            if ("INTRA_STATE".equalsIgnoreCase(
                    gstPayload.gstSupplyType()
            )) {

                /*
                 * Split the already-rounded total.
                 *
                 * This guarantees:
                 *
                 * CGST + SGST == total GST
                 */
                cgstAmount =
                        totalGstAmount.divide(
                                BigDecimal.valueOf(2),
                                MONEY_SCALE,
                                MONEY_ROUNDING
                        );

                sgstAmount =
                        money(
                                totalGstAmount.subtract(
                                        cgstAmount
                                )
                        );

            } else if ("INTER_STATE".equalsIgnoreCase(
                    gstPayload.gstSupplyType()
            )) {

                igstAmount =
                        totalGstAmount;

            } else {

                throw new ValidationException(
                        "Invalid GST supply type: "
                                + gstPayload.gstSupplyType(),
                        "ERR_INVALID_GST_SUPPLY_TYPE"
                );
            }
        }

        BigDecimal grossInvoiceAmount =
                money(
                        basicAmount.add(
                                totalGstAmount
                        )
                );

        /*
         * ================================================================
         * TDS
         * ================================================================
         *
         * TDS is calculated on taxable/basic value,
         * not on GST-inclusive invoice amount.
         */

        BigDecimal tdsAmount =
                zeroMoney();

        if (Boolean.TRUE.equals(
                paymentRequest.getTdsActive()
        )) {

            validatePercentage(
                    paymentRequest.getTdsPercentage(),
                    "TDS percentage",
                    "ERR_TDS_PERCENTAGE_REQUIRED"
            );

            tdsAmount =
                    percentageOf(
                            basicAmount,
                            paymentRequest.getTdsPercentage()
                    );
        }

        BigDecimal vendorNetPayable =
                money(
                        grossInvoiceAmount.subtract(
                                tdsAmount
                        )
                );

        if (vendorNetPayable.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new ValidationException(
                    "Vendor payable amount must be greater than zero after TDS deduction",
                    "ERR_INVALID_VENDOR_NET_PAYABLE"
            );
        }

        /*
         * Current flow = full settlement.
         *
         * Actual bank/cash payment is the vendor net payable.
         */
        BigDecimal bankPaymentAmount =
                vendorNetPayable;

        return new PaymentCalculation(
                basicAmount,
                cgstAmount,
                sgstAmount,
                igstAmount,
                totalGstAmount,
                grossInvoiceAmount,
                tdsAmount,
                vendorNetPayable,
                bankPaymentAmount
        );
    }


    private void applyOperationCalculation(
            ProcurementPaymentRequest paymentRequest,
            PaymentCalculation calculation
    ) {

        /*
         * DO NOT call paymentRequest.setAmount(...) here.
         *
         * The original basic amount must remain unchanged.
         */

        paymentRequest.setCgstAmount(
                calculation.cgstAmount()
        );

        paymentRequest.setSgstAmount(
                calculation.sgstAmount()
        );

        paymentRequest.setIgstAmount(
                calculation.igstAmount()
        );

        paymentRequest.setTotalGstAmount(
                calculation.totalGstAmount()
        );

        paymentRequest.setInvoiceAmount(
                calculation.grossInvoiceAmount()
        );

        paymentRequest.setTdsAmount(
                calculation.tdsAmount()
        );

        paymentRequest.setPayableAmount(
                calculation.vendorNetPayableAmount()
        );

        paymentRequest.setBankPaymentAmount(
                calculation.bankPaymentAmount()
        );
    }


    // ================================================================
    // VENDOR ACCOUNTING CONTEXT
    // ================================================================

    private VendorSyncContext resolveVendorSyncContext(
            ProcurementPaymentRequest paymentRequest
    ) {

        Vendor vendor =
                paymentRequest.getVendor();

        if (vendor == null
                || vendor.getId() == null) {

            throw new ValidationException(
                    "Vendor is not available against payment request ID: "
                            + paymentRequest.getId(),
                    "ERR_PAYMENT_REQUEST_VENDOR_NOT_FOUND"
            );
        }

        Long vendorId =
                vendor.getId();

        VendorAccountsSubmission accountsSubmission =
                vendorAccountsSubmissionRepository
                        .findFirstByVendor_IdOrderByIdDesc(
                                vendorId
                        )
                        .orElseThrow(() ->
                                new ValidationException(
                                        "Vendor accounts submission not found for vendor ID: "
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
                                        "Vendor finalization not found for vendor ID: "
                                                + vendorId,
                                        "ERR_VENDOR_FINALIZATION_NOT_FOUND"
                                )
                        );

        String gstRegistrationType =
                accountsSubmission.getGstRegistrationType() != null
                        ? accountsSubmission
                        .getGstRegistrationType()
                        .name()
                        : vendor.getGstRegistrationType() != null
                        ? vendor.getGstRegistrationType()
                        .name()
                        : null;

        String gstNumber =
                hasText(
                        accountsSubmission.getGstNumber()
                )
                        ? accountsSubmission
                        .getGstNumber()
                        .trim()
                        : clean(
                        vendor.getGstNumber()
                );

        return new VendorSyncContext(
                vendor,
                accountsSubmission,
                vendorFinalization,
                gstRegistrationType,
                gstNumber
        );
    }


    // ================================================================
    // SYNC WITH EXISTING ACCOUNT SERVICE
    // ================================================================

    private AccountVendorSyncResponseDto syncVendorWithAccountService(
            ProcurementPaymentRequest paymentRequest,
            Long operationUserId,
            ProcurementPaymentActionRequestDto actionRequest,
            VendorSyncContext context,
            GstPayload gstPayload,
            PaymentCalculation calculation
    ) {

        Vendor vendor =
                context.vendor();

        VendorAccountsSubmission accountsSubmission =
                context.accountsSubmission();

        VendorFinalization vendorFinalization =
                context.vendorFinalization();

        VendorPaymentApprovalRequestDto paymentApproval =
                buildPaymentReleaseRequest(
                        paymentRequest,
                        operationUserId,
                        actionRequest,
                        gstPayload,
                        calculation
                );

        LocalDateTime now =
                LocalDateTime.now();

        AccountVendorSyncRequestDto syncRequest =
                AccountVendorSyncRequestDto.builder()

                        .operationVendorId(
                                vendor.getId()
                        )

                        .vendorAccountsSubmissionId(
                                accountsSubmission.getId()
                        )

                        .vendorFinalizationId(
                                vendorFinalization.getId()
                        )

                        .vendorName(
                                vendor.getName()
                        )

                        .email(
                                vendor.getEmail()
                        )

                        .mobile(
                                vendor.getMobile()
                        )

                        .pan(
                                vendor.getPanNumber()
                        )

                        .gstNumber(
                                context.gstNumber()
                        )

                        .gstRegistrationType(
                                context.gstRegistrationType()
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

                        /*
                         * Current entity does not expose bank name.
                         */
                        .bankName(null)

                        .branchAddress(
                                accountsSubmission.getBranchAddress()
                        )

                        .fullAddress(
                                hasText(
                                        vendor.getFullAddress()
                                )
                                        ? vendor.getFullAddress()
                                        .trim()
                                        : clean(
                                        accountsSubmission
                                                .getBranchAddress()
                                )
                        )

                        .city(
                                vendor.getCity()
                        )

                        .state(
                                vendor.getState()
                        )

                        .country(
                                vendor.getCountry()
                        )

                        .active(
                                vendor.getStatus()
                                        == VendorStatus.ACTIVE
                        )

                        .approvedByOperationUserId(
                                operationUserId
                        )

                        .approvedAt(
                                now
                        )

                        .operationUpdatedAt(
                                now
                        )

                        .paymentApproval(
                                paymentApproval
                        )

                        .build();

        try {

            log.info(
                    "[ACCOUNT-VENDOR-SYNC-START] "
                            + "paymentRequestId={} | vendorId={} | "
                            + "basic={} | gross={} | tds={} | bankPayment={} | "
                            + "bankLedgerId={}",
                    paymentRequest.getId(),
                    vendor.getId(),
                    calculation.basicAmount(),
                    calculation.grossInvoiceAmount(),
                    calculation.tdsAmount(),
                    calculation.bankPaymentAmount(),
                    paymentApproval.getBankLedgerId()
            );

            AccountVendorSyncResponseDto response =
                    accountFeignClient.syncVendor(
                            syncRequest
                    );

            if (response == null) {

                throw new ValidationException(
                        "Account Service returned an empty response for vendor ID: "
                                + vendor.getId(),
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
                    "[ACCOUNT-VENDOR-SYNC-SUCCESS] "
                            + "paymentRequestId={} | vendorId={} | "
                            + "ledgerId={} | invoiceVoucherId={} | paymentVoucherId={}",
                    paymentRequest.getId(),
                    vendor.getId(),
                    response.getLedgerId(),
                    response.getVoucherId(),
                    response.getPaymentVoucherId()
            );

            return response;

        } catch (FeignException exception) {

            log.error(
                    "[ACCOUNT-VENDOR-SYNC-FAILED] "
                            + "paymentRequestId={} | vendorId={} | "
                            + "status={} | response={}",
                    paymentRequest.getId(),
                    vendor.getId(),
                    exception.status(),
                    exception.contentUTF8(),
                    exception
            );

            throw new ValidationException(
                    extractAccountServiceError(
                            exception
                    ),
                    "ERR_ACCOUNT_VENDOR_SYNC_FAILED"
            );
        }
    }


    // ================================================================
    // BUILD EXISTING ACCOUNT SERVICE REQUEST
    // ================================================================

    private VendorPaymentApprovalRequestDto buildPaymentReleaseRequest(
            ProcurementPaymentRequest paymentRequest,
            Long operationUserId,
            ProcurementPaymentActionRequestDto actionRequest,
            GstPayload gstPayload,
            PaymentCalculation calculation
    ) {

        LocalDate paymentDate =
                actionRequest.getPaymentDate() != null
                        ? actionRequest.getPaymentDate()
                        : LocalDate.now();

        String paymentMode =
                hasText(
                        actionRequest.getPaymentMode()
                )
                        ? actionRequest.getPaymentMode()
                        .trim()
                        : paymentRequest.getPaymentMode();

        String transactionReference =
                hasText(
                        actionRequest.getTransactionReference()
                )
                        ? actionRequest
                        .getTransactionReference()
                        .trim()
                        : paymentRequest.getTransactionReference();

        String paymentProof =
                hasText(
                        actionRequest.getPaymentProof()
                )
                        ? actionRequest
                        .getPaymentProof()
                        .trim()
                        : paymentRequest.getPaymentProof();

        String releaseComment =
                hasText(
                        actionRequest.getComment()
                )
                        ? actionRequest
                        .getComment()
                        .trim()
                        : clean(
                        paymentRequest
                                .getCompletionRemarks()
                );

        /*
         * IMPORTANT:
         *
         * This DTO is INTERNAL Operation -> Account.
         *
         * Therefore fields removed from ProcurementPaymentActionRequestDto
         * still exist here because Operation populates them.
         */
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

                /*
                 * Basic amount
                 */
                .price(
                        calculation.basicAmount()
                )

                .taxableAmount(
                        calculation.basicAmount()
                )

                /*
                 * GST config
                 */
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

                /*
                 * GST calculated snapshot
                 */
                .cgstAmount(
                        calculation.cgstAmount()
                )

                .sgstAmount(
                        calculation.sgstAmount()
                )

                .igstAmount(
                        calculation.igstAmount()
                )

                .totalGstAmount(
                        calculation.totalGstAmount()
                )

                /*
                 * Existing vendor liability / gross amount
                 */
                .invoiceGrossAmount(
                        calculation.grossInvoiceAmount()
                )

                /*
                 * TDS
                 */
                .tdsActive(
                        Boolean.TRUE.equals(
                                paymentRequest.getTdsActive()
                        )
                )

                .tdsBaseAmount(
                        calculation.basicAmount()
                )

                .tdsPercentage(
                        Boolean.TRUE.equals(
                                paymentRequest.getTdsActive()
                        )
                                ? money(paymentRequest.getTdsPercentage())
                                : zeroMoney()
                )

                .tdsAmount(
                        calculation.tdsAmount()
                )

                .tdsPayableLedgerId(null)

                /*
                 * Payable
                 */
                .vendorNetPayableAmount(
                        calculation.vendorNetPayableAmount()
                )

                /*
                 * Full settlement
                 */
                .settlementAmount(
                        money(
                                calculation.bankPaymentAmount()
                                        .add(calculation.tdsAmount())
                        )
                )

                /*
                 * Payment
                 */
                .paymentDate(
                        paymentDate
                )

                .bankPaymentAmount(
                        calculation.bankPaymentAmount()
                )

                .paymentMode(
                        paymentMode
                )

                .bankLedgerId(
                        actionRequest.getBankLedgerId()
                )

                /*
                 * Vendor ledger resolved in Account
                 */
                .ledgerId(null)
                .ledgerType(null)

                /*
                 * Transaction
                 */
                .transactionReference(
                        transactionReference
                )

                .paymentProof(
                        paymentProof
                )

                .proofAttachmentUrls(
                        actionRequest.getProofAttachmentUrls() != null
                                ? actionRequest.getProofAttachmentUrls()
                                : paymentRequest.getProofAttachmentUrls()
                )

                /*
                 * Audit
                 */
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

                .paymentReleasedDate(
                        paymentDate
                )

                .releaseComment(
                        releaseComment
                )

                .calculationVersion(
                        "PROCUREMENT_PAYMENT_V1"
                )

                .build();


    }


    // ================================================================
    // ACCOUNT RESPONSE VALIDATION
    // ================================================================

    private void validateAccountCalculation(
            PaymentCalculation operationCalculation,
            AccountVendorSyncResponseDto accountResponse
    ) {

        if (accountResponse == null) {

            throw new ValidationException(
                    "Account Service response is required",
                    "ERR_ACCOUNT_RESPONSE_REQUIRED"
            );
        }

        /*
         * Account response values are rounded to Operation's
         * 2-decimal procurement precision before comparison.
         */

        validateAmountMatch(
                "basic amount",
                operationCalculation.basicAmount(),
                accountResponse.getPrice()
        );

        validateAmountMatch(
                "CGST amount",
                operationCalculation.cgstAmount(),
                accountResponse.getCgstAmount()
        );

        validateAmountMatch(
                "SGST amount",
                operationCalculation.sgstAmount(),
                accountResponse.getSgstAmount()
        );

        validateAmountMatch(
                "IGST amount",
                operationCalculation.igstAmount(),
                accountResponse.getIgstAmount()
        );

        validateAmountMatch(
                "total GST amount",
                operationCalculation.totalGstAmount(),
                accountResponse.getTotalGstAmount()
        );

        validateAmountMatch(
                "gross invoice amount",
                operationCalculation.grossInvoiceAmount(),
                accountResponse.getGrossInvoiceAmount()
        );

        validateAmountMatch(
                "TDS amount",
                operationCalculation.tdsAmount(),
                accountResponse.getTdsAmount()
        );

        validateAmountMatch(
                "vendor net payable amount",
                operationCalculation.vendorNetPayableAmount(),
                accountResponse.getVendorNetPayableAmount()
        );
    }


    private void validateAmountMatch(
            String fieldName,
            BigDecimal expected,
            BigDecimal accountValue
    ) {

        /*
         * If old Account response does not expose a value,
         * do not break backwards compatibility.
         */
        if (accountValue == null) {
            return;
        }

        BigDecimal expectedMoney =
                money(expected);

        BigDecimal actualMoney =
                money(accountValue);

        if (expectedMoney.compareTo(
                actualMoney
        ) != 0) {

            throw new ValidationException(
                    "Account Service calculation mismatch for "
                            + fieldName
                            + ". Operation calculated "
                            + expectedMoney
                            + " but Account returned "
                            + actualMoney,
                    "ERR_ACCOUNT_PAYMENT_CALCULATION_MISMATCH"
            );
        }
    }


    // ================================================================
    // APPLY ACCOUNT CALCULATED VALUES
    // ================================================================

    private void applyAccountCalculatedAmounts(
            ProcurementPaymentRequest paymentRequest,
            AccountVendorSyncResponseDto accountResponse
    ) {

        if (accountResponse.getCgstAmount() != null) {

            paymentRequest.setCgstAmount(
                    money(
                            accountResponse.getCgstAmount()
                    )
            );
        }

        if (accountResponse.getSgstAmount() != null) {

            paymentRequest.setSgstAmount(
                    money(
                            accountResponse.getSgstAmount()
                    )
            );
        }

        if (accountResponse.getIgstAmount() != null) {

            paymentRequest.setIgstAmount(
                    money(
                            accountResponse.getIgstAmount()
                    )
            );
        }

        if (accountResponse.getTotalGstAmount() != null) {

            paymentRequest.setTotalGstAmount(
                    money(
                            accountResponse.getTotalGstAmount()
                    )
            );
        }

        if (accountResponse.getGrossInvoiceAmount() != null) {

            paymentRequest.setInvoiceAmount(
                    money(
                            accountResponse.getGrossInvoiceAmount()
                    )
            );
        }

        if (accountResponse.getTdsAmount() != null) {

            paymentRequest.setTdsAmount(
                    money(
                            accountResponse.getTdsAmount()
                    )
            );
        }

        if (accountResponse.getVendorNetPayableAmount() != null) {

            paymentRequest.setPayableAmount(
                    money(
                            accountResponse
                                    .getVendorNetPayableAmount()
                    )
            );
        }
    }


    // ================================================================
    // GST CONFIGURATION
    // ================================================================

    private GstPayload resolveGstPayload(
            ProcurementPaymentRequest paymentRequest,
            String gstRegistrationType
    ) {

        boolean gstActive =
                Boolean.TRUE.equals(
                        paymentRequest.getGstActive()
                );

        /*
         * Keep existing vendor behaviour:
         * INTERNATIONAL / SEZ are zero-rated.
         */
        boolean zeroRated =
                "INTERNATIONAL".equalsIgnoreCase(
                        gstRegistrationType
                )
                        ||
                        "SEZ".equalsIgnoreCase(
                                gstRegistrationType
                        );

        if (!gstActive || zeroRated) {

            return new GstPayload(
                    false,
                    gstRegistrationType,
                    null,
                    clean(
                            paymentRequest.getGstStateCode()
                    ),
                    zeroMoney()
            );
        }

        BigDecimal gstPercentage =
                money(
                        paymentRequest.getGstPercentage()
                );

        validatePercentage(
                gstPercentage,
                "GST percentage",
                "ERR_GST_PERCENTAGE_REQUIRED"
        );

        String supplyType =
                resolveGstSupplyType(
                        paymentRequest
                );

        return new GstPayload(
                true,
                gstRegistrationType,
                supplyType,
                clean(
                        paymentRequest.getGstStateCode()
                ),
                gstPercentage
        );
    }


    private String resolveGstSupplyType(
            ProcurementPaymentRequest paymentRequest
    ) {

        if (!hasText(
                paymentRequest.getGstType()
        )) {

            throw new ValidationException(
                    "GST type is required",
                    "ERR_GST_TYPE_REQUIRED"
            );
        }

        String gstType =
                paymentRequest
                        .getGstType()
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

        throw new ValidationException(
                "Invalid GST type: "
                        + paymentRequest.getGstType()
                        + ". Allowed values are INTRA_STATE/CGST_SGST "
                        + "or INTER_STATE/IGST",
                "ERR_INVALID_GST_TYPE"
        );
    }


    // ================================================================
    // GET ACTIVE PAYMENT REQUEST
    // ================================================================

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
                paymentRequestRepository
                        .findById(
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


    private ProcurementPaymentRequest getActivePaymentRequestForUpdate(
            Long paymentRequestId
    ) {

        if (paymentRequestId == null) {

            throw new ValidationException(
                    "Payment request id is required",
                    "ERR_PAYMENT_REQUEST_ID_REQUIRED"
            );
        }

        return paymentRequestRepository
                .findActiveByIdForUpdate(
                        paymentRequestId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Procurement payment request not found",
                                "ERR_PAYMENT_REQUEST_NOT_FOUND"
                        )
                );
    }


    // ================================================================
    // USER VALIDATION
    // ================================================================

    private void validateUser(
            Long userId
    ) {

        if (userId == null) {

            throw new ValidationException(
                    "User id is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        userRepository
                .findActiveUserById(
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found",
                                "ERR_USER_NOT_FOUND"
                        )
                );
    }


    // ================================================================
    // MONEY HELPERS
    // ================================================================

    private BigDecimal percentageOf(
            BigDecimal amount,
            BigDecimal percentage
    ) {

        return money(
                money(amount)
                        .multiply(
                                percentage
                        )
                        .divide(
                                BigDecimal.valueOf(100),
                                8,
                                MONEY_ROUNDING
                        )
        );
    }


    private void validatePercentage(
            BigDecimal percentage,
            String fieldName,
            String errorCode
    ) {

        if (percentage == null
                || percentage.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new ValidationException(
                    fieldName
                            + " must be greater than zero",
                    errorCode
            );
        }

        if (percentage.compareTo(
                BigDecimal.valueOf(100)
        ) > 0) {

            throw new ValidationException(
                    fieldName
                            + " cannot be greater than 100",
                    errorCode
            );
        }
    }


    private BigDecimal money(
            BigDecimal value
    ) {

        return value == null
                ? zeroMoney()
                : value.setScale(
                MONEY_SCALE,
                MONEY_ROUNDING
        );
    }


    private BigDecimal zeroMoney() {

        return BigDecimal.ZERO.setScale(
                MONEY_SCALE,
                MONEY_ROUNDING
        );
    }


    // ================================================================
    // STRING / DATE HELPERS
    // ================================================================

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


    private LocalDate toLocalDate(
            Date value
    ) {

        return value == null
                ? null
                : value.toInstant()
                .atZone(
                        java.time.ZoneId
                                .systemDefault()
                )
                .toLocalDate();
    }


    // ================================================================
    // ACCOUNT ERROR
    // ================================================================

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


    // ================================================================
    // RESPONSE MAPPING
    // ================================================================

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

                .id(
                        request.getId()
                )

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

                .tdsAmount(
                        request.getTdsAmount()
                )

                .gstActive(
                        request.getGstActive()
                )

                .gstStateCode(
                        request.getGstStateCode()
                )

                /*
                 * FIX:
                 * use payment request's actual stored GST percentage,
                 * not order.getGstRate().
                 */
                .gstPercentage(
                        request.getGstPercentage()
                )

                .gstType(
                        request.getGstType()
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

                /*
                 * Taxable/basic amount.
                 */
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

                .build();
    }


    // ================================================================
    // INTERNAL RECORDS
    // ================================================================

    private record PaymentCalculation(
            BigDecimal basicAmount,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal totalGstAmount,
            BigDecimal grossInvoiceAmount,
            BigDecimal tdsAmount,
            BigDecimal vendorNetPayableAmount,
            BigDecimal bankPaymentAmount
    ) {
    }


    private record GstPayload(
            Boolean gstActive,
            String gstRegistrationType,
            String gstSupplyType,
            String gstStateCode,
            BigDecimal gstPercentage
    ) {
    }


    private record VendorSyncContext(
            Vendor vendor,
            VendorAccountsSubmission accountsSubmission,
            VendorFinalization vendorFinalization,
            String gstRegistrationType,
            String gstNumber
    ) {
    }
}