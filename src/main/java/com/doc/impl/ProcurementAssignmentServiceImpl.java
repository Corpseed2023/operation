package com.doc.impl;

import com.doc.dto.procurement.ProcurementAssignmentResponseDto;
import com.doc.dto.procurement.SelectProcurementVendorRequestDto;
import com.doc.dto.procurement.VendorSummaryDto;
import com.doc.dto.vendor.request.SelectVendorQuotationRequestDto;
import com.doc.entity.product.Product;
import com.doc.entity.vendor.ProcurementMilestoneAssignment;
import com.doc.entity.project.ProcurementStatus;
import com.doc.entity.user.User;
import com.doc.entity.vendor.Vendor;
import com.doc.entity.vendor.VendorStatus;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProcurementMilestoneAssignmentRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.vendor.VendorRepository;
import com.doc.service.ProcurementAssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.doc.entity.vendor.ProcurementVendorQuotation;
import com.doc.repository.vendor.ProcurementVendorQuotationRepository;


import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ProcurementAssignmentServiceImpl implements ProcurementAssignmentService {

    private final ProcurementMilestoneAssignmentRepository procurementMilestoneAssignmentRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final ProcurementVendorQuotationRepository procurementVendorQuotationRepository;

    public ProcurementAssignmentServiceImpl(
            ProcurementMilestoneAssignmentRepository procurementMilestoneAssignmentRepository,
            VendorRepository vendorRepository,
            UserRepository userRepository,
            ProcurementVendorQuotationRepository procurementVendorQuotationRepository
    ) {
        this.procurementMilestoneAssignmentRepository = procurementMilestoneAssignmentRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.procurementVendorQuotationRepository = procurementVendorQuotationRepository;
    }

    @Override
    public ProcurementAssignmentResponseDto getProcurementAssignment(Long procurementAssignmentId) {

        System.out.println("========== GET PROCUREMENT ASSIGNMENT API CALLED ==========");
        System.out.println("Requested Procurement Assignment ID: " + procurementAssignmentId);

        ProcurementMilestoneAssignment assignment = procurementMilestoneAssignmentRepository
                .findByIdAndIsDeletedFalse(procurementAssignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Procurement assignment not found",
                        "ERR_PROCUREMENT_ASSIGNMENT_NOT_FOUND"
                ));

        System.out.println("Assignment Found ID: " + assignment.getId());
        System.out.println("Assignment Status: " + assignment.getStatus());

        if (assignment.getProject() != null) {
            System.out.println("Project ID: " + assignment.getProject().getId());
            System.out.println("Project Name: " + assignment.getProject().getName());
        } else {
            System.out.println("Project is NULL");
        }

        if (assignment.getSelectedVendor() != null) {
            System.out.println("Selected Vendor ID: " + assignment.getSelectedVendor().getId());
            System.out.println("Selected Vendor Name: " + assignment.getSelectedVendor().getName());
        } else {
            System.out.println("Selected Vendor is NULL after DB fetch");
        }

        System.out.println("===========================================================");

        return buildResponseAndRefreshVendorStatus(assignment);
    }

    @Override
    public ProcurementAssignmentResponseDto getProcurementAssignmentByProject(Long projectId) {

        ProcurementMilestoneAssignment assignment = procurementMilestoneAssignmentRepository
                .findByProjectIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Procurement assignment not found for this project",
                        "ERR_PROCUREMENT_ASSIGNMENT_NOT_FOUND"
                ));

        return buildResponseAndRefreshVendorStatus(assignment);
    }

    @Override
    public List<ProcurementAssignmentResponseDto> getProcurementAssignmentsByUser(Long userId) {

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        List<ProcurementMilestoneAssignment> assignments =
                procurementMilestoneAssignmentRepository.findByAssignedToIdAndIsDeletedFalse(userId);

        return assignments.stream()
                .map(this::buildResponseAndRefreshVendorStatus)
                .toList();
    }

    @Override
    public List<ProcurementAssignmentResponseDto> getVendorRequiredAssignments() {

        List<ProcurementMilestoneAssignment> assignments =
                procurementMilestoneAssignmentRepository.findByStatusAndIsDeletedFalse(
                        ProcurementStatus.VENDOR_REQUIRED
                );

        return assignments.stream()
                .map(this::buildResponseAndRefreshVendorStatus)
                .toList();
    }

    @Override
    public ProcurementAssignmentResponseDto selectVendor(
            Long procurementAssignmentId,
            SelectProcurementVendorRequestDto requestDto) {

        ProcurementMilestoneAssignment assignment = findAssignmentById(procurementAssignmentId);

        User user = userRepository.findActiveUserById(requestDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        Vendor vendor = vendorRepository.findByIdAndIsDeletedFalse(requestDto.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found", "ERR_VENDOR_NOT_FOUND"));

        // Validations - simplified (no more product mapping)
        if (vendor.getStatus() != VendorStatus.ACTIVE) {
            throw new ValidationException("Only ACTIVE vendor can be selected", "ERR_VENDOR_NOT_ACTIVE");
        }

        // Prevent re-selection
        if (assignment.getSelectedVendor() != null) {
            throw new ValidationException("Vendor is already selected for this assignment", "ERR_VENDOR_ALREADY_SELECTED");
        }

        // Link Vendor
        assignment.setSelectedVendor(vendor);
        assignment.setStatus(ProcurementStatus.VENDOR_FINALIZED);
        assignment.setUpdatedBy(user.getId());
        assignment.setUpdatedDate(new Date());

        procurementMilestoneAssignmentRepository.save(assignment);

        return buildResponseAndRefreshVendorStatus(assignment);
    }

    private ProcurementMilestoneAssignment findAssignmentById(Long id) {
        return procurementMilestoneAssignmentRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Procurement assignment not found",
                        "ERR_PROCUREMENT_ASSIGNMENT_NOT_FOUND"
                ));
    }



    private ProcurementAssignmentResponseDto buildResponseAndRefreshVendorStatus(
            ProcurementMilestoneAssignment assignment) {

        List<Vendor> eligibleVendors;

        //  If vendor is already selected → show only the selected vendor
        if (assignment.getSelectedVendor() != null) {
            eligibleVendors = List.of(assignment.getSelectedVendor());
        }
        // If no vendor selected → show all active vendors
        else {
            eligibleVendors = vendorRepository.findAllByStatusAndIsDeletedFalse(
                    VendorStatus.ACTIVE, false);
        }

        boolean vendorAvailable = !eligibleVendors.isEmpty();

        // Auto-refresh status only when no vendor is selected yet
        if (assignment.getSelectedVendor() == null) {
            ProcurementStatus newStatus = vendorAvailable
                    ? ProcurementStatus.VENDOR_SHORTLISTED
                    : ProcurementStatus.VENDOR_REQUIRED;

            if (assignment.getStatus() != newStatus) {
                assignment.setStatus(newStatus);
                assignment.setVendorShortlistedDate(vendorAvailable ? new Date() : null);
                assignment.setUpdatedDate(new Date());
                procurementMilestoneAssignmentRepository.save(assignment);
            }
        }

        ProcurementAssignmentResponseDto dto = new ProcurementAssignmentResponseDto();

        dto.setProcurementAssignmentId(assignment.getId());

        dto.setProjectId(assignment.getProject() != null ? assignment.getProject().getId() : null);
        dto.setProjectName(assignment.getProject() != null ? assignment.getProject().getName() : null);
        dto.setProjectNo(assignment.getProject() != null ? assignment.getProject().getProjectNo() : null);

        Product product = assignment.getProject() != null ? assignment.getProject().getProduct() : null;
        dto.setProductId(product != null ? product.getId() : null);
        dto.setProductName(product != null ? product.getProductName() : null);

        dto.setMilestoneId(assignment.getMilestone() != null ? assignment.getMilestone().getId() : null);
        dto.setMilestoneName(assignment.getMilestone() != null ? assignment.getMilestone().getName() : null);

        dto.setAssignedToUserId(assignment.getAssignedTo() != null ? assignment.getAssignedTo().getId() : null);
        dto.setAssignedToUserName(assignment.getAssignedTo() != null ? assignment.getAssignedTo().getFullName() : null);

        dto.setSelectedVendorId(assignment.getSelectedVendor() != null ? assignment.getSelectedVendor().getId() : null);
        dto.setSelectedVendorName(assignment.getSelectedVendor() != null ? assignment.getSelectedVendor().getName() : null);

        dto.setStatus(assignment.getStatus());
        dto.setVendorAvailable(vendorAvailable);

        if (assignment.getSelectedVendor() != null) {
            dto.setActionRequired(null);
            dto.setMessage("Vendor selected successfully for this procurement milestone.");
        } else if (vendorAvailable) {
            dto.setActionRequired("SELECT_VENDOR");
            dto.setMessage("Vendors are available. Please select one.");
        } else {
            dto.setActionRequired("CREATE_VENDOR");
            dto.setMessage("No vendor available. Please create a vendor.");
        }

        // Map eligible vendors
        dto.setEligibleVendors(
                eligibleVendors.stream()
                        .map(this::mapVendorSummary)
                        .toList()
        );

        dto.setVendorShortlistedDate(assignment.getVendorShortlistedDate());
        dto.setPoCreatedDate(assignment.getPoCreatedDate());
        dto.setPoReleasedDate(assignment.getPoReleasedDate());
        dto.setCreatedDate(assignment.getCreatedDate());
        dto.setUpdatedDate(assignment.getUpdatedDate());

        return dto;
    }


    @Override
    public ProcurementAssignmentResponseDto selectVendorQuotation(
            Long procurementAssignmentId,
            SelectVendorQuotationRequestDto requestDto) {

        ProcurementMilestoneAssignment assignment = findAssignmentById(procurementAssignmentId);

        User user = userRepository.findActiveUserById(requestDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        ProcurementVendorQuotation selectedQuotation =
                procurementVendorQuotationRepository.findByIdAndIsDeletedFalse(requestDto.getQuotationId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Vendor quotation not found",
                                "ERR_VENDOR_QUOTATION_NOT_FOUND"
                        ));

        if (!selectedQuotation.getProcurementAssignment().getId().equals(procurementAssignmentId)) {
            throw new ValidationException(
                    "Quotation does not belong to this procurement assignment",
                    "ERR_INVALID_QUOTATION_ASSIGNMENT"
            );
        }

        if (assignment.getSelectedVendor() != null) {
            throw new ValidationException(
                    "Vendor is already selected for this assignment",
                    "ERR_VENDOR_ALREADY_SELECTED"
            );
        }

        List<ProcurementVendorQuotation> quotations =
                procurementVendorQuotationRepository
                        .findByProcurementAssignmentIdAndIsDeletedFalse(procurementAssignmentId);

        for (ProcurementVendorQuotation quotation : quotations) {
            quotation.setSelected(false);
            quotation.setUpdatedBy(user.getId());
            quotation.setUpdatedDate(new Date());
        }

        selectedQuotation.setSelected(true);
        selectedQuotation.setUpdatedBy(user.getId());
        selectedQuotation.setUpdatedDate(new Date());

        procurementVendorQuotationRepository.saveAll(quotations);

        Vendor selectedVendor = selectedQuotation.getVendor();

        assignment.setSelectedVendor(selectedVendor);
        assignment.setStatus(ProcurementStatus.VENDOR_FINALIZED);
        assignment.setUpdatedBy(user.getId());
        assignment.setUpdatedDate(new Date());

        procurementMilestoneAssignmentRepository.save(assignment);

        return buildResponseAndRefreshVendorStatus(assignment);
    }


    private VendorSummaryDto mapVendorSummary(Vendor vendor) {
        VendorSummaryDto dto = new VendorSummaryDto();
        dto.setId(vendor.getId());
        dto.setName(vendor.getName());
        dto.setEmail(vendor.getEmail());
        dto.setMobile(vendor.getMobile());
        dto.setGstNumber(vendor.getGstNumber());
        dto.setPanNumber(vendor.getPanNumber());
        dto.setStatus(vendor.getStatus());
        return dto;
    }
}