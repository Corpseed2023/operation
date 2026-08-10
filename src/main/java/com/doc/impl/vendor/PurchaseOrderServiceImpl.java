package com.doc.impl.vendor;

import com.doc.dto.vendor.ProcurementOrderResponseDto;
import com.doc.dto.vendor.PurchaseOrderRequestDto;
import com.doc.dto.vendor.PurchaseOrderResponseDto;
import com.doc.entity.client.PaymentType;
import com.doc.entity.project.ProcurementStatus;
import com.doc.entity.user.User;
import com.doc.entity.vendor.*;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.PaymentTypeRepository;
import com.doc.repository.ProcurementMilestoneAssignmentRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.vendor.PurchaseOrderRepository;
import com.doc.repository.vendor.VendorRepository;
import com.doc.service.vendor.PurchaseOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Date;

@Service
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final Logger logger =
            LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

    private static final BigDecimal TWO =
            new BigDecimal("2");

    // =========================================================
    // REPOSITORIES
    // =========================================================

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private ProcurementMilestoneAssignmentRepository procurementRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentTypeRepository paymentTypeRepository;

    // =========================================================
    // CREATE PURCHASE ORDER
    // =========================================================

    @Override
    @Transactional
    public PurchaseOrderResponseDto createPurchaseOrder(
            PurchaseOrderRequestDto dto
    ) {

        logger.info(
                "Creating Purchase Order for procurementAssignmentId: {}",
                dto.getProcurementAssignmentId()
        );

        // =====================================================
        // REQUIRED VALIDATIONS
        // =====================================================

        if (dto.getProcurementAssignmentId() == null) {

            throw new ValidationException(
                    "Procurement Assignment ID is required",
                    "ERR_PROCUREMENT_ASSIGNMENT_REQUIRED"
            );
        }

        if (dto.getVendorId() == null) {

            throw new ValidationException(
                    "Vendor ID is required",
                    "ERR_VENDOR_ID_REQUIRED"
            );
        }

        if (dto.getCreatedBy() == null) {

            throw new ValidationException(
                    "CreatedBy user ID is required",
                    "ERR_CREATED_BY_REQUIRED"
            );
        }

        validateFinalAmount(
                dto.getFinalAmount()
        );

        // =====================================================
        // PROCUREMENT ASSIGNMENT
        // =====================================================

        ProcurementMilestoneAssignment procurement =
                procurementRepository
                        .findById(
                                dto.getProcurementAssignmentId()
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Procurement assignment not found",
                                        "ERR_PROCUREMENT_NOT_FOUND"
                                )
                        );

        // =====================================================
        // VENDOR
        // =====================================================

        Vendor vendor =
                vendorRepository
                        .findByIdAndIsDeletedFalse(
                                dto.getVendorId()
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Vendor not found",
                                        "ERR_VENDOR_NOT_FOUND"
                                )
                        );

        // =====================================================
        // CREATED BY
        // =====================================================

        User createdByUser =
                userRepository
                        .findActiveUserById(
                                dto.getCreatedBy()
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "CreatedBy user not found",
                                        "ERR_USER_NOT_FOUND"
                                )
                        );

        // =====================================================
        // PO BASIC / COMMERCIAL AMOUNT
        //
        // IMPORTANT:
        // PO does NOT calculate GST/TDS.
        //
        // GST/TDS configuration is maintained in
        // ProcurementPaymentRequest.
        //
        // Final CGST/SGST/IGST/TDS calculation happens during
        // payment release.
        // =====================================================

        BigDecimal finalAmount =
                money(
                        dto.getFinalAmount()
                );

        Date currentDate =
                new Date();

        // =====================================================
        // CREATE PO
        // =====================================================

        ProcurementOrder po =
                new ProcurementOrder();

        po.setProcurementAssignment(
                procurement
        );

        po.setProject(
                procurement.getProject()
        );

        po.setVendor(
                vendor
        );

        po.setPoNumber(
                generatePoNumber()
        );

        po.setPoReferenceNumber(
                dto.getPoReferenceNumber()
        );

        // =====================================================
        // SET BASIC PO AMOUNT ONLY
        // =====================================================

        setPurchaseOrderBaseAmount(
                po,
                vendor,
                finalAmount
        );

        // =====================================================
        // VALIDATE PO BASIC VALUE AGAINST PROJECT VALUE
        // =====================================================

        validatePoValueNotGreaterThanProjectValue(
                po.getFinalAmount(),
                procurement
        );

        // =====================================================
        // COMMERCIAL DETAILS
        // =====================================================

        po.setScopeOfWork(
                dto.getScopeOfWork()
        );

        po.setTermsAndConditions(
                dto.getTermsAndConditions()
        );

        po.setRemarks(
                dto.getRemarks()
        );

        if (dto.getAttachmentUrls() != null) {

            po.setAttachmentUrls(
                    dto.getAttachmentUrls()
            );
        }

        // =====================================================
        // STATUS
        // =====================================================

        po.setStatus(
                ProcurementOrderStatus.DRAFT
        );

        po.setPoCreatedDate(
                currentDate
        );

        // =====================================================
        // AUDIT
        // =====================================================

        po.setCreatedBy(
                createdByUser.getId()
        );

        po.setUpdatedBy(
                createdByUser.getId()
        );

        po.setCreatedDate(
                currentDate
        );

        po.setUpdatedDate(
                currentDate
        );

        // =====================================================
        // PAYMENT TYPE
        // =====================================================

        setPaymentType(
                po,
                dto.getPaymentTypeName()
        );

        // =====================================================
        // SAVE PO
        // =====================================================

        ProcurementOrder savedPo =
                purchaseOrderRepository.save(
                        po
                );

        // =====================================================
        // UPDATE PROCUREMENT ASSIGNMENT
        // =====================================================

        procurement.setStatus(
                ProcurementStatus.PO_CREATED
        );

        procurement.setSelectedVendor(
                vendor
        );

        procurement.setPoCreatedDate(
                currentDate
        );

        procurement.setUpdatedBy(
                createdByUser.getId()
        );

        procurement.setUpdatedDate(
                currentDate
        );

        procurementRepository.save(
                procurement
        );

        logger.info(
                "Purchase Order created | poNumber={} | "
                        + "basicAmount={} | "
                        + "GST/TDS calculation deferred to Procurement Payment Request",
                savedPo.getPoNumber(),
                savedPo.getFinalAmount()
        );

        return mapToResponseDto(
                savedPo
        );
    }

    // =========================================================
    // RELEASE
    //
    // Existing application treats PO release as PO approval.
    // =========================================================

    @Override
    public PurchaseOrderResponseDto releasePurchaseOrder(
            Long poId,
            Long userId
    ) {

        return approvePurchaseOrderInternal(
                poId,
                userId,
                null
        );
    }

    // =========================================================
    // GET PURCHASE ORDER BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDto getPurchaseOrderById(
            Long id
    ) {

        if (id == null) {

            throw new ValidationException(
                    "Purchase Order ID is required",
                    "ERR_PO_ID_REQUIRED"
            );
        }

        ProcurementOrder po =
                purchaseOrderRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Purchase Order not found",
                                        "ERR_PO_NOT_FOUND"
                                )
                        );

        if (po.isDeleted()) {

            throw new ValidationException(
                    "Deleted Purchase Order cannot be fetched",
                    "ERR_DELETED_PO"
            );
        }

        return mapToResponseDto(
                po
        );
    }

    // =========================================================
    // GET BY PROCUREMENT ASSIGNMENT
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDto getByProcurementAssignmentId(
            Long procurementAssignmentId
    ) {

        if (procurementAssignmentId == null) {

            throw new ValidationException(
                    "Procurement Assignment ID is required",
                    "ERR_NULL_ID"
            );
        }

        ProcurementOrder po =
                purchaseOrderRepository
                        .findByProcurementAssignmentId(
                                procurementAssignmentId
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "No Purchase Order found for this procurement assignment",
                                        "ERR_PO_NOT_FOUND"
                                )
                        );

        if (po.isDeleted()) {

            throw new ValidationException(
                    "Deleted Purchase Order cannot be fetched",
                    "ERR_DELETED_PO"
            );
        }

        return mapToResponseDto(
                po
        );
    }

    // =========================================================
    // GET PURCHASE ORDERS BY PROJECT ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponseDto> getPurchaseOrdersByProjectId(
            Long projectId,
            int page,
            int size
    ) {

        if (projectId == null) {

            throw new ValidationException(
                    "Project ID is required",
                    "ERR_PROJECT_ID_REQUIRED"
            );
        }

        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 10;
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

        Page<ProcurementOrder> purchaseOrders =
                purchaseOrderRepository
                        .findByProjectIdAndIsDeletedFalse(
                                projectId,
                                pageable
                        );

        return purchaseOrders.map(
                this::mapToResponseDto
        );
    }

    // =========================================================
    // GET PROCUREMENT ORDERS BY STATUS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementOrderResponseDto> getProcurementOrdersByStatus(
            ProcurementOrderStatus status,
            int page,
            int size
    ) {

        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 10;
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

        Page<ProcurementOrder> orders;

        if (status == null) {

            orders =
                    purchaseOrderRepository
                            .findByIsDeletedFalse(
                                    pageable
                            );

        } else {

            orders =
                    purchaseOrderRepository
                            .findByStatusAndIsDeletedFalse(
                                    status,
                                    pageable
                            );
        }

        return orders.map(
                this::mapToResponse
        );
    }

    // =========================================================
    // APPROVE PROCUREMENT ORDER
    // =========================================================

    @Override
    @Transactional
    public ProcurementOrderResponseDto approveProcurementOrder(
            Long procurementOrderId,
            Long userId,
            String comment
    ) {

        PurchaseOrderResponseDto approved =
                approvePurchaseOrderInternal(
                        procurementOrderId,
                        userId,
                        comment
                );

        ProcurementOrder order =
                purchaseOrderRepository
                        .findById(
                                approved.getId()
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Purchase Order not found",
                                        "ERR_PO_NOT_FOUND"
                                )
                        );

        return mapToResponse(
                order
        );
    }

    // =========================================================
    // REJECT PROCUREMENT ORDER
    // =========================================================

    @Override
    @Transactional
    public ProcurementOrderResponseDto rejectProcurementOrder(
            Long procurementOrderId,
            Long userId,
            String reason
    ) {

        throw new ValidationException(
                "PO rejection flow is removed. "
                        + "Purchase Order supports only "
                        + "DRAFT and APPROVED status.",
                "ERR_PO_REJECTION_FLOW_REMOVED"
        );
    }

    // =========================================================
    // UPDATE PURCHASE ORDER
    // =========================================================

    @Override
    @Transactional
    public PurchaseOrderResponseDto updatePurchaseOrder(
            Long poId,
            PurchaseOrderRequestDto dto
    ) {

        logger.info(
                "Updating Purchase Order id: {}",
                poId
        );

        if (poId == null) {

            throw new ValidationException(
                    "Purchase Order ID is required",
                    "ERR_PO_ID_REQUIRED"
            );
        }

        ProcurementOrder po =
                purchaseOrderRepository
                        .findById(poId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Purchase Order not found",
                                        "ERR_PO_NOT_FOUND"
                                )
                        );

        if (po.isDeleted()) {

            throw new ValidationException(
                    "Deleted Purchase Order cannot be updated",
                    "ERR_DELETED_PO_CANNOT_BE_UPDATED"
            );
        }

        if (po.getStatus()
                != ProcurementOrderStatus.DRAFT) {

            throw new ValidationException(
                    "Only DRAFT Purchase Order can be updated. "
                            + "Current status: "
                            + po.getStatus(),
                    "ERR_INVALID_PO_STATUS_FOR_UPDATE"
            );
        }

        validateFinalAmount(
                dto.getFinalAmount()
        );

        // =====================================================
        // PROCUREMENT
        // =====================================================

        ProcurementMilestoneAssignment procurement =
                po.getProcurementAssignment();

        if (dto.getProcurementAssignmentId() != null) {

            procurement =
                    procurementRepository
                            .findById(
                                    dto.getProcurementAssignmentId()
                            )
                            .orElseThrow(
                                    () -> new ResourceNotFoundException(
                                            "Procurement assignment not found",
                                            "ERR_PROCUREMENT_NOT_FOUND"
                                    )
                            );

            po.setProcurementAssignment(
                    procurement
            );

            po.setProject(
                    procurement.getProject()
            );
        }

        if (procurement == null) {

            throw new ValidationException(
                    "Procurement assignment is required",
                    "ERR_PROCUREMENT_ASSIGNMENT_REQUIRED"
            );
        }

        // =====================================================
        // VENDOR
        // =====================================================

        Vendor vendor =
                po.getVendor();

        if (dto.getVendorId() != null) {

            vendor =
                    vendorRepository
                            .findByIdAndIsDeletedFalse(
                                    dto.getVendorId()
                            )
                            .orElseThrow(
                                    () -> new ResourceNotFoundException(
                                            "Vendor not found",
                                            "ERR_VENDOR_NOT_FOUND"
                                    )
                            );

            po.setVendor(
                    vendor
            );
        }

        if (vendor == null) {

            throw new ValidationException(
                    "Vendor is required",
                    "ERR_VENDOR_REQUIRED"
            );
        }

        po.setPoReferenceNumber(
                dto.getPoReferenceNumber()
        );

        // =====================================================
        // UPDATE PO BASIC AMOUNT ONLY
        //
        // GST/TDS are NOT calculated here.
        // =====================================================

        BigDecimal finalAmount =
                money(
                        dto.getFinalAmount()
                );

        setPurchaseOrderBaseAmount(
                po,
                vendor,
                finalAmount
        );

        // =====================================================
        // VALIDATE BASIC PO AMOUNT
        // =====================================================

        validatePoValueNotGreaterThanProjectValue(
                po.getFinalAmount(),
                procurement
        );

        // =====================================================
        // COMMERCIAL
        // =====================================================

        po.setScopeOfWork(
                dto.getScopeOfWork()
        );

        po.setTermsAndConditions(
                dto.getTermsAndConditions()
        );

        po.setRemarks(
                dto.getRemarks()
        );

        if (dto.getAttachmentUrls() != null) {

            po.setAttachmentUrls(
                    dto.getAttachmentUrls()
            );
        }

        // =====================================================
        // UPDATED BY
        // =====================================================

        Long updatedBy =
                dto.getUserId() != null
                        ? dto.getUserId()
                        : dto.getCreatedBy();

        if (updatedBy != null) {

            User updatedByUser =
                    userRepository
                            .findActiveUserById(
                                    updatedBy
                            )
                            .orElseThrow(
                                    () -> new ResourceNotFoundException(
                                            "UpdatedBy user not found",
                                            "ERR_USER_NOT_FOUND"
                                    )
                            );

            po.setUpdatedBy(
                    updatedByUser.getId()
            );
        }

        // =====================================================
        // PAYMENT TYPE
        // =====================================================

        if (dto.getPaymentTypeName() != null) {

            if (dto.getPaymentTypeName()
                    .trim()
                    .isEmpty()) {

                po.setPaymentType(
                        null
                );

            } else {

                setPaymentType(
                        po,
                        dto.getPaymentTypeName()
                );
            }
        }

        po.setUpdatedDate(
                new Date()
        );

        ProcurementOrder savedPo =
                purchaseOrderRepository.save(
                        po
                );

        logger.info(
                "Purchase Order updated | poNumber={} | "
                        + "basicAmount={} | "
                        + "GST/TDS calculation deferred to Procurement Payment Request",
                savedPo.getPoNumber(),
                savedPo.getFinalAmount()
        );

        return mapToResponseDto(
                savedPo
        );
    }

    // =========================================================
    // UPDATE PURCHASE ORDER STATUS
    // =========================================================

    @Override
    @Transactional
    public PurchaseOrderResponseDto updatePurchaseOrderStatus(
            Long poId,
            ProcurementOrderStatus newStatus,
            Long userId,
            String remarks
    ) {

        logger.info(
                "Updating Purchase Order status | "
                        + "poId={}, newStatus={}, userId={}",
                poId,
                newStatus,
                userId
        );

        if (poId == null) {

            throw new ValidationException(
                    "Purchase Order ID is required",
                    "ERR_PO_ID_REQUIRED"
            );
        }

        if (newStatus == null) {

            throw new ValidationException(
                    "Purchase Order status is required",
                    "ERR_PO_STATUS_REQUIRED"
            );
        }

        if (newStatus
                != ProcurementOrderStatus.APPROVED) {

            throw new ValidationException(
                    "Only APPROVED status update is allowed. "
                            + "Purchase Order supports only "
                            + "DRAFT and APPROVED.",
                    "ERR_INVALID_PO_STATUS"
            );
        }

        return approvePurchaseOrderInternal(
                poId,
                userId,
                remarks
        );
    }

    // =========================================================
    // INTERNAL APPROVE
    // =========================================================

    private PurchaseOrderResponseDto approvePurchaseOrderInternal(
            Long poId,
            Long userId,
            String remarks
    ) {

        if (poId == null) {

            throw new ValidationException(
                    "Purchase Order ID is required",
                    "ERR_PO_ID_REQUIRED"
            );
        }

        if (userId == null) {

            throw new ValidationException(
                    "User ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        ProcurementOrder po =
                purchaseOrderRepository
                        .findById(poId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Purchase Order not found",
                                        "ERR_PO_NOT_FOUND"
                                )
                        );

        if (po.isDeleted()) {

            throw new ValidationException(
                    "Deleted Purchase Order cannot be approved",
                    "ERR_DELETED_PO"
            );
        }

        if (po.getStatus()
                != ProcurementOrderStatus.DRAFT) {

            throw new ValidationException(
                    "Only DRAFT Purchase Order can be approved. "
                            + "Current status: "
                            + po.getStatus(),
                    "ERR_INVALID_PO_STATUS"
            );
        }

        User approvedByUser =
                userRepository
                        .findActiveUserById(
                                userId
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "User not found",
                                        "ERR_USER_NOT_FOUND"
                                )
                        );

        Date now =
                new Date();

        po.setStatus(
                ProcurementOrderStatus.APPROVED
        );

        po.setApprovedBy(
                approvedByUser.getId()
        );

        po.setPoApprovedDate(
                now
        );

        po.setUpdatedBy(
                approvedByUser.getId()
        );

        po.setUpdatedDate(
                now
        );

        if (remarks != null
                && !remarks.trim().isEmpty()) {

            po.setRemarks(
                    remarks.trim()
            );
        }

        ProcurementOrder savedPo =
                purchaseOrderRepository.save(
                        po
                );

        ProcurementMilestoneAssignment procurement =
                savedPo.getProcurementAssignment();

        if (procurement != null) {

            procurement.setStatus(
                    ProcurementStatus.PO_APPROVED
            );

            procurement.setUpdatedBy(
                    approvedByUser.getId()
            );

            procurement.setUpdatedDate(
                    now
            );

            procurementRepository.save(
                    procurement
            );
        }

        logger.info(
                "Purchase Order approved | "
                        + "poNumber={} | approvedBy={}",
                savedPo.getPoNumber(),
                approvedByUser.getId()
        );

        return mapToResponseDto(
                savedPo
        );
    }

    // =========================================================
    // SET PURCHASE ORDER BASIC AMOUNT
    //
    // IMPORTANT:
    //
    // Purchase Order does NOT own GST/TDS calculation.
    //
    // GST/TDS configuration belongs to ProcurementPaymentRequest.
    // Actual GST/TDS amounts are calculated during payment release.
    //
    // Existing PO tax fields are retained at ZERO only for
    // database/API backwards compatibility.
    // =========================================================

    private void setPurchaseOrderBaseAmount(
            ProcurementOrder po,
            Vendor vendor,
            BigDecimal finalAmount
    ) {

        BigDecimal zero =
                BigDecimal.ZERO.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal basicAmount =
                money(
                        finalAmount
                );

        // =====================================================
        // BASIC PO AMOUNT
        // =====================================================

        po.setFinalAmount(
                basicAmount
        );

        // =====================================================
        // GST IS NOT CALCULATED AT PO STAGE
        // =====================================================

        po.setGstRate(
                zero
        );

        po.setCgstAmount(
                zero
        );

        po.setSgstAmount(
                zero
        );

        po.setIgstAmount(
                zero
        );

        po.setTotalTaxAmount(
                zero
        );

        // =====================================================
        // TDS IS NOT CALCULATED AT PO STAGE
        // =====================================================

        po.setTdsPercentage(
                zero
        );

        po.setTdsAmount(
                zero
        );

        // =====================================================
        // GRAND TOTAL
        //
        // At PO stage:
        //
        // grandTotal == basic negotiated PO amount
        //
        // Gross invoice amount will be calculated later
        // during Procurement Payment Request release.
        // =====================================================

        po.setGrandTotal(
                basicAmount
        );

        // =====================================================
        // PLACE OF SUPPLY
        //
        // Actual GST state/supply type is selected on PR.
        // =====================================================

        po.setPlaceOfSupplyStateCode(
                null
        );

        // =====================================================
        // VENDOR GST SNAPSHOT ONLY
        //
        // This does NOT activate GST calculation.
        // =====================================================

        po.setVendorGSTRegistrationType(
                vendor != null
                        ? vendor.getGstRegistrationType()
                        : null
        );
    }

    // =========================================================
    // VALIDATE PO BASIC VALUE AGAINST PROJECT
    //
    // IMPORTANT:
    // This compares PO basic/final amount only.
    // GST/TDS must not affect this validation.
    // =========================================================

    private void validatePoValueNotGreaterThanProjectValue(
            BigDecimal finalAmount,
            ProcurementMilestoneAssignment procurement
    ) {

        if (procurement == null) {

            throw new ValidationException(
                    "Procurement assignment is required",
                    "ERR_PROCUREMENT_ASSIGNMENT_REQUIRED"
            );
        }

        if (procurement.getProject() == null) {

            throw new ValidationException(
                    "Project not found for procurement assignment",
                    "ERR_PROJECT_NOT_FOUND"
            );
        }

        if (procurement
                .getProject()
                .getPaymentDetail() == null) {

            throw new ValidationException(
                    "Project payment detail not found",
                    "ERR_PROJECT_PAYMENT_DETAIL_NOT_FOUND"
            );
        }

        BigDecimal poValue =
                money(
                        finalAmount
                );

        if (poValue.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new ValidationException(
                    "PO value must be greater than zero",
                    "ERR_INVALID_PO_VALUE"
            );
        }

        BigDecimal projectValue =
                BigDecimal.valueOf(
                        procurement
                                .getProject()
                                .getPaymentDetail()
                                .getTotalAmount()
                );

        if (poValue.compareTo(
                projectValue
        ) > 0) {

            throw new ValidationException(
                    "PO value cannot be greater than project value. "
                            + "Project value: "
                            + projectValue
                            + ", PO value: "
                            + poValue,
                    "ERR_PO_VALUE_EXCEEDS_PROJECT_VALUE"
            );
        }
    }

    // =========================================================
    // PROCUREMENT ORDER RESPONSE
    // =========================================================

    private ProcurementOrderResponseDto mapToResponse(
            ProcurementOrder order
    ) {

        ProcurementOrderResponseDto dto =
                new ProcurementOrderResponseDto();

        dto.setId(
                order.getId()
        );

        dto.setProcurementAssignmentId(
                order.getProcurementAssignment() != null
                        ? order
                        .getProcurementAssignment()
                        .getId()
                        : null
        );

        dto.setProjectId(
                order.getProject() != null
                        ? order
                        .getProject()
                        .getId()
                        : null
        );

        dto.setProjectName(
                order.getProject() != null
                        ? order
                        .getProject()
                        .getName()
                        : null
        );

        dto.setVendorId(
                order.getVendor() != null
                        ? order
                        .getVendor()
                        .getId()
                        : null
        );

        dto.setVendorName(
                order.getVendor() != null
                        ? order
                        .getVendor()
                        .getName()
                        : null
        );

        dto.setVendorContactId(
                order.getVendorContact() != null
                        ? order
                        .getVendorContact()
                        .getId()
                        : null
        );

        dto.setVendorContactName(
                order.getVendorContact() != null
                        ? order
                        .getVendorContact()
                        .getName()
                        : null
        );

        dto.setPoNumber(
                order.getPoNumber()
        );

        dto.setPoReferenceNumber(
                order.getPoReferenceNumber()
        );

        dto.setFinalAmount(
                moneyOrZero(
                        order.getFinalAmount()
                )
        );

        /*
         * Legacy PO GST/TDS fields.
         *
         * New Purchase Orders store these as ZERO because
         * ProcurementPaymentRequest owns taxation.
         */
        dto.setGstRate(
                percentageOrZero(
                        order.getGstRate()
                )
        );

        dto.setCgstAmount(
                moneyOrZero(
                        order.getCgstAmount()
                )
        );

        dto.setSgstAmount(
                moneyOrZero(
                        order.getSgstAmount()
                )
        );

        dto.setIgstAmount(
                moneyOrZero(
                        order.getIgstAmount()
                )
        );

        dto.setTotalTaxAmount(
                moneyOrZero(
                        order.getTotalTaxAmount()
                )
        );

        dto.setGrandTotal(
                moneyOrZero(
                        order.getGrandTotal()
                )
        );

        dto.setScopeOfWork(
                order.getScopeOfWork()
        );

        dto.setTermsAndConditions(
                order.getTermsAndConditions()
        );

        dto.setRemarks(
                order.getRemarks()
        );

        dto.setAttachmentUrls(
                order.getAttachmentUrls()
        );

        dto.setStatus(
                order.getStatus()
        );

        dto.setPoCreatedDate(
                order.getPoCreatedDate()
        );

        dto.setPoSubmittedForApprovalDate(
                order.getPoSubmittedForApprovalDate()
        );

        dto.setPoApprovedDate(
                order.getPoApprovedDate()
        );

        dto.setPoReleasedDate(
                order.getPoReleasedDate()
        );

        dto.setPaymentTypeId(
                order.getPaymentType() != null
                        ? order
                        .getPaymentType()
                        .getId()
                        : null
        );

        dto.setPaymentTypeName(
                order.getPaymentType() != null
                        ? order
                        .getPaymentType()
                        .getName()
                        : null
        );

        dto.setCreatedBy(
                order.getCreatedBy()
        );

        dto.setUpdatedBy(
                order.getUpdatedBy()
        );

        dto.setApprovedBy(
                order.getApprovedBy()
        );

        dto.setCreatedDate(
                order.getCreatedDate()
        );

        dto.setUpdatedDate(
                order.getUpdatedDate()
        );

        return dto;
    }

    // =========================================================
    // PURCHASE ORDER RESPONSE
    // =========================================================

    private PurchaseOrderResponseDto mapToResponseDto(
            ProcurementOrder po
    ) {

        PurchaseOrderResponseDto dto =
                new PurchaseOrderResponseDto();

        BigDecimal zero =
                BigDecimal.ZERO.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        // =====================================================
        // PO
        // =====================================================

        dto.setId(
                po.getId()
        );

        dto.setPoNumber(
                po.getPoNumber()
        );

        dto.setPoReferenceNumber(
                po.getPoReferenceNumber()
        );

        // =====================================================
        // PROCUREMENT
        // =====================================================

        if (po.getProcurementAssignment() != null) {

            dto.setProcurementAssignmentId(
                    po
                            .getProcurementAssignment()
                            .getId()
            );
        }

        // =====================================================
        // PROJECT
        // =====================================================

        if (po.getProject() != null) {

            dto.setProjectId(
                    po.getProject()
                            .getId()
            );

            dto.setProjectName(
                    po.getProject()
                            .getName()
            );

            dto.setProjectNo(
                    po.getProject()
                            .getProjectNo()
            );
        }

        // =====================================================
        // VENDOR
        // =====================================================

        if (po.getVendor() != null) {

            Vendor vendor =
                    po.getVendor();

            dto.setVendorId(
                    vendor.getId()
            );

            dto.setVendorName(
                    vendor.getName()
            );

            dto.setVendorEmail(
                    vendor.getEmail()
            );

            dto.setVendorMobile(
                    vendor.getMobile()
            );

            dto.setVendorAddress(
                    vendor.getFullAddress()
            );

            dto.setVendorCity(
                    vendor.getCity()
            );

            dto.setVendorState(
                    vendor.getState()
            );

            dto.setVendorCountry(
                    vendor.getCountry()
            );

            dto.setVendorGSTNumber(
                    vendor.getGstNumber()
            );

            VendorGSTRegistrationType registrationType =
                    po.getVendorGSTRegistrationType() != null
                            ? po.getVendorGSTRegistrationType()
                            : vendor.getGstRegistrationType();

            dto.setVendorGSTRegistrationType(
                    registrationType
            );

            dto.setVendorPANNumber(
                    vendor.getPanNumber()
            );

            /*
             * Vendor GSTIN state code is informational only.
             *
             * It does not calculate GST on the PO.
             */
            if (vendor.getGstNumber() != null
                    && vendor
                    .getGstNumber()
                    .trim()
                    .length() >= 2) {

                dto.setVendorStateCode(
                        vendor
                                .getGstNumber()
                                .trim()
                                .substring(
                                        0,
                                        2
                                )
                );
            }
        }

        /*
         * PO doesn't own GST place-of-supply configuration.
         */
        dto.setPlaceOfSupplyStateCode(
                po.getPlaceOfSupplyStateCode()
        );

        // =====================================================
        // BASIC AMOUNT
        // =====================================================

        dto.setFinalAmount(
                moneyOrZero(
                        po.getFinalAmount()
                )
        );

        // =====================================================
        // LEGACY PO GST FIELDS
        //
        // These are ZERO for new POs.
        //
        // Actual GST exists on ProcurementPaymentRequest.
        // =====================================================

        BigDecimal gstRate =
                percentageOrZero(
                        po.getGstRate()
                );

        dto.setGstRate(
                gstRate
        );

        BigDecimal cgstAmount =
                moneyOrZero(
                        po.getCgstAmount()
                );

        BigDecimal sgstAmount =
                moneyOrZero(
                        po.getSgstAmount()
                );

        BigDecimal igstAmount =
                moneyOrZero(
                        po.getIgstAmount()
                );

        dto.setCgstAmount(
                cgstAmount
        );

        dto.setSgstAmount(
                sgstAmount
        );

        dto.setIgstAmount(
                igstAmount
        );

        dto.setTotalTaxAmount(
                moneyOrZero(
                        po.getTotalTaxAmount()
                )
        );

        // =====================================================
        // GST RATE BREAKUP
        //
        // Supports old POs which may still contain GST.
        //
        // New POs will return:
        //
        // cgstRate = 0
        // sgstRate = 0
        // igstRate = 0
        // =====================================================

        boolean hasIgst =
                igstAmount.compareTo(
                        BigDecimal.ZERO
                ) > 0;

        boolean hasCgstOrSgst =
                cgstAmount.compareTo(
                        BigDecimal.ZERO
                ) > 0
                        ||
                        sgstAmount.compareTo(
                                BigDecimal.ZERO
                        ) > 0;

        if (hasIgst) {

            dto.setCgstRate(
                    zero
            );

            dto.setSgstRate(
                    zero
            );

            dto.setIgstRate(
                    gstRate
            );

        } else if (hasCgstOrSgst) {

            BigDecimal halfRate =
                    gstRate.divide(
                            TWO,
                            2,
                            RoundingMode.HALF_UP
                    );

            dto.setCgstRate(
                    halfRate
            );

            dto.setSgstRate(
                    halfRate
            );

            dto.setIgstRate(
                    zero
            );

        } else {

            dto.setCgstRate(
                    zero
            );

            dto.setSgstRate(
                    zero
            );

            dto.setIgstRate(
                    zero
            );
        }

        // =====================================================
        // TDS
        //
        // New POs contain ZERO.
        // Actual TDS belongs to ProcurementPaymentRequest.
        // =====================================================

        dto.setTdsPercentage(
                percentageOrZero(
                        po.getTdsPercentage()
                )
        );

        dto.setTdsAmount(
                moneyOrZero(
                        po.getTdsAmount()
                )
        );

        // =====================================================
        // GRAND TOTAL
        //
        // At PO level:
        // grandTotal == finalAmount/basicAmount
        // =====================================================

        dto.setGrandTotal(
                moneyOrZero(
                        po.getGrandTotal()
                )
        );

        // =====================================================
        // COMMERCIAL
        // =====================================================

        dto.setScopeOfWork(
                po.getScopeOfWork()
        );

        dto.setTermsAndConditions(
                po.getTermsAndConditions()
        );

        dto.setRemarks(
                po.getRemarks()
        );

        dto.setAttachmentUrls(
                po.getAttachmentUrls()
        );

        // =====================================================
        // STATUS
        // =====================================================

        dto.setStatus(
                po.getStatus()
        );

        // =====================================================
        // PAYMENT TYPE
        // =====================================================

        if (po.getPaymentType() != null) {

            dto.setPaymentTypeName(
                    po
                            .getPaymentType()
                            .getName()
            );
        }

        // =====================================================
        // DATES
        // =====================================================

        dto.setPoCreatedDate(
                po.getPoCreatedDate()
        );

        dto.setPoSubmittedForApprovalDate(
                po.getPoSubmittedForApprovalDate()
        );

        dto.setPoApprovedDate(
                po.getPoApprovedDate()
        );

        dto.setPoReleasedDate(
                po.getPoReleasedDate()
        );

        // =====================================================
        // AUDIT
        // =====================================================

        dto.setCreatedBy(
                po.getCreatedBy()
        );

        dto.setUpdatedBy(
                po.getUpdatedBy()
        );

        dto.setApprovedBy(
                po.getApprovedBy()
        );

        dto.setCreatedDate(
                po.getCreatedDate()
        );

        dto.setUpdatedDate(
                po.getUpdatedDate()
        );

        return dto;
    }

    // =========================================================
    // PAYMENT TYPE
    // =========================================================

    private void setPaymentType(
            ProcurementOrder po,
            String paymentTypeName
    ) {

        if (paymentTypeName == null
                || paymentTypeName
                .trim()
                .isEmpty()) {

            return;
        }

        String name =
                paymentTypeName.trim();

        PaymentType paymentType =
                paymentTypeRepository
                        .findByName(
                                name
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Payment type not found: "
                                                + name,
                                        "ERR_PAYMENT_TYPE_NOT_FOUND"
                                )
                        );

        po.setPaymentType(
                paymentType
        );
    }

    // =========================================================
    // FINAL AMOUNT VALIDATION
    // =========================================================

    private void validateFinalAmount(
            BigDecimal amount
    ) {

        if (amount == null
                || amount.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new ValidationException(
                    "Final amount must be greater than zero",
                    "ERR_INVALID_AMOUNT"
            );
        }
    }

    // =========================================================
    // MONEY
    // =========================================================

    private BigDecimal money(
            BigDecimal value
    ) {

        if (value == null) {

            throw new ValidationException(
                    "Amount cannot be null",
                    "ERR_NULL_AMOUNT"
            );
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    // =========================================================
    // MONEY OR ZERO
    // =========================================================

    private BigDecimal moneyOrZero(
            BigDecimal value
    ) {

        if (value == null) {

            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    // =========================================================
    // PERCENTAGE OR ZERO
    //
    // Kept only for backwards-compatible response mapping.
    // =========================================================

    private BigDecimal percentageOrZero(
            BigDecimal value
    ) {

        if (value == null) {

            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    // =========================================================
    // GENERATE PO NUMBER
    // =========================================================

    private String generatePoNumber() {

        int year =
                LocalDate.now()
                        .getYear();

        long count =
                purchaseOrderRepository
                        .count() + 1;

        return String.format(
                "CORP-PO-%d-%05d",
                year,
                count
        );
    }
}