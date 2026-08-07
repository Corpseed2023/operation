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
public class PurchaseOrderServiceImpl
        implements PurchaseOrderService {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    PurchaseOrderServiceImpl.class
            );

    private static final BigDecimal HUNDRED =
            new BigDecimal("100");

    private static final BigDecimal TWO =
            new BigDecimal("2");

    // =========================================================
    // REPOSITORIES
    // =========================================================

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private ProcurementMilestoneAssignmentRepository
            procurementRepository;

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
        // PROCUREMENT
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
        // AMOUNTS
        // =====================================================

        BigDecimal finalAmount =
                money(
                        dto.getFinalAmount()
                );

        BigDecimal gstRate =
                percentageOrZero(
                        dto.getGstRate()
                );

        BigDecimal tdsPercentage =
                percentageOrZero(
                        dto.getTdsPercentage()
                );

        validatePercentage(
                gstRate,
                "GST rate",
                "ERR_INVALID_GST_RATE"
        );

        validatePercentage(
                tdsPercentage,
                "TDS percentage",
                "ERR_INVALID_TDS_PERCENTAGE"
        );

        Date currentDate =
                new Date();

        // =====================================================
        // PO
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
        // CALCULATE TAXES
        // =====================================================

        calculateAndSetTaxes(
                po,
                vendor,
                finalAmount,
                gstRate,
                tdsPercentage,
                dto.getPlaceOfSupplyStateCode()
        );

        // =====================================================
        // VALIDATE PO VALUE AFTER TAX CALCULATION
        // =====================================================

        validatePoValueNotGreaterThanProjectValue(
                po.getGrandTotal(),
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
        // UPDATE PROCUREMENT
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
                "Purchase Order created | poNumber={} | base={} | GST={} | TDS={} | grandTotal={}",
                savedPo.getPoNumber(),
                savedPo.getFinalAmount(),
                savedPo.getTotalTaxAmount(),
                savedPo.getTdsAmount(),
                savedPo.getGrandTotal()
        );

        return mapToResponseDto(
                savedPo
        );
    }

    // =========================================================
    // RELEASE
    //
    // Existing application currently treats release as approval.
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
    // GET BY PO ID
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
    // GET BY PROCUREMENT ASSIGNMENT ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDto
    getByProcurementAssignmentId(
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
    public Page<PurchaseOrderResponseDto>
    getPurchaseOrdersByProjectId(
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
    public Page<ProcurementOrderResponseDto>
    getProcurementOrdersByStatus(
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
    public ProcurementOrderResponseDto
    approveProcurementOrder(
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
    // REJECT
    // =========================================================

    @Override
    @Transactional
    public ProcurementOrderResponseDto
    rejectProcurementOrder(
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

        if (dto.getProcurementAssignmentId()
                != null) {

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

        // =====================================================
        // VENDOR
        // =====================================================

        Vendor vendor =
                po.getVendor();

        if (dto.getVendorId()
                != null) {

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
        // RECALCULATE TAXES
        // =====================================================

        BigDecimal finalAmount =
                money(
                        dto.getFinalAmount()
                );

        BigDecimal gstRate =
                dto.getGstRate() != null
                        ? percentageOrZero(
                        dto.getGstRate()
                )
                        : percentageOrZero(
                        po.getGstRate()
                );

        BigDecimal tdsPercentage =
                dto.getTdsPercentage() != null
                        ? percentageOrZero(
                        dto.getTdsPercentage()
                )
                        : percentageOrZero(
                        po.getTdsPercentage()
                );

        String placeOfSupplyStateCode =
                dto.getPlaceOfSupplyStateCode()
                        != null
                        ? dto.getPlaceOfSupplyStateCode()
                        : po.getPlaceOfSupplyStateCode();

        validatePercentage(
                gstRate,
                "GST rate",
                "ERR_INVALID_GST_RATE"
        );

        validatePercentage(
                tdsPercentage,
                "TDS percentage",
                "ERR_INVALID_TDS_PERCENTAGE"
        );

        calculateAndSetTaxes(
                po,
                vendor,
                finalAmount,
                gstRate,
                tdsPercentage,
                placeOfSupplyStateCode
        );

        // =====================================================
        // VALIDATE AFTER SERVER CALCULATION
        // =====================================================

        validatePoValueNotGreaterThanProjectValue(
                po.getGrandTotal(),
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

        if (dto.getAttachmentUrls()
                != null) {

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

        if (dto.getPaymentTypeName()
                != null) {

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
                "Purchase Order updated | po={} | base={} | GST={} | TDS={} | grandTotal={}",
                savedPo.getPoNumber(),
                savedPo.getFinalAmount(),
                savedPo.getTotalTaxAmount(),
                savedPo.getTdsAmount(),
                savedPo.getGrandTotal()
        );

        return mapToResponseDto(
                savedPo
        );
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    @Override
    @Transactional
    public PurchaseOrderResponseDto
    updatePurchaseOrderStatus(
            Long poId,
            ProcurementOrderStatus newStatus,
            Long userId,
            String remarks
    ) {

        logger.info(
                "Updating Purchase Order status | poId={}, newStatus={}, userId={}",
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

    private PurchaseOrderResponseDto
    approvePurchaseOrderInternal(
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
                "Purchase Order approved | poNumber={} | approvedBy={}",
                savedPo.getPoNumber(),
                approvedByUser.getId()
        );

        return mapToResponseDto(
                savedPo
        );
    }

    // =========================================================
    // GST + TDS CALCULATION
    // =========================================================

    private void calculateAndSetTaxes(
            ProcurementOrder po,
            Vendor vendor,
            BigDecimal finalAmount,
            BigDecimal gstRate,
            BigDecimal tdsPercentage,
            String placeOfSupplyStateCode
    ) {

        BigDecimal zero =
                BigDecimal.ZERO.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        finalAmount =
                money(
                        finalAmount
                );

        gstRate =
                percentageOrZero(
                        gstRate
                );

        tdsPercentage =
                percentageOrZero(
                        tdsPercentage
                );

        validatePercentage(
                gstRate,
                "GST rate",
                "ERR_INVALID_GST_RATE"
        );

        validatePercentage(
                tdsPercentage,
                "TDS percentage",
                "ERR_INVALID_TDS_PERCENTAGE"
        );

        // =====================================================
        // TDS
        //
        // TDS = Base Amount * TDS %
        // =====================================================

        BigDecimal tdsAmount =
                finalAmount
                        .multiply(
                                tdsPercentage
                        )
                        .divide(
                                HUNDRED,
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal cgstAmount =
                zero;

        BigDecimal sgstAmount =
                zero;

        BigDecimal igstAmount =
                zero;

        // =====================================================
        // GST APPLICABILITY
        // =====================================================

        boolean registeredVendor =
                VendorGSTRegistrationType.REGISTERED
                        .equals(
                                vendor.getGstRegistrationType()
                        );

        /*
         * Normal GST treatment:
         * if vendor is not REGISTERED,
         * GST is not added.
         */
        if (!registeredVendor) {

            gstRate =
                    zero;
        }

        // =====================================================
        // GST CALCULATION
        // =====================================================

        if (registeredVendor
                && gstRate.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            String gstNumber =
                    vendor.getGstNumber();

            if (gstNumber == null
                    || gstNumber
                    .trim()
                    .length() != 15) {

                throw new ValidationException(
                        "A valid 15 character Vendor GSTIN is required for GST calculation",
                        "ERR_INVALID_VENDOR_GST_NUMBER"
                );
            }

            gstNumber =
                    gstNumber
                            .trim()
                            .toUpperCase();

            /*
             * First 2 characters of GSTIN
             * represent the state code.
             *
             * Example:
             * 09GFFGH5465HFZG
             *
             * Supplier State = 09
             */
            String vendorStateCode =
                    gstNumber.substring(
                            0,
                            2
                    );

            if (!vendorStateCode.matches(
                    "\\d{2}"
            )) {

                throw new ValidationException(
                        "Invalid Vendor GST state code",
                        "ERR_INVALID_VENDOR_GST_STATE_CODE"
                );
            }

            String normalizedPlaceCode =
                    normalizeStateCode(
                            placeOfSupplyStateCode
                    );

            if (normalizedPlaceCode == null) {

                throw new ValidationException(
                        "Place of supply state code is required when GST is applicable",
                        "ERR_PLACE_OF_SUPPLY_REQUIRED"
                );
            }

            // =================================================
            // TOTAL GST
            //
            // Example:
            //
            // Base = 18000
            // GST = 18%
            //
            // Total GST = 3240
            // =================================================

            BigDecimal totalGstAmount =
                    finalAmount
                            .multiply(
                                    gstRate
                            )
                            .divide(
                                    HUNDRED,
                                    2,
                                    RoundingMode.HALF_UP
                            );

            boolean sameState =
                    vendorStateCode.equals(
                            normalizedPlaceCode
                    );

            if (sameState) {

                // =============================================
                // INTRA-STATE
                //
                // 18% GST
                //
                // CGST = 9%
                // SGST = 9%
                // =============================================

                cgstAmount =
                        totalGstAmount
                                .divide(
                                        TWO,
                                        2,
                                        RoundingMode.HALF_UP
                                );

                /*
                 * Using subtraction prevents
                 * one-paisa rounding difference.
                 */
                sgstAmount =
                        totalGstAmount
                                .subtract(
                                        cgstAmount
                                )
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP
                                );

            } else {

                // =============================================
                // INTER-STATE
                //
                // Full GST becomes IGST
                // =============================================

                igstAmount =
                        totalGstAmount;
            }

            placeOfSupplyStateCode =
                    normalizedPlaceCode;
        }

        // =====================================================
        // TOTAL GST
        // =====================================================

        BigDecimal totalTaxAmount =
                cgstAmount
                        .add(
                                sgstAmount
                        )
                        .add(
                                igstAmount
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        // =====================================================
        // GRAND TOTAL
        //
        // Base + GST - TDS
        // =====================================================

        BigDecimal grandTotal =
                finalAmount
                        .add(
                                totalTaxAmount
                        )
                        .subtract(
                                tdsAmount
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        // =====================================================
        // SET ENTITY VALUES
        // =====================================================

        po.setFinalAmount(
                finalAmount
        );

        po.setGstRate(
                gstRate
        );

        po.setCgstAmount(
                cgstAmount
        );

        po.setSgstAmount(
                sgstAmount
        );

        po.setIgstAmount(
                igstAmount
        );

        po.setTotalTaxAmount(
                totalTaxAmount
        );

        po.setTdsPercentage(
                tdsPercentage
        );

        po.setTdsAmount(
                tdsAmount
        );

        po.setGrandTotal(
                grandTotal
        );

        po.setPlaceOfSupplyStateCode(
                normalizeStateCode(
                        placeOfSupplyStateCode
                )
        );

        po.setVendorGSTRegistrationType(
                vendor.getGstRegistrationType()
        );
    }

    // =========================================================
    // VALIDATE PO VALUE AGAINST PROJECT
    // =========================================================

    private void validatePoValueNotGreaterThanProjectValue(
            BigDecimal grandTotal,
            BigDecimal finalAmount,
            ProcurementMilestoneAssignment procurement
    ) {

        if (procurement == null) {

            throw new ValidationException(
                    "Procurement assignment is required",
                    "ERR_PROCUREMENT_ASSIGNMENT_REQUIRED"
            );
        }

        if (procurement.getProject()
                == null) {

            throw new ValidationException(
                    "Project not found for procurement assignment",
                    "ERR_PROJECT_NOT_FOUND"
            );
        }

        if (procurement
                .getProject()
                .getPaymentDetail()
                == null) {

            throw new ValidationException(
                    "Project payment detail not found",
                    "ERR_PROJECT_PAYMENT_DETAIL_NOT_FOUND"
            );
        }

        BigDecimal poValue =
                grandTotal != null
                        ? grandTotal
                        : finalAmount;

        if (poValue == null
                || poValue.compareTo(
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
                order.getProcurementAssignment()
                        != null
                        ? order
                        .getProcurementAssignment()
                        .getId()
                        : null
        );

        dto.setProjectId(
                order.getProject()
                        != null
                        ? order
                        .getProject()
                        .getId()
                        : null
        );

        dto.setProjectName(
                order.getProject()
                        != null
                        ? order
                        .getProject()
                        .getName()
                        : null
        );

        dto.setVendorId(
                order.getVendor()
                        != null
                        ? order
                        .getVendor()
                        .getId()
                        : null
        );

        dto.setVendorName(
                order.getVendor()
                        != null
                        ? order
                        .getVendor()
                        .getName()
                        : null
        );

        dto.setVendorContactId(
                order.getVendorContact()
                        != null
                        ? order
                        .getVendorContact()
                        .getId()
                        : null
        );

        dto.setVendorContactName(
                order.getVendorContact()
                        != null
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
                order.getFinalAmount()
        );

        dto.setGstRate(
                order.getGstRate()
        );

        dto.setCgstAmount(
                order.getCgstAmount()
        );

        dto.setSgstAmount(
                order.getSgstAmount()
        );

        dto.setIgstAmount(
                order.getIgstAmount()
        );

        dto.setTotalTaxAmount(
                order.getTotalTaxAmount()
        );

        dto.setGrandTotal(
                order.getGrandTotal()
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
                order.getPaymentType()
                        != null
                        ? order
                        .getPaymentType()
                        .getId()
                        : null
        );

        dto.setPaymentTypeName(
                order.getPaymentType()
                        != null
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

        if (po.getProcurementAssignment()
                != null) {

            dto.setProcurementAssignmentId(
                    po
                            .getProcurementAssignment()
                            .getId()
            );
        }

        // =====================================================
        // PROJECT
        // =====================================================

        if (po.getProject()
                != null) {

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

        if (po.getVendor()
                != null) {

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

            // =================================================
            // VENDOR ADDRESS
            // Exact fields from your Vendor entity
            // =================================================

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

            // =================================================
            // VENDOR GST
            // =================================================

            dto.setVendorGSTNumber(
                    vendor.getGstNumber()
            );

            VendorGSTRegistrationType registrationType =
                    po.getVendorGSTRegistrationType()
                            != null
                            ? po.getVendorGSTRegistrationType()
                            : vendor.getGstRegistrationType();

            dto.setVendorGSTRegistrationType(
                    registrationType
            );

            dto.setVendorPANNumber(
                    vendor.getPanNumber()
            );

            if (vendor.getGstNumber()
                    != null
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

        dto.setPlaceOfSupplyStateCode(
                po.getPlaceOfSupplyStateCode()
        );

        // =====================================================
        // AMOUNTS
        // =====================================================

        dto.setFinalAmount(
                moneyOrZero(
                        po.getFinalAmount()
                )
        );

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
        // PAYMENT
        // =====================================================

        if (po.getPaymentType()
                != null) {

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
    // PAYMENT TYPE HELPER
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
    // PERCENTAGE VALIDATION
    // =========================================================

    private void validatePercentage(
            BigDecimal value,
            String fieldName,
            String errorCode
    ) {

        if (value == null) {
            return;
        }

        if (value.compareTo(
                BigDecimal.ZERO
        ) < 0
                ||
                value.compareTo(
                        HUNDRED
                ) > 0) {

            throw new ValidationException(
                    fieldName
                            + " must be between 0 and 100",
                    errorCode
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

            return BigDecimal.ZERO
                    .setScale(
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
    // =========================================================

    private BigDecimal percentageOrZero(
            BigDecimal value
    ) {

        if (value == null) {

            return BigDecimal.ZERO
                    .setScale(
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
    // STATE CODE
    // =========================================================

    private String normalizeStateCode(
            String stateCode
    ) {

        if (stateCode == null
                || stateCode
                .trim()
                .isEmpty()) {

            return null;
        }

        String normalized =
                stateCode.trim();

        /*
         * Example:
         * 9 -> 09
         */
        if (normalized.matches(
                "\\d"
        )) {

            normalized =
                    "0" + normalized;
        }

        if (!normalized.matches(
                "\\d{2}"
        )) {

            throw new ValidationException(
                    "State code must contain exactly 2 digits",
                    "ERR_INVALID_STATE_CODE"
            );
        }

        return normalized;
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