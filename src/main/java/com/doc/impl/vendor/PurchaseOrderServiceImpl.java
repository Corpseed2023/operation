package com.doc.impl.vendor;

import com.doc.dto.vendor.PurchaseOrderRequestDto;
import com.doc.dto.vendor.PurchaseOrderResponseDto;
import com.doc.entity.client.PaymentType;
import com.doc.entity.project.ProcurementMilestoneAssignment;
import com.doc.entity.project.ProcurementStatus;
import com.doc.entity.user.User;
import com.doc.entity.vendor.POStatus;
import com.doc.entity.vendor.PurchaseOrder;
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

        ProcurementMilestoneAssignment procurement = procurementRepository.findById(dto.getProcurementAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Procurement assignment not found", "ERR_PROCUREMENT_NOT_FOUND"));

        Vendor vendor = vendorRepository.findByIdAndIsDeletedFalse(dto.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found", "ERR_VENDOR_NOT_FOUND"));

        User createdByUser = userRepository.findActiveUserById(dto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("CreatedBy user not found", "ERR_USER_NOT_FOUND"));

        // Validate vendor is assigned
        if (procurement.getSelectedVendor() == null ||
                !procurement.getSelectedVendor().getId().equals(vendor.getId())) {
            throw new ValidationException("Selected vendor does not match assigned vendor in procurement", "ERR_VENDOR_MISMATCH");
        }

        String poNumber = generatePoNumber();

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(poNumber);
        po.setProcurementAssignment(procurement);
        po.setProject(procurement.getProject());
        po.setVendor(vendor);
        po.setTotalAmount(dto.getTotalAmount());
        po.setGstAmount(dto.getGstAmount() != null ? dto.getGstAmount() : BigDecimal.ZERO);
        po.setGrandTotal(po.getTotalAmount().add(po.getGstAmount()));
        po.setScopeOfWork(dto.getScopeOfWork());
        po.setIssueDate(LocalDate.now());
        po.setValidTillDate(dto.getValidTillDate());
        po.setStatus(POStatus.DRAFT);
        po.setTermsAndConditions(dto.getTermsAndConditions());
        po.setCreatedBy(createdByUser.getId());
        po.setCreatedDate(new Date());
        po.setUpdatedDate(new Date());

        if (dto.getPaymentTypeName() != null) {
            PaymentType paymentType = paymentTypeRepository.findByName(dto.getPaymentTypeName())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment type not found: " + dto.getPaymentTypeName(), "ERR_PAYMENT_TYPE_NOT_FOUND"));
            po.setPaymentType(paymentType);
        }

        po = purchaseOrderRepository.save(po);

        logger.info("Purchase Order created successfully: {}", poNumber);
        return mapToResponseDto(po);
    }

    @Override
    public PurchaseOrderResponseDto releasePurchaseOrder(Long poId, Long userId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found", "ERR_PO_NOT_FOUND"));

        if (po.getStatus() != POStatus.DRAFT) {
            throw new ValidationException("Only DRAFT PO can be released", "ERR_INVALID_PO_STATUS");
        }

        User approvedBy = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        po.setStatus(POStatus.RELEASED);
        po.setApprovedBy(approvedBy);
        po.setApprovedDate(new Date());
        po.setUpdatedBy(userId);
        po.setUpdatedDate(new Date());

        po = purchaseOrderRepository.save(po);

        // Update Procurement Milestone Status
        ProcurementMilestoneAssignment procurement = po.getProcurementAssignment();
        procurement.setStatus(ProcurementStatus.PO_RELEASED);
        procurement.setPoReleasedDate(new Date());
        // procurementRepository.save(procurement);   // Uncomment if you want to save immediately

        logger.info("Purchase Order {} released successfully by user {}", po.getPoNumber(), userId);

        return mapToResponseDto(po);
    }

    @Override
    public PurchaseOrderResponseDto getPurchaseOrderById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found", "ERR_PO_NOT_FOUND"));
        return mapToResponseDto(po);
    }

    @Override
    public PurchaseOrderResponseDto getByProcurementAssignmentId(Long procurementAssignmentId) {
        if (procurementAssignmentId == null) {
            throw new ValidationException("Procurement Assignment ID is required", "ERR_NULL_ID");
        }

        PurchaseOrder po = purchaseOrderRepository.findByProcurementAssignmentId(procurementAssignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Purchase Order found for procurement assignment ID: " + procurementAssignmentId,
                        "ERR_PO_NOT_FOUND"));

        return mapToResponseDto(po);
    }



    private String generatePoNumber() {
        int year = LocalDate.now().getYear();
        long count = purchaseOrderRepository.count() + 1;
        return String.format("PO-%d-%05d", year, count);
    }

    // ==================== Mapping ====================
    private PurchaseOrderResponseDto mapToResponseDto(PurchaseOrder po) {
        PurchaseOrderResponseDto dto = new PurchaseOrderResponseDto();

        dto.setId(po.getId());
        dto.setPoNumber(po.getPoNumber());
        dto.setProcurementAssignmentId(po.getProcurementAssignment().getId());
        dto.setProjectId(po.getProject() != null ? po.getProject().getId() : null);
        dto.setVendorId(po.getVendor().getId());
        dto.setVendorName(po.getVendor().getName());
        dto.setTotalAmount(po.getTotalAmount());
        dto.setGstAmount(po.getGstAmount());
        dto.setGrandTotal(po.getGrandTotal());
        dto.setScopeOfWork(po.getScopeOfWork());
        dto.setIssueDate(po.getIssueDate());
        dto.setValidTillDate(po.getValidTillDate());
        dto.setStatus(po.getStatus());
        dto.setPaymentTypeName(po.getPaymentType() != null ? po.getPaymentType().getName() : null);
        dto.setTermsAndConditions(po.getTermsAndConditions());
        dto.setCreatedDate(po.getCreatedDate());
        dto.setApprovedDate(po.getApprovedDate());

        return dto;
    }
}