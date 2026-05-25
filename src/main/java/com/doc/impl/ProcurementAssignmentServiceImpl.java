package com.doc.impl;

import com.doc.dto.procurement.ProcurementAssignmentResponseDto;
import com.doc.dto.procurement.SelectProcurementVendorRequestDto;
import com.doc.dto.procurement.VendorSummaryDto;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ProcurementAssignmentServiceImpl implements ProcurementAssignmentService {

    @Autowired
    private ProcurementMilestoneAssignmentRepository procurementMilestoneAssignmentRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ProcurementAssignmentResponseDto getProcurementAssignment(Long procurementAssignmentId) {

        ProcurementMilestoneAssignment assignment = procurementMilestoneAssignmentRepository
                .findByIdAndIsDeletedFalse(procurementAssignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Procurement assignment not found",
                        "ERR_PROCUREMENT_ASSIGNMENT_NOT_FOUND"
                ));

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

        // Validations
        if (vendor.getStatus() != VendorStatus.ACTIVE) {
            throw new ValidationException("Only ACTIVE vendor can be selected", "ERR_VENDOR_NOT_ACTIVE");
        }

        validateVendorForProjectProduct(assignment, vendor);

        // Prevent re-selection if already finalized
        if (assignment.getSelectedVendor() != null) {
            throw new ValidationException("Vendor is already selected for this assignment", "ERR_VENDOR_ALREADY_SELECTED");
        }

        // === Link Vendor to Project ===
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

    private void validateVendorForProjectProduct(ProcurementMilestoneAssignment assignment, Vendor vendor) {
        Product product = assignment.getProject().getProduct();
        if (product == null) {
            throw new ValidationException("Project does not have any service/product mapped",
                    "ERR_PROJECT_PRODUCT_NOT_FOUND");
        }

        List<Vendor> eligibleVendors = vendorRepository.findVendorsByProductId(product.getId());
        boolean isEligible = eligibleVendors.stream()
                .anyMatch(v -> v.getId().equals(vendor.getId()));

        if (!isEligible) {
            throw new ValidationException(
                    "This vendor is not mapped with the required service. Please select a valid vendor.",
                    "ERR_VENDOR_NOT_MAPPED_WITH_SERVICE"
            );
        }
    }



    private ProcurementAssignmentResponseDto buildResponseAndRefreshVendorStatus(
            ProcurementMilestoneAssignment assignment
    ) {

        Product product = assignment.getProject().getProduct();

        List<Vendor> eligibleVendors = List.of();

        if (product != null && product.getId() != null) {
            eligibleVendors = vendorRepository.findVendorsByProductId(product.getId());
        }

        boolean vendorAvailable = !eligibleVendors.isEmpty();

        /*
         * Auto-refresh only when vendor is not selected.
         * Example:
         * Earlier status was VENDOR_REQUIRED.
         * Later someone created vendor and mapped it with this product.
         * On next fetch, status becomes VENDOR_SHORTLISTED.
         */
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
            dto.setMessage("Vendors are available for this service. Please select one vendor.");
        } else {
            dto.setActionRequired("CREATE_VENDOR");
            dto.setMessage("No vendor available for this service. Please create vendor and map it with this service.");
        }

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

    private VendorSummaryDto mapVendorSummary(Vendor vendor) {

        VendorSummaryDto dto = new VendorSummaryDto();

        dto.setId(vendor.getId());
        dto.setName(vendor.getName());
        dto.setEmail(vendor.getEmail());
        dto.setMobile(vendor.getMobile());
        dto.setGstNumber(vendor.getGstNumber());
        dto.setPanNumber(vendor.getPanNumber());
        dto.setStatus(vendor.getStatus());
        dto.setVerified(vendor.isVerified());

        return dto;
    }
}