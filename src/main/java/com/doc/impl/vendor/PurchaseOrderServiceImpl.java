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

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

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

    @Override
    @Transactional
    public PurchaseOrderResponseDto createPurchaseOrder(
            PurchaseOrderRequestDto dto
    ) {

        logger.info(
                "Creating Purchase Order for procurementAssignmentId: {}",
                dto.getProcurementAssignmentId()
        );

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

        User createdByUser = userRepository
                .findActiveUserById(dto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CreatedBy user not found",
                        "ERR_USER_NOT_FOUND"
                ));

        if (dto.getFinalAmount() == null
                || dto.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "Final amount must be greater than zero",
                    "ERR_INVALID_AMOUNT"
            );
        }

        BigDecimal finalAmount = dto.getFinalAmount()
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal tdsPercentage = dto.getTdsPercentage();

        if (tdsPercentage == null) {
            throw new ValidationException(
                    "TDS percentage is required",
                    "ERR_TDS_PERCENTAGE_REQUIRED"
            );
        }

        if (tdsPercentage.compareTo(BigDecimal.ZERO) < 0
                || tdsPercentage.compareTo(new BigDecimal("100")) > 0) {

            throw new ValidationException(
                    "TDS percentage must be between 0 and 100",
                    "ERR_INVALID_TDS_PERCENTAGE"
            );
        }

        tdsPercentage = tdsPercentage.setScale(
                2,
                RoundingMode.HALF_UP
        );

        BigDecimal tdsAmount = finalAmount
                .multiply(tdsPercentage)
                .divide(
                        new BigDecimal("100"),
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal totalTaxAmount = dto.getTotalTaxAmount() != null
                ? dto.getTotalTaxAmount().setScale(
                2,
                RoundingMode.HALF_UP
        )
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        /*
         * Existing GST calculation and breakup remain unchanged.
         *
         * Grand Total = Final Amount + GST - TDS
         */
        BigDecimal grandTotal = finalAmount
                .add(totalTaxAmount)
                .subtract(tdsAmount)
                .setScale(2, RoundingMode.HALF_UP);

        validatePoValueNotGreaterThanProjectValue(
                grandTotal,
                finalAmount,
                procurement
        );

        String poNumber = generatePoNumber();
        Date currentDate = new Date();

        ProcurementOrder po = new ProcurementOrder();

        po.setProcurementAssignment(procurement);
        po.setProject(procurement.getProject());
        po.setVendor(vendor);

        po.setPoNumber(poNumber);
        po.setPoReferenceNumber(dto.getPoReferenceNumber());

        po.setFinalAmount(finalAmount);

        po.setTdsPercentage(tdsPercentage);
        po.setTdsAmount(tdsAmount);

        /*
         * Existing CGST, SGST and IGST logic remains unchanged.
         */
        po.setGstRate(dto.getGstRate());
        po.setCgstAmount(dto.getCgstAmount());
        po.setSgstAmount(dto.getSgstAmount());
        po.setIgstAmount(dto.getIgstAmount());
        po.setTotalTaxAmount(totalTaxAmount);

        po.setGrandTotal(grandTotal);

        po.setScopeOfWork(dto.getScopeOfWork());
        po.setTermsAndConditions(dto.getTermsAndConditions());
        po.setRemarks(dto.getRemarks());
        po.setAttachmentUrls(dto.getAttachmentUrls());

        po.setStatus(ProcurementOrderStatus.DRAFT);
        po.setPoCreatedDate(currentDate);
        po.setVendorGSTRegistrationType(vendor.getGstRegistrationType());

        po.setCreatedBy(createdByUser.getId());
        po.setUpdatedBy(createdByUser.getId());
        po.setCreatedDate(currentDate);
        po.setUpdatedDate(currentDate);

        if (dto.getPaymentTypeName() != null
                && !dto.getPaymentTypeName().trim().isEmpty()) {

            PaymentType paymentType = paymentTypeRepository
                    .findByName(dto.getPaymentTypeName().trim())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment type not found: "
                                    + dto.getPaymentTypeName(),
                            "ERR_PAYMENT_TYPE_NOT_FOUND"
                    ));

            po.setPaymentType(paymentType);
        }

        ProcurementOrder savedPo =
                purchaseOrderRepository.save(po);

        procurement.setStatus(ProcurementStatus.PO_CREATED);
        procurement.setSelectedVendor(vendor);
        procurement.setPoCreatedDate(currentDate);
        procurement.setUpdatedBy(createdByUser.getId());
        procurement.setUpdatedDate(currentDate);

        procurementRepository.save(procurement);

        logger.info(
                "Purchase Order created successfully: {} | Project: {} | "
                        + "Vendor: {} | Final Amount: {} | TDS: {} | "
                        + "GST: {} | Grand Total: {}",
                poNumber,
                procurement.getProject() != null
                        ? procurement.getProject().getProjectNo()
                        : null,
                vendor.getName(),
                finalAmount,
                tdsAmount,
                totalTaxAmount,
                grandTotal
        );

        return mapToResponseDto(savedPo);
    }

    /*
     * Keeping method name because controller currently uses /release.
     * Internally this now means APPROVE PO.
     */
    @Override
    public PurchaseOrderResponseDto releasePurchaseOrder(Long poId, Long userId) {
        return approvePurchaseOrderInternal(poId, userId, null);
    }

    @Override
    public PurchaseOrderResponseDto getPurchaseOrderById(Long id) {

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

        return mapToResponseDto(po);
    }

    @Override
    public PurchaseOrderResponseDto getByProcurementAssignmentId(Long procurementAssignmentId) {

        if (procurementAssignmentId == null) {
            throw new ValidationException(
                    "Procurement Assignment ID is required",
                    "ERR_NULL_ID"
            );
        }

        ProcurementOrder po = purchaseOrderRepository.findByProcurementAssignmentId(procurementAssignmentId)
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

        return mapToResponseDto(po);
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
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Page<ProcurementOrder> purchaseOrders =
                purchaseOrderRepository.findByProjectIdAndIsDeletedFalse(projectId, pageable);

        return purchaseOrders.map(this::mapToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementOrderResponseDto> getProcurementOrdersByStatus(
            ProcurementOrderStatus status,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Page<ProcurementOrder> orders;

        if (status == null) {
            orders = purchaseOrderRepository.findByIsDeletedFalse(pageable);
        } else {
            orders = purchaseOrderRepository.findByStatusAndIsDeletedFalse(status, pageable);
        }

        return orders.map(this::mapToResponse);
    }

    /*
     * Kept only for compatibility if your interface/controller still calls this.
     * This now approves DRAFT PO directly.
     */
    @Override
    @Transactional
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

        return mapToResponse(order);
    }

    /*
     * Rejection flow removed.
     * Keep this only if PurchaseOrderService interface still declares it.
     */
    @Override
    @Transactional
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
    @Transactional
    public PurchaseOrderResponseDto updatePurchaseOrder(Long poId, PurchaseOrderRequestDto dto) {

        logger.info("Updating Purchase Order id: {}", poId);

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

        /*
         * New flow:
         * Only DRAFT PO can be edited.
         * APPROVED PO cannot be changed.
         */
        if (po.getStatus() != ProcurementOrderStatus.DRAFT) {
            throw new ValidationException(
                    "Only DRAFT Purchase Order can be updated. Current status: " + po.getStatus(),
                    "ERR_INVALID_PO_STATUS_FOR_UPDATE"
            );
        }

        if (dto.getFinalAmount() == null || dto.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "Final amount must be greater than zero",
                    "ERR_INVALID_AMOUNT"
            );
        }

        ProcurementMilestoneAssignment procurementForValidation = po.getProcurementAssignment();

        if (dto.getProcurementAssignmentId() != null) {
            procurementForValidation = procurementRepository
                    .findById(dto.getProcurementAssignmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Procurement assignment not found",
                            "ERR_PROCUREMENT_NOT_FOUND"
                    ));
        }

        validatePoValueNotGreaterThanProjectValue(
                dto.getGrandTotal(),
                dto.getFinalAmount(),
                procurementForValidation
        );

        if (dto.getProcurementAssignmentId() != null) {
            po.setProcurementAssignment(procurementForValidation);
            po.setProject(procurementForValidation.getProject());
        }

        if (dto.getVendorId() != null) {
            Vendor vendor = vendorRepository.findByIdAndIsDeletedFalse(dto.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Vendor not found",
                            "ERR_VENDOR_NOT_FOUND"
                    ));

            po.setVendor(vendor);
        }

        po.setPoReferenceNumber(dto.getPoReferenceNumber());

        po.setFinalAmount(dto.getFinalAmount());

        po.setGstRate(dto.getGstRate());
        po.setCgstAmount(dto.getCgstAmount());
        po.setSgstAmount(dto.getSgstAmount());
        po.setIgstAmount(dto.getIgstAmount());
        po.setTotalTaxAmount(dto.getTotalTaxAmount());
        po.setGrandTotal(dto.getGrandTotal());

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
            PaymentType paymentType = paymentTypeRepository.findByName(dto.getPaymentTypeName())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment type not found: " + dto.getPaymentTypeName(),
                            "ERR_PAYMENT_TYPE_NOT_FOUND"
                    ));

            po.setPaymentType(paymentType);
        }

        po.setUpdatedDate(new Date());

        ProcurementOrder savedPo = purchaseOrderRepository.save(po);

        logger.info("Purchase Order updated successfully: {}", savedPo.getPoNumber());

        return mapToResponseDto(savedPo);
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDto updatePurchaseOrderStatus(
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

        if (newStatus != ProcurementOrderStatus.APPROVED) {
            throw new ValidationException(
                    "Only APPROVED status update is allowed. Purchase Order supports only DRAFT and APPROVED.",
                    "ERR_INVALID_PO_STATUS"
            );
        }

        return approvePurchaseOrderInternal(poId, userId, remarks);
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

        po.setStatus(ProcurementOrderStatus.APPROVED);
        po.setApprovedBy(approvedByUser.getId());
        po.setPoApprovedDate(new Date());
        po.setUpdatedBy(approvedByUser.getId());
        po.setUpdatedDate(new Date());

        if (remarks != null && !remarks.trim().isEmpty()) {
            po.setRemarks(remarks.trim());
        }

        ProcurementOrder savedPo = purchaseOrderRepository.save(po);

        ProcurementMilestoneAssignment procurement = savedPo.getProcurementAssignment();

        if (procurement != null) {
            procurement.setStatus(ProcurementStatus.PO_APPROVED);
            procurement.setUpdatedBy(approvedByUser.getId());
            procurement.setUpdatedDate(new Date());
        }

        logger.info(
                "Purchase Order approved successfully | poNumber={} | approvedBy={}",
                savedPo.getPoNumber(),
                approvedByUser.getId()
        );

        return mapToResponseDto(savedPo);
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

        BigDecimal projectValue = BigDecimal.valueOf(
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

    private ProcurementOrderResponseDto mapToResponse(ProcurementOrder order) {

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

    private String generatePoNumber() {
        int year = LocalDate.now().getYear();
        long count = purchaseOrderRepository.count() + 1;
        return String.format("CORP-PO-%d-%05d", year, count);
    }

    private PurchaseOrderResponseDto mapToResponseDto(ProcurementOrder po) {

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

        dto.setVendorId(
                po.getVendor() != null
                        ? po.getVendor().getId()
                        : null
        );

        dto.setVendorName(
                po.getVendor() != null
                        ? po.getVendor().getName()
                        : null
        );

        dto.setFinalAmount(po.getFinalAmount());

        dto.setGstRate(po.getGstRate());
        dto.setCgstAmount(po.getCgstAmount());
        dto.setSgstAmount(po.getSgstAmount());
        dto.setIgstAmount(po.getIgstAmount());
        dto.setTotalTaxAmount(po.getTotalTaxAmount());
        dto.setGrandTotal(po.getGrandTotal());

        dto.setScopeOfWork(po.getScopeOfWork());
        dto.setTermsAndConditions(po.getTermsAndConditions());
        dto.setRemarks(po.getRemarks());
        dto.setAttachmentUrls(po.getAttachmentUrls());

        dto.setStatus(po.getStatus());

        dto.setPoCreatedDate(po.getPoCreatedDate());
        dto.setPoApprovedDate(po.getPoApprovedDate());
        dto.setPoReleasedDate(po.getPoReleasedDate());

        dto.setTdsAmount(po.getTdsAmount());
        dto.setTdsPercentage(po.getTdsPercentage());

        dto.setVendorGSTRegistrationType(po.getVendorGSTRegistrationType());

        dto.setCreatedDate(po.getCreatedDate());
        dto.setUpdatedDate(po.getUpdatedDate());

        return dto;
    }
}