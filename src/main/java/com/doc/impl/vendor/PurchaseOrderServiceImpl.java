package com.doc.impl.vendor;

import com.doc.dto.vendor.ProcurementOrderResponseDto;
import com.doc.dto.vendor.PurchaseOrderRequestDto;
import com.doc.dto.vendor.PurchaseOrderResponseDto;
import com.doc.entity.client.PaymentType;
import com.doc.entity.vendor.ProcurementMilestoneAssignment;

import com.doc.entity.project.ProcurementStatus;
import com.doc.entity.user.User;
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
import java.time.LocalDate;
import java.util.Date;

@Service
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private ProcurementMilestoneAssignmentRepository procurementRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PaymentTypeRepository paymentTypeRepository;

    @Override
    public PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto dto) {
        logger.info("Creating Purchase Order for procurementAssignmentId: {}", dto.getProcurementAssignmentId());

        ProcurementMilestoneAssignment procurement = procurementRepository
                .findById(dto.getProcurementAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Procurement assignment not found", "ERR_PROCUREMENT_NOT_FOUND"));

        Vendor vendor = vendorRepository.findByIdAndIsDeletedFalse(dto.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found", "ERR_VENDOR_NOT_FOUND"));

        User createdByUser = userRepository.findActiveUserById(dto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("CreatedBy user not found", "ERR_USER_NOT_FOUND"));

        // Validation
        if (dto.getFinalAmount() == null || dto.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Final amount must be greater than zero", "ERR_INVALID_AMOUNT");
        }

        String poNumber = generatePoNumber();

        // Create ProcurementOrder
        ProcurementOrder po = new ProcurementOrder();
        po.setProcurementAssignment(procurement);
        po.setProject(procurement.getProject());
        po.setVendor(vendor);

        po.setPoNumber(poNumber);
        po.setPoReferenceNumber(dto.getPoReferenceNumber());

        po.setFinalAmount(dto.getFinalAmount());

        // GST Fields
        po.setGstRate(dto.getGstRate());
        po.setCgstAmount(dto.getCgstAmount());
        po.setSgstAmount(dto.getSgstAmount());
        po.setIgstAmount(dto.getIgstAmount());
        po.setTotalTaxAmount(dto.getTotalTaxAmount());
        po.setGrandTotal(dto.getGrandTotal());

        po.setScopeOfWork(dto.getScopeOfWork());
        po.setTermsAndConditions(dto.getTermsAndConditions());
        po.setRemarks(dto.getRemarks());
        po.setAttachmentUrls(dto.getAttachmentUrls());

        po.setStatus(ProcurementOrderStatus.PENDING_APPROVAL);
        po.setPoCreatedDate(new Date());

        po.setCreatedBy(createdByUser.getId());
        po.setCreatedDate(new Date());
        po.setUpdatedDate(new Date());

        // Payment Type
        if (dto.getPaymentTypeName() != null && !dto.getPaymentTypeName().trim().isEmpty()) {
            PaymentType paymentType = paymentTypeRepository.findByName(dto.getPaymentTypeName())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment type not found: " + dto.getPaymentTypeName(), "ERR_PAYMENT_TYPE_NOT_FOUND"));
            po.setPaymentType(paymentType);
        }

        po = purchaseOrderRepository.save(po);

        logger.info("Purchase Order created successfully: {} | Project: {} | Vendor: {}",
                poNumber, procurement.getProject().getProjectNo(), vendor.getName());

        return mapToResponseDto(po);
    }

    @Override
    public PurchaseOrderResponseDto releasePurchaseOrder(Long poId, Long userId) {
        ProcurementOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found", "ERR_PO_NOT_FOUND"));

        if (po.getStatus() != ProcurementOrderStatus.DRAFT && po.getStatus() != ProcurementOrderStatus.PENDING_APPROVAL) {
            throw new ValidationException("Only DRAFT or PENDING_APPROVAL PO can be released", "ERR_INVALID_PO_STATUS");
        }

        User approvedBy = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        po.setStatus(ProcurementOrderStatus.RELEASED);
        po.setApprovedBy(approvedBy.getId());
        po.setPoReleasedDate(new Date());
        po.setUpdatedBy(userId);
        po.setUpdatedDate(new Date());

        po = purchaseOrderRepository.save(po);

        // Optional: Update parent procurement status
        ProcurementMilestoneAssignment procurement = po.getProcurementAssignment();
        procurement.setStatus(ProcurementStatus.PO_RELEASED);
        procurement.setPoReleasedDate(new Date());

        logger.info("Purchase Order {} released successfully by user {}", po.getPoNumber(), userId);

        return mapToResponseDto(po);
    }

    @Override
    public PurchaseOrderResponseDto getPurchaseOrderById(Long id) {
        ProcurementOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found", "ERR_PO_NOT_FOUND"));
        return mapToResponseDto(po);
    }

    @Override
    public PurchaseOrderResponseDto getByProcurementAssignmentId(Long procurementAssignmentId) {
        if (procurementAssignmentId == null) {
            throw new ValidationException("Procurement Assignment ID is required", "ERR_NULL_ID");
        }

        ProcurementOrder po = purchaseOrderRepository.findByProcurementAssignmentId(procurementAssignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Purchase Order found for this procurement assignment", "ERR_PO_NOT_FOUND"));

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

    @Override
    @Transactional
    public ProcurementOrderResponseDto approveProcurementOrder(
            Long procurementOrderId,
            Long userId,
            String comment
    ) {
        ProcurementOrder order = purchaseOrderRepository.findById(procurementOrderId)
                .orElseThrow(() -> new RuntimeException(
                        "Procurement order not found with id: " + procurementOrderId
                ));

        if (order.isDeleted()) {
            throw new RuntimeException("Procurement order is deleted and cannot be approved");
        }

        if (order.getStatus() != ProcurementOrderStatus.PENDING_APPROVAL) {
            throw new RuntimeException(
                    "Only PENDING_APPROVAL procurement orders can be approved. Current status: "
                            + order.getStatus()
            );
        }

        order.setStatus(ProcurementOrderStatus.APPROVED);
        order.setApprovedBy(userId);
        order.setPoApprovedDate(new Date());
        order.setUpdatedBy(userId);
        order.setUpdatedDate(new Date());

        if (comment != null && !comment.trim().isEmpty()) {
            order.setRemarks(comment.trim());
        }

        ProcurementOrder savedOrder = purchaseOrderRepository.save(order);

        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional
    public ProcurementOrderResponseDto rejectProcurementOrder(
            Long procurementOrderId,
            Long userId,
            String reason
    ) {
        ProcurementOrder order = purchaseOrderRepository.findById(procurementOrderId)
                .orElseThrow(() -> new RuntimeException(
                        "Procurement order not found with id: " + procurementOrderId
                ));

        if (order.isDeleted()) {
            throw new RuntimeException("Procurement order is deleted and cannot be rejected");
        }

        if (order.getStatus() != ProcurementOrderStatus.PENDING_APPROVAL) {
            throw new RuntimeException(
                    "Only PENDING_APPROVAL procurement orders can be rejected. Current status: "
                            + order.getStatus()
            );
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("Rejection reason is required");
        }

        order.setStatus(ProcurementOrderStatus.REJECTED);
        order.setUpdatedBy(userId);
        order.setUpdatedDate(new Date());
        order.setRemarks(reason.trim());

        ProcurementOrder savedOrder = purchaseOrderRepository.save(order);

        return mapToResponse(savedOrder);
    }


    @Override
    @Transactional
    public PurchaseOrderResponseDto updatePurchaseOrder(Long poId, PurchaseOrderRequestDto dto) {

        logger.info("Updating Purchase Order id: {}", poId);

        if (poId == null) {
            throw new ValidationException("Purchase Order ID is required", "ERR_PO_ID_REQUIRED");
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
         * Safe workflow:
         * Only DRAFT or REJECTED PO can be edited.
         * APPROVED / RELEASED / COMPLETED should not be edited because accounts/vendor flow may already depend on it.
         */
        if (po.getStatus() != ProcurementOrderStatus.DRAFT
                && po.getStatus() != ProcurementOrderStatus.REJECTED) {
            throw new ValidationException(
                    "Only DRAFT or REJECTED Purchase Order can be updated. Current status: " + po.getStatus(),
                    "ERR_INVALID_PO_STATUS_FOR_UPDATE"
            );
        }

        if (dto.getFinalAmount() == null || dto.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "Final amount must be greater than zero",
                    "ERR_INVALID_AMOUNT"
            );
        }

        // Optional: update procurement assignment only if sent
        if (dto.getProcurementAssignmentId() != null) {
            ProcurementMilestoneAssignment procurement = procurementRepository
                    .findById(dto.getProcurementAssignmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Procurement assignment not found",
                            "ERR_PROCUREMENT_NOT_FOUND"
                    ));

            po.setProcurementAssignment(procurement);
            po.setProject(procurement.getProject());
        }

        // Optional: update vendor only if sent
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

        if (dto.getUserId() != null) {
            User updatedByUser = userRepository.findActiveUserById(dto.getUserId())
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
        logger.info("Updating Purchase Order status | poId={}, newStatus={}, userId={}",
                poId, newStatus, userId);

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
                    "Deleted Purchase Order status cannot be updated",
                    "ERR_DELETED_PO_STATUS_CANNOT_BE_UPDATED"
            );
        }

        User updatedByUser = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        ProcurementOrderStatus currentStatus = po.getStatus();

        validatePurchaseOrderStatusTransition(currentStatus, newStatus);

        po.setStatus(newStatus);
        po.setUpdatedBy(updatedByUser.getId());
        po.setUpdatedDate(new Date());

        if (remarks != null && !remarks.trim().isEmpty()) {
            po.setRemarks(remarks.trim());
        }

        ProcurementOrder savedPo = purchaseOrderRepository.save(po);

        logger.info("Purchase Order status updated successfully | poNumber={} | oldStatus={} | newStatus={}",
                savedPo.getPoNumber(), currentStatus, newStatus);

        return mapToResponseDto(savedPo);
    }

    private void validatePurchaseOrderStatusTransition(
            ProcurementOrderStatus currentStatus,
            ProcurementOrderStatus newStatus
    ) {
        if (currentStatus == null) {
            throw new ValidationException(
                    "Current Purchase Order status is missing",
                    "ERR_CURRENT_PO_STATUS_MISSING"
            );
        }

        if (currentStatus == newStatus) {
            throw new ValidationException(
                    "Purchase Order is already in status: " + newStatus,
                    "ERR_PO_ALREADY_IN_SAME_STATUS"
            );
        }

        /*
         * Allowed transitions:
         * APPROVED  -> PARTIALLY_COMPLETED / COMPLETED
         * RELEASED  -> PARTIALLY_COMPLETED / COMPLETED
         * PARTIALLY_COMPLETED -> COMPLETED
         */
        boolean validTransition =
                (currentStatus == ProcurementOrderStatus.APPROVED
                        && (newStatus == ProcurementOrderStatus.PARTIALLY_COMPLETED
                        || newStatus == ProcurementOrderStatus.COMPLETED))

                        || (currentStatus == ProcurementOrderStatus.RELEASED
                        && (newStatus == ProcurementOrderStatus.PARTIALLY_COMPLETED
                        || newStatus == ProcurementOrderStatus.COMPLETED))

                        || (currentStatus == ProcurementOrderStatus.PARTIALLY_COMPLETED
                        && newStatus == ProcurementOrderStatus.COMPLETED);

        if (!validTransition) {
            throw new ValidationException(
                    "Invalid Purchase Order status change from "
                            + currentStatus
                            + " to "
                            + newStatus
                            + ". Allowed transitions are: APPROVED/RELEASED -> PARTIALLY_COMPLETED, APPROVED/RELEASED -> COMPLETED, PARTIALLY_COMPLETED -> COMPLETED",
                    "ERR_INVALID_PO_STATUS_TRANSITION"
            );
        }
    }

    private ProcurementOrderResponseDto mapToResponse(ProcurementOrder order) {
        return ProcurementOrderResponseDto.builder()
                .id(order.getId())

                .procurementAssignmentId(
                        order.getProcurementAssignment() != null
                                ? order.getProcurementAssignment().getId()
                                : null
                )

                .projectId(
                        order.getProject() != null
                                ? order.getProject().getId()
                                : null
                )
                .projectName(
                        order.getProject() != null
                                ? order.getProject().getName()
                                : null
                )

                .vendorId(
                        order.getVendor() != null
                                ? order.getVendor().getId()
                                : null
                )
                .vendorName(
                        order.getVendor() != null
                                ? order.getVendor().getName()
                                : null
                )

                .vendorContactId(
                        order.getVendorContact() != null
                                ? order.getVendorContact().getId()
                                : null
                )
                .vendorContactName(
                        order.getVendorContact() != null
                                ? order.getVendorContact().getName()
                                : null
                )

                .poNumber(order.getPoNumber())
                .poReferenceNumber(order.getPoReferenceNumber())
                .finalAmount(order.getFinalAmount())
                .gstRate(order.getGstRate())

                .cgstAmount(order.getCgstAmount())
                .sgstAmount(order.getSgstAmount())
                .igstAmount(order.getIgstAmount())

                .totalTaxAmount(order.getTotalTaxAmount())
                .grandTotal(order.getGrandTotal())

                .scopeOfWork(order.getScopeOfWork())
                .termsAndConditions(order.getTermsAndConditions())
                .remarks(order.getRemarks())

                .attachmentUrls(order.getAttachmentUrls())

                .status(order.getStatus())

                .poCreatedDate(order.getPoCreatedDate())
                .poSubmittedForApprovalDate(order.getPoSubmittedForApprovalDate())
                .poApprovedDate(order.getPoApprovedDate())
                .poReleasedDate(order.getPoReleasedDate())

                .paymentTypeId(
                        order.getPaymentType() != null
                                ? order.getPaymentType().getId()
                                : null
                )
                .paymentTypeName(
                        order.getPaymentType() != null
                                ? order.getPaymentType().getName()
                                : null
                )

                .createdBy(order.getCreatedBy())
                .updatedBy(order.getUpdatedBy())
                .approvedBy(order.getApprovedBy())

                .createdDate(order.getCreatedDate())
                .updatedDate(order.getUpdatedDate())

                .build();
    }

    // ==================== Helper Methods ====================

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

        dto.setProcurementAssignmentId(po.getProcurementAssignment().getId());
        dto.setProjectId(po.getProject() != null ? po.getProject().getId() : null);
        dto.setProjectName(po.getProject() != null ? po.getProject().getName() : null);
        dto.setProjectNo(po.getProject() != null ? po.getProject().getProjectNo() : null);

        dto.setVendorId(po.getVendor().getId());
        dto.setVendorName(po.getVendor().getName());
        dto.setFinalAmount(po.getFinalAmount());
        dto.setCgstAmount(po.getCgstAmount());
        dto.setSgstAmount(po.getSgstAmount());
        dto.setIgstAmount(po.getIgstAmount());
        dto.setTotalTaxAmount(po.getTotalTaxAmount());
        dto.setGrandTotal(po.getGrandTotal());
        dto.setGstRate(po.getGstRate());

        dto.setScopeOfWork(po.getScopeOfWork());
        dto.setTermsAndConditions(po.getTermsAndConditions());
        dto.setRemarks(po.getRemarks());
        dto.setAttachmentUrls(po.getAttachmentUrls());

        dto.setStatus(po.getStatus());

        dto.setPoCreatedDate(po.getPoCreatedDate());
        dto.setPoApprovedDate(po.getPoApprovedDate());
        dto.setPoReleasedDate(po.getPoReleasedDate());

        dto.setCreatedDate(po.getCreatedDate());
        dto.setUpdatedDate(po.getUpdatedDate());

        return dto;
    }
}