package com.doc.impl.vendor;

import com.doc.dto.vendor.PurchaseOrderRequestDto;
import com.doc.dto.vendor.PurchaseOrderResponseDto;
import com.doc.entity.client.PaymentType;
import com.doc.entity.project.ProcurementMilestoneAssignment;

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

        po.setEstimatedAmount(dto.getEstimatedAmount());
        po.setFinalAmount(dto.getFinalAmount());

        // GST Fields
        po.setGstRate(dto.getGstRate());
        po.setCgstAmount(dto.getCgstAmount());
        po.setSgstAmount(dto.getSgstAmount());
        po.setIgstAmount(dto.getIgstAmount());
        po.setTotalTaxAmount(dto.getTotalTaxAmount());
        po.setGrandTotal(dto.getGrandTotal());

        po.setScopeOfWork(dto.getScopeOfWork());
        po.setPaymentTerms(dto.getPaymentTerms());
        po.setTermsAndConditions(dto.getTermsAndConditions());
        po.setRemarks(dto.getRemarks());
        po.setAttachmentUrls(dto.getAttachmentUrls());

        po.setStatus(ProcurementOrderStatus.DRAFT);
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

    // ==================== Helper Methods ====================

    private String generatePoNumber() {
        int year = LocalDate.now().getYear();
        long count = purchaseOrderRepository.count() + 1;
        return String.format("PO-%d-%05d", year, count);
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

        dto.setEstimatedAmount(po.getEstimatedAmount());
        dto.setFinalAmount(po.getFinalAmount());
        dto.setCgstAmount(po.getCgstAmount());
        dto.setSgstAmount(po.getSgstAmount());
        dto.setIgstAmount(po.getIgstAmount());
        dto.setTotalTaxAmount(po.getTotalTaxAmount());
        dto.setGrandTotal(po.getGrandTotal());
        dto.setGstRate(po.getGstRate());

        dto.setScopeOfWork(po.getScopeOfWork());
        dto.setPaymentTerms(po.getPaymentTerms());
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