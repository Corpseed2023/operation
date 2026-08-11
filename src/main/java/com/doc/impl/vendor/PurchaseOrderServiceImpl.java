package com.doc.impl.vendor;

import com.doc.dto.vendor.ProcurementOrderResponseDto;
import com.doc.dto.vendor.PurchaseOrderRequestDto;
import com.doc.dto.vendor.PurchaseOrderResponseDto;
import com.doc.entity.client.PaymentType;
import com.doc.entity.project.ProcurementStatus;
import com.doc.entity.user.User;
import com.doc.entity.vendor.ProcurementMilestoneAssignment;
import com.doc.entity.vendor.ProcurementOrder;
import com.doc.entity.vendor.ProcurementOrderStatus;
import com.doc.entity.vendor.Vendor;
import com.doc.entity.vendor.VendorFinalization;
import com.doc.entity.vendor.VendorFinalizationStatus;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.PaymentTypeRepository;
import com.doc.repository.ProcurementMilestoneAssignmentRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.vendor.PurchaseOrderRepository;
import com.doc.repository.vendor.VendorFinalizationRepository;
import com.doc.repository.vendor.VendorRepository;
import com.doc.service.vendor.PurchaseOrderService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger logger = LogManager.getLogger(PurchaseOrderServiceImpl.class);

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

    @Autowired
    private VendorFinalizationRepository vendorFinalizationRepository;

    /**
     * Buyer's GST state code. This is configured on the backend so that the
     * client cannot decide whether CGST/SGST or IGST applies.
     */
    @Value("${company.gst.state-code}")
    private String companyStateCode;

    @Override
    public PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto dto) {

        logger.info("Creating Purchase Order for procurementAssignmentId={}", dto.getProcurementAssignmentId());

        validateCreateRequest(dto);

        ProcurementMilestoneAssignment procurement = procurementRepository
                .findById(dto.getProcurementAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Procurement assignment not found",
                        "ERR_PROCUREMENT_NOT_FOUND"
                ));

        Vendor vendor = vendorRepository
                .findByIdAndIsDeletedFalse(dto.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor not found",
                        "ERR_VENDOR_NOT_FOUND"
                ));

        BigDecimal finalizedGstRate = getFinalizedGstRate(
                dto.getVendorFinalizationId(),
                vendor
        );

        User createdByUser = userRepository
                .findActiveUserById(dto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CreatedBy user not found",
                        "ERR_USER_NOT_FOUND"
                ));

        PoAmountBreakup amountBreakup = calculatePoAmountBreakup(
                vendor,
                dto.getFinalAmount(),
                finalizedGstRate,
                dto.getTdsPercentage()
        );

        validatePoValueNotGreaterThanProjectValue(
                amountBreakup.getGrandTotal(),
                amountBreakup.getFinalAmount(),
                procurement
        );

        Date currentDate = new Date();

        ProcurementOrder po = new ProcurementOrder();

        po.setProcurementAssignment(procurement);
        po.setProject(procurement.getProject());
        po.setVendor(vendor);

        po.setPoNumber(generatePoNumber());
        po.setPoReferenceNumber(dto.getPoReferenceNumber());

        po.setVendorGSTRegistrationType(vendor.getGstRegistrationType());
        po.setPlaceOfSupplyStateCode(getConfiguredCompanyStateCode());

        po.setFinalAmount(amountBreakup.getFinalAmount());
        po.setGstRate(amountBreakup.getGstRate());
        po.setCgstAmount(amountBreakup.getCgstAmount());
        po.setSgstAmount(amountBreakup.getSgstAmount());
        po.setIgstAmount(amountBreakup.getIgstAmount());
        po.setTdsPercentage(amountBreakup.getTdsPercentage());
        po.setTdsAmount(amountBreakup.getTdsAmount());
        po.setTotalTaxAmount(amountBreakup.getTotalTaxAmount());
        po.setGrandTotal(amountBreakup.getGrandTotal());

        po.setScopeOfWork(dto.getScopeOfWork());
        po.setTermsAndConditions(dto.getTermsAndConditions());
        po.setRemarks(dto.getRemarks());

        if (dto.getAttachmentUrls() != null) {
            po.setAttachmentUrls(dto.getAttachmentUrls());
        }

        po.setStatus(ProcurementOrderStatus.DRAFT);
        po.setPoCreatedDate(currentDate);

        po.setCreatedBy(createdByUser.getId());
        po.setUpdatedBy(createdByUser.getId());
        po.setCreatedDate(currentDate);
        po.setUpdatedDate(currentDate);

        if (dto.getPaymentTypeName() != null && !dto.getPaymentTypeName().trim().isEmpty()) {
            PaymentType paymentType = paymentTypeRepository
                    .findByName(dto.getPaymentTypeName().trim())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment type not found: " + dto.getPaymentTypeName(),
                            "ERR_PAYMENT_TYPE_NOT_FOUND"
                    ));

            po.setPaymentType(paymentType);
        }

        ProcurementOrder savedPo = purchaseOrderRepository.save(po);

        procurement.setStatus(ProcurementStatus.PO_CREATED);
        procurement.setSelectedVendor(vendor);
        procurement.setPoCreatedDate(currentDate);
        procurement.setUpdatedBy(createdByUser.getId());
        procurement.setUpdatedDate(currentDate);

        procurementRepository.save(procurement);

        logger.info(
                "Purchase Order created successfully. poNumber={}, createdBy={}",
                savedPo.getPoNumber(),
                createdByUser.getId()
        );

        return convertToPurchaseOrderResponseDto(savedPo);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDto getPurchaseOrderById(Long id) {

        logger.info("Fetching Purchase Order by id={}", id);

        ProcurementOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase Order not found",
                        "ERR_PO_NOT_FOUND"
                ));

        if (po.isDeleted()) {
            throw new ValidationException(
                    "Deleted Purchase Order cannot be fetched",
                    "ERR_DELETED_PO"
            );
        }

        return convertToPurchaseOrderResponseDto(po);
    }

    @Override
    public PurchaseOrderResponseDto releasePurchaseOrder(Long poId, Long userId) {
        return approvePurchaseOrderInternal(poId, userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDto getByProcurementAssignmentId(Long procurementAssignmentId) {

        if (procurementAssignmentId == null) {
            throw new ValidationException(
                    "Procurement Assignment ID is required",
                    "ERR_NULL_ID"
            );
        }

        ProcurementOrder po = purchaseOrderRepository
                .findByProcurementAssignmentId(procurementAssignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Purchase Order found for this procurement assignment",
                        "ERR_PO_NOT_FOUND"
                ));

        if (po.isDeleted()) {
            throw new ValidationException(
                    "Deleted Purchase Order cannot be fetched",
                    "ERR_DELETED_PO"
            );
        }

        return convertToPurchaseOrderResponseDto(po);
    }

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

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size <= 0 ? 10 : size,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Page<ProcurementOrder> purchaseOrders =
                purchaseOrderRepository.findByProjectIdAndIsDeletedFalse(projectId, pageable);

        return purchaseOrders.map(this::convertToPurchaseOrderResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementOrderResponseDto> getProcurementOrdersByStatus(
            ProcurementOrderStatus status,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size <= 0 ? 10 : size,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Page<ProcurementOrder> orders = status == null
                ? purchaseOrderRepository.findByIsDeletedFalse(pageable)
                : purchaseOrderRepository.findByStatusAndIsDeletedFalse(status, pageable);

        return orders.map(this::mapToProcurementOrderResponseDto);
    }

    @Override
    public ProcurementOrderResponseDto approveProcurementOrder(
            Long procurementOrderId,
            Long userId,
            String comment
    ) {

        PurchaseOrderResponseDto approved = approvePurchaseOrderInternal(
                procurementOrderId,
                userId,
                comment
        );

        ProcurementOrder order = purchaseOrderRepository.findById(approved.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase Order not found",
                        "ERR_PO_NOT_FOUND"
                ));

        return mapToProcurementOrderResponseDto(order);
    }

    @Override
    public ProcurementOrderResponseDto rejectProcurementOrder(
            Long procurementOrderId,
            Long userId,
            String reason
    ) {
        throw new ValidationException(
                "PO rejection flow is removed. Purchase Order supports only DRAFT and APPROVED status.",
                "ERR_PO_REJECTION_FLOW_REMOVED"
        );
    }

    @Override
    public PurchaseOrderResponseDto updatePurchaseOrder(Long poId, PurchaseOrderRequestDto dto) {

        logger.info("Updating Purchase Order id={}", poId);

        if (poId == null) {
            throw new ValidationException(
                    "Purchase Order ID is required",
                    "ERR_PO_ID_REQUIRED"
            );
        }

        ProcurementOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase Order not found",
                        "ERR_PO_NOT_FOUND"
                ));

        if (po.isDeleted()) {
            throw new ValidationException(
                    "Deleted Purchase Order cannot be updated",
                    "ERR_DELETED_PO_CANNOT_BE_UPDATED"
            );
        }

        if (po.getStatus() != ProcurementOrderStatus.DRAFT) {
            throw new ValidationException(
                    "Only DRAFT Purchase Order can be updated. Current status: " + po.getStatus(),
                    "ERR_INVALID_PO_STATUS_FOR_UPDATE"
            );
        }

        validateUpdateRequest(dto);

        ProcurementMilestoneAssignment procurementForValidation = po.getProcurementAssignment();

        if (dto.getProcurementAssignmentId() != null) {
            procurementForValidation = procurementRepository
                    .findById(dto.getProcurementAssignmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Procurement assignment not found",
                            "ERR_PROCUREMENT_NOT_FOUND"
                    ));
        }

        Vendor vendorForCalculation = po.getVendor();

        if (dto.getVendorId() != null) {
            vendorForCalculation = vendorRepository.findByIdAndIsDeletedFalse(dto.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Vendor not found",
                            "ERR_VENDOR_NOT_FOUND"
                    ));
        }

        BigDecimal finalizedGstRate = getFinalizedGstRate(
                dto.getVendorFinalizationId(),
                vendorForCalculation
        );

        PoAmountBreakup amountBreakup = calculatePoAmountBreakup(
                vendorForCalculation,
                dto.getFinalAmount(),
                finalizedGstRate,
                dto.getTdsPercentage()
        );

        validatePoValueNotGreaterThanProjectValue(
                amountBreakup.getGrandTotal(),
                amountBreakup.getFinalAmount(),
                procurementForValidation
        );

        if (dto.getProcurementAssignmentId() != null) {
            po.setProcurementAssignment(procurementForValidation);
            po.setProject(procurementForValidation.getProject());
        }

        if (dto.getVendorId() != null) {
            po.setVendor(vendorForCalculation);
            po.setVendorGSTRegistrationType(vendorForCalculation.getGstRegistrationType());
        }

        po.setPoReferenceNumber(dto.getPoReferenceNumber());
        po.setPlaceOfSupplyStateCode(getConfiguredCompanyStateCode());

        po.setFinalAmount(amountBreakup.getFinalAmount());
        po.setGstRate(amountBreakup.getGstRate());
        po.setCgstAmount(amountBreakup.getCgstAmount());
        po.setSgstAmount(amountBreakup.getSgstAmount());
        po.setIgstAmount(amountBreakup.getIgstAmount());
        po.setTdsPercentage(amountBreakup.getTdsPercentage());
        po.setTdsAmount(amountBreakup.getTdsAmount());
        po.setTotalTaxAmount(amountBreakup.getTotalTaxAmount());
        po.setGrandTotal(amountBreakup.getGrandTotal());

        po.setScopeOfWork(dto.getScopeOfWork());
        po.setTermsAndConditions(dto.getTermsAndConditions());
        po.setRemarks(dto.getRemarks());

        if (dto.getAttachmentUrls() != null) {
            po.setAttachmentUrls(dto.getAttachmentUrls());
        }

        Long updatedBy = dto.getUserId() != null ? dto.getUserId() : dto.getCreatedBy();

        if (updatedBy != null) {
            User updatedByUser = userRepository.findActiveUserById(updatedBy)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "UpdatedBy user not found",
                            "ERR_USER_NOT_FOUND"
                    ));

            po.setUpdatedBy(updatedByUser.getId());
        }

        if (dto.getPaymentTypeName() != null && !dto.getPaymentTypeName().trim().isEmpty()) {
            PaymentType paymentType = paymentTypeRepository
                    .findByName(dto.getPaymentTypeName().trim())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment type not found: " + dto.getPaymentTypeName(),
                            "ERR_PAYMENT_TYPE_NOT_FOUND"
                    ));

            po.setPaymentType(paymentType);
        }

        po.setUpdatedDate(new Date());

        ProcurementOrder savedPo = purchaseOrderRepository.save(po);

        logger.info("Purchase Order updated successfully. poNumber={}", savedPo.getPoNumber());

        return convertToPurchaseOrderResponseDto(savedPo);
    }

    @Override
    public PurchaseOrderResponseDto updatePurchaseOrderStatus(
            Long poId,
            ProcurementOrderStatus newStatus,
            Long userId,
            String remarks
    ) {

        logger.info(
                "Updating Purchase Order status. poId={}, newStatus={}, userId={}",
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

        if (newStatus != ProcurementOrderStatus.APPROVED) {
            throw new ValidationException(
                    "Only APPROVED status update is allowed. Purchase Order supports only DRAFT and APPROVED.",
                    "ERR_INVALID_PO_STATUS"
            );
        }

        return approvePurchaseOrderInternal(poId, userId, remarks);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponseDto> getPurchaseOrdersByUserId(
            Long userId,
            int page,
            int size
    ) {

        logger.info("Fetching Purchase Orders by userId={}, page={}, size={}", userId, page, size);

        if (userId == null) {
            throw new ValidationException(
                    "User ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size <= 0 ? 10 : size,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Page<ProcurementOrder> orders =
                purchaseOrderRepository.findByCreatedByAndIsDeletedFalse(userId, pageable);

        logger.info(
                "Fetched {} Purchase Orders for userId={}",
                orders.getNumberOfElements(),
                userId
        );

        return orders.map(this::convertToPurchaseOrderResponseDto);
    }

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

        ProcurementOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase Order not found",
                        "ERR_PO_NOT_FOUND"
                ));

        if (po.isDeleted()) {
            throw new ValidationException(
                    "Deleted Purchase Order cannot be approved",
                    "ERR_DELETED_PO"
            );
        }

        if (po.getStatus() != ProcurementOrderStatus.DRAFT) {
            throw new ValidationException(
                    "Only DRAFT Purchase Order can be approved. Current status: " + po.getStatus(),
                    "ERR_INVALID_PO_STATUS"
            );
        }

        User approvedByUser = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        Date currentDate = new Date();

        po.setStatus(ProcurementOrderStatus.APPROVED);
        po.setApprovedBy(approvedByUser.getId());
        po.setPoApprovedDate(currentDate);
        po.setUpdatedBy(approvedByUser.getId());
        po.setUpdatedDate(currentDate);

        if (remarks != null && !remarks.trim().isEmpty()) {
            po.setRemarks(remarks.trim());
        }

        ProcurementOrder savedPo = purchaseOrderRepository.save(po);

        ProcurementMilestoneAssignment procurement = savedPo.getProcurementAssignment();

        if (procurement != null) {
            procurement.setStatus(ProcurementStatus.PO_APPROVED);
            procurement.setUpdatedBy(approvedByUser.getId());
            procurement.setUpdatedDate(currentDate);
            procurementRepository.save(procurement);
        }

        logger.info(
                "Purchase Order approved successfully. poNumber={}, approvedBy={}",
                savedPo.getPoNumber(),
                approvedByUser.getId()
        );

        return convertToPurchaseOrderResponseDto(savedPo);
    }

    private void validateCreateRequest(PurchaseOrderRequestDto dto) {

        if (dto == null) {
            throw new ValidationException(
                    "Purchase Order request is required",
                    "ERR_PO_REQUEST_REQUIRED"
            );
        }

        if (dto.getProcurementAssignmentId() == null) {
            throw new ValidationException(
                    "Procurement assignment ID is required",
                    "ERR_PROCUREMENT_ASSIGNMENT_ID_REQUIRED"
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
                    "Created by user ID is required",
                    "ERR_CREATED_BY_REQUIRED"
            );
        }

        if (dto.getVendorFinalizationId() == null) {
            throw new ValidationException(
                    "Vendor finalization ID is required",
                    "ERR_VENDOR_FINALIZATION_ID_REQUIRED"
            );
        }

        validateAmountFields(dto);
    }

    private void validateUpdateRequest(PurchaseOrderRequestDto dto) {

        if (dto == null) {
            throw new ValidationException(
                    "Purchase Order request is required",
                    "ERR_PO_REQUEST_REQUIRED"
            );
        }

        if (dto.getVendorFinalizationId() == null) {
            throw new ValidationException(
                    "Vendor finalization ID is required",
                    "ERR_VENDOR_FINALIZATION_ID_REQUIRED"
            );
        }

        validateAmountFields(dto);
    }

    private void validateAmountFields(PurchaseOrderRequestDto dto) {

        if (dto.getFinalAmount() == null || dto.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "Final amount must be greater than zero",
                    "ERR_INVALID_AMOUNT"
            );
        }

        if (dto.getTdsPercentage() == null) {
            throw new ValidationException(
                    "TDS percentage is required",
                    "ERR_TDS_PERCENTAGE_REQUIRED"
            );
        }

        if (dto.getTdsPercentage().compareTo(BigDecimal.ZERO) < 0
                || dto.getTdsPercentage().compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException(
                    "TDS percentage must be between 0 and 100",
                    "ERR_INVALID_TDS_PERCENTAGE"
            );
        }
    }

    /**
     * Loads GST from the selected finalized vendor record. The request DTO does
     * not contain a GST rate, so callers cannot override the finalized rate.
     */
    private BigDecimal getFinalizedGstRate(
            Long vendorFinalizationId,
            Vendor vendor
    ) {
        if (vendorFinalizationId == null) {
            throw new ValidationException(
                    "Vendor finalization ID is required",
                    "ERR_VENDOR_FINALIZATION_ID_REQUIRED"
            );
        }

        VendorFinalization finalization = vendorFinalizationRepository
                .findByIdAndIsDeletedFalse(vendorFinalizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor finalization not found",
                        "ERR_VENDOR_FINALIZATION_NOT_FOUND"
                ));

        if (finalization.getStatus() != VendorFinalizationStatus.FINALIZED) {
            throw new ValidationException(
                    "Purchase Order can only be created from a finalized vendor record",
                    "ERR_VENDOR_NOT_FINALIZED"
            );
        }

        if (finalization.getVendor() == null
                || vendor == null
                || !finalization.getVendor().getId().equals(vendor.getId())) {
            throw new ValidationException(
                    "Selected vendor does not match the vendor finalization",
                    "ERR_VENDOR_FINALIZATION_MISMATCH"
            );
        }

        BigDecimal gstRate = finalization.getTaxPercent();

        if (gstRate == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (gstRate.compareTo(BigDecimal.ZERO) < 0
                || gstRate.compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException(
                    "Vendor finalization contains an invalid GST percentage",
                    "ERR_INVALID_FINALIZATION_GST_RATE"
            );
        }

        return gstRate.setScale(2, RoundingMode.HALF_UP);
    }

    private String getConfiguredCompanyStateCode() {
        if (companyStateCode == null
                || !companyStateCode.trim().matches("^[0-9]{2}$")) {
            throw new ValidationException(
                    "Company GST state code must be configured as exactly 2 digits",
                    "ERR_INVALID_COMPANY_GST_STATE_CODE"
            );
        }

        return companyStateCode.trim();
    }

    private PoAmountBreakup calculatePoAmountBreakup(
            Vendor vendor,
            BigDecimal finalAmount,
            BigDecimal gstRate,
            BigDecimal tdsPercentage
    ) {

        BigDecimal baseAmount = finalAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal normalizedGstRate = gstRate.setScale(2, RoundingMode.HALF_UP);
        BigDecimal normalizedTdsPercentage = tdsPercentage.setScale(2, RoundingMode.HALF_UP);

        BigDecimal cgstAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgstAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal igstAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        String vendorStateCode = getVendorStateCode(
                vendor != null ? vendor.getGstNumber() : null
        );

        String buyerStateCode = getConfiguredCompanyStateCode();

        if (normalizedGstRate.compareTo(BigDecimal.ZERO) > 0) {

            if (vendorStateCode == null) {
                throw new ValidationException(
                        "Valid vendor GST number is required for GST calculation",
                        "ERR_VENDOR_GST_REQUIRED"
                );
            }

            if (vendorStateCode.equals(buyerStateCode)) {
                BigDecimal halfGstRate = normalizedGstRate
                        .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

                cgstAmount = calculatePercentageAmount(baseAmount, halfGstRate);
                sgstAmount = calculatePercentageAmount(baseAmount, halfGstRate);
            } else {
                igstAmount = calculatePercentageAmount(baseAmount, normalizedGstRate);
            }
        }

        BigDecimal totalTaxAmount = cgstAmount
                .add(sgstAmount)
                .add(igstAmount)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal tdsAmount = calculatePercentageAmount(
                baseAmount,
                normalizedTdsPercentage
        );

        BigDecimal grandTotal = baseAmount
                .add(totalTaxAmount)
                .subtract(tdsAmount)
                .setScale(2, RoundingMode.HALF_UP);

        return new PoAmountBreakup(
                baseAmount,
                normalizedGstRate,
                cgstAmount,
                sgstAmount,
                igstAmount,
                normalizedTdsPercentage,
                tdsAmount,
                totalTaxAmount,
                grandTotal
        );
    }

    private BigDecimal calculatePercentageAmount(BigDecimal amount, BigDecimal percentage) {
        return amount
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

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

        if (procurement.getProject() == null) {
            throw new ValidationException(
                    "Project not found for procurement assignment",
                    "ERR_PROJECT_NOT_FOUND"
            );
        }

        if (procurement.getProject().getPaymentDetail() == null) {
            throw new ValidationException(
                    "Project payment detail not found",
                    "ERR_PROJECT_PAYMENT_DETAIL_NOT_FOUND"
            );
        }

        BigDecimal poValue = grandTotal != null ? grandTotal : finalAmount;

        if (poValue == null || poValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "PO value must be greater than zero",
                    "ERR_INVALID_PO_VALUE"
            );
        }

        BigDecimal projectValue = toBigDecimal(
                procurement.getProject().getPaymentDetail().getTotalAmount()
        );

        if (poValue.compareTo(projectValue) > 0) {
            throw new ValidationException(
                    "PO value cannot be greater than project value. Project value: "
                            + projectValue
                            + ", PO value: "
                            + poValue,
                    "ERR_PO_VALUE_EXCEEDS_PROJECT_VALUE"
            );
        }
    }

    private BigDecimal toBigDecimal(Object value) {

        if (value == null) {
            throw new ValidationException(
                    "Project total amount is required",
                    "ERR_PROJECT_TOTAL_AMOUNT_REQUIRED"
            );
        }

        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
        }

        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private String generatePoNumber() {
        int year = LocalDate.now().getYear();
        long count = purchaseOrderRepository.count() + 1;
        return String.format("CORP-PO-%d-%05d", year, count);
    }

    private PurchaseOrderResponseDto convertToPurchaseOrderResponseDto(ProcurementOrder po) {

        PurchaseOrderResponseDto dto = new PurchaseOrderResponseDto();

        dto.setId(po.getId());
        dto.setPoNumber(po.getPoNumber());
        dto.setPoReferenceNumber(po.getPoReferenceNumber());

        dto.setProcurementAssignmentId(
                po.getProcurementAssignment() != null
                        ? po.getProcurementAssignment().getId()
                        : null
        );

        dto.setProjectId(
                po.getProject() != null
                        ? po.getProject().getId()
                        : null
        );

        dto.setProjectName(
                po.getProject() != null
                        ? po.getProject().getName()
                        : null
        );

        dto.setProjectNo(
                po.getProject() != null
                        ? po.getProject().getProjectNo()
                        : null
        );

        if (po.getVendor() != null) {
            Vendor vendor = po.getVendor();

            dto.setVendorId(vendor.getId());
            dto.setVendorName(vendor.getName());
            dto.setVendorEmail(vendor.getEmail());
            dto.setVendorMobile(vendor.getMobile());
            dto.setVendorAddress(vendor.getFullAddress());
            dto.setVendorCity(vendor.getCity());
            dto.setVendorState(vendor.getState());
            dto.setVendorCountry(vendor.getCountry());
            dto.setVendorGSTNumber(vendor.getGstNumber());
            dto.setVendorGSTRegistrationType(vendor.getGstRegistrationType());
            dto.setVendorStateCode(getVendorStateCode(vendor.getGstNumber()));
            dto.setVendorPANNumber(vendor.getPanNumber());
        } else {
            dto.setVendorGSTRegistrationType(po.getVendorGSTRegistrationType());
        }

        dto.setPlaceOfSupplyStateCode(po.getPlaceOfSupplyStateCode());

        dto.setFinalAmount(po.getFinalAmount());
        dto.setGstRate(po.getGstRate());

        dto.setCgstRate(getCgstRate(po));
        dto.setSgstRate(getSgstRate(po));
        dto.setIgstRate(getIgstRate(po));

        dto.setCgstAmount(po.getCgstAmount());
        dto.setSgstAmount(po.getSgstAmount());
        dto.setIgstAmount(po.getIgstAmount());
        dto.setTotalTaxAmount(po.getTotalTaxAmount());

        dto.setTdsPercentage(po.getTdsPercentage());
        dto.setTdsAmount(po.getTdsAmount());

        dto.setGrandTotal(po.getGrandTotal());

        dto.setScopeOfWork(po.getScopeOfWork());
        dto.setTermsAndConditions(po.getTermsAndConditions());
        dto.setRemarks(po.getRemarks());

        dto.setStatus(po.getStatus());

        dto.setPaymentTypeName(
                po.getPaymentType() != null
                        ? po.getPaymentType().getName()
                        : null
        );

        dto.setAttachmentUrls(po.getAttachmentUrls());

        dto.setPoCreatedDate(po.getPoCreatedDate());
        dto.setPoSubmittedForApprovalDate(po.getPoSubmittedForApprovalDate());
        dto.setPoApprovedDate(po.getPoApprovedDate());
        dto.setPoReleasedDate(po.getPoReleasedDate());

        dto.setCreatedBy(po.getCreatedBy());
        dto.setUpdatedBy(po.getUpdatedBy());
        dto.setApprovedBy(po.getApprovedBy());
        dto.setCreatedDate(po.getCreatedDate());
        dto.setUpdatedDate(po.getUpdatedDate());

        return dto;
    }

    private ProcurementOrderResponseDto mapToProcurementOrderResponseDto(ProcurementOrder order) {

        ProcurementOrderResponseDto dto = new ProcurementOrderResponseDto();

        dto.setId(order.getId());

        dto.setProcurementAssignmentId(
                order.getProcurementAssignment() != null
                        ? order.getProcurementAssignment().getId()
                        : null
        );

        dto.setProjectId(
                order.getProject() != null
                        ? order.getProject().getId()
                        : null
        );

        dto.setProjectName(
                order.getProject() != null
                        ? order.getProject().getName()
                        : null
        );

        dto.setVendorId(
                order.getVendor() != null
                        ? order.getVendor().getId()
                        : null
        );

        dto.setVendorName(
                order.getVendor() != null
                        ? order.getVendor().getName()
                        : null
        );

        dto.setVendorContactId(
                order.getVendorContact() != null
                        ? order.getVendorContact().getId()
                        : null
        );

        dto.setVendorContactName(
                order.getVendorContact() != null
                        ? order.getVendorContact().getName()
                        : null
        );

        dto.setPoNumber(order.getPoNumber());
        dto.setPoReferenceNumber(order.getPoReferenceNumber());

        dto.setFinalAmount(order.getFinalAmount());
        dto.setGstRate(order.getGstRate());
        dto.setCgstAmount(order.getCgstAmount());
        dto.setSgstAmount(order.getSgstAmount());
        dto.setIgstAmount(order.getIgstAmount());
        dto.setTdsPercentage(order.getTdsPercentage());
        dto.setTdsAmount(order.getTdsAmount());
        dto.setTotalTaxAmount(order.getTotalTaxAmount());
        dto.setGrandTotal(order.getGrandTotal());

        dto.setScopeOfWork(order.getScopeOfWork());
        dto.setTermsAndConditions(order.getTermsAndConditions());
        dto.setRemarks(order.getRemarks());

        dto.setAttachmentUrls(order.getAttachmentUrls());

        dto.setStatus(order.getStatus());

        dto.setPoCreatedDate(order.getPoCreatedDate());
        dto.setPoSubmittedForApprovalDate(order.getPoSubmittedForApprovalDate());
        dto.setPoApprovedDate(order.getPoApprovedDate());
        dto.setPoReleasedDate(order.getPoReleasedDate());

        dto.setPaymentTypeId(
                order.getPaymentType() != null
                        ? order.getPaymentType().getId()
                        : null
        );

        dto.setPaymentTypeName(
                order.getPaymentType() != null
                        ? order.getPaymentType().getName()
                        : null
        );

        dto.setCreatedBy(order.getCreatedBy());
        dto.setUpdatedBy(order.getUpdatedBy());
        dto.setApprovedBy(order.getApprovedBy());

        dto.setCreatedDate(order.getCreatedDate());
        dto.setUpdatedDate(order.getUpdatedDate());

        return dto;
    }

    private String getVendorStateCode(String gstNumber) {

        if (gstNumber == null || gstNumber.trim().length() < 2) {
            return null;
        }

        return gstNumber.trim().substring(0, 2);
    }

    private BigDecimal getCgstRate(ProcurementOrder po) {

        if (po.getCgstAmount() != null
                && po.getCgstAmount().compareTo(BigDecimal.ZERO) > 0
                && po.getGstRate() != null) {
            return po.getGstRate()
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getSgstRate(ProcurementOrder po) {

        if (po.getSgstAmount() != null
                && po.getSgstAmount().compareTo(BigDecimal.ZERO) > 0
                && po.getGstRate() != null) {
            return po.getGstRate()
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getIgstRate(ProcurementOrder po) {

        if (po.getIgstAmount() != null
                && po.getIgstAmount().compareTo(BigDecimal.ZERO) > 0
                && po.getGstRate() != null) {
            return po.getGstRate().setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static class PoAmountBreakup {

        private final BigDecimal finalAmount;

        private final BigDecimal gstRate;

        private final BigDecimal cgstAmount;

        private final BigDecimal sgstAmount;

        private final BigDecimal igstAmount;

        private final BigDecimal tdsPercentage;

        private final BigDecimal tdsAmount;

        private final BigDecimal totalTaxAmount;

        private final BigDecimal grandTotal;

        private PoAmountBreakup(
                BigDecimal finalAmount,
                BigDecimal gstRate,
                BigDecimal cgstAmount,
                BigDecimal sgstAmount,
                BigDecimal igstAmount,
                BigDecimal tdsPercentage,
                BigDecimal tdsAmount,
                BigDecimal totalTaxAmount,
                BigDecimal grandTotal
        ) {
            this.finalAmount = finalAmount;
            this.gstRate = gstRate;
            this.cgstAmount = cgstAmount;
            this.sgstAmount = sgstAmount;
            this.igstAmount = igstAmount;
            this.tdsPercentage = tdsPercentage;
            this.tdsAmount = tdsAmount;
            this.totalTaxAmount = totalTaxAmount;
            this.grandTotal = grandTotal;
        }

        private BigDecimal getFinalAmount() {
            return finalAmount;
        }

        private BigDecimal getGstRate() {
            return gstRate;
        }

        private BigDecimal getCgstAmount() {
            return cgstAmount;
        }

        private BigDecimal getSgstAmount() {
            return sgstAmount;
        }

        private BigDecimal getIgstAmount() {
            return igstAmount;
        }

        private BigDecimal getTdsPercentage() {
            return tdsPercentage;
        }

        private BigDecimal getTdsAmount() {
            return tdsAmount;
        }

        private BigDecimal getTotalTaxAmount() {
            return totalTaxAmount;
        }

        private BigDecimal getGrandTotal() {
            return grandTotal;
        }
    }
}