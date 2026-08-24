package com.doc.impl.project;

import com.doc.constants.StatusConstants;
import com.doc.dto.ProjectMilestoneassignment.ReassignMilestoneDto;
import com.doc.dto.ProjectMilestoneassignment.ReassignMilestoneResponseDto;
import com.doc.dto.ProjectMilestoneassignment.SendBackToPreviousMilestoneDto;
import com.doc.dto.ProjectMilestoneassignment.UpdateMilestoneStatusDto;
import com.doc.entity.document.DocumentStatus;
import com.doc.entity.milestone.MilestoneStatus;
import com.doc.entity.milestone.MilestoneStatusHistory;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.project.*;
import com.doc.entity.department.Department;
import com.doc.entity.user.User;
import com.doc.entity.user.UserProductMap;
import com.doc.entity.vendor.ProcurementMilestoneAssignment;
import com.doc.entity.vendor.ProcurementOrder;
import com.doc.entity.vendor.ProcurementOrderStatus;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.notification.*;
import com.doc.repository.*;
import com.doc.repository.documentRepo.ProjectDocumentUploadRepository;
import com.doc.repository.projectRepo.ProjectStatusRepository;
import com.doc.repository.vendor.ProcurementPaymentRequestRepository;
import com.doc.repository.vendor.PurchaseOrderRepository;
import com.doc.service.*;
//import com.doc.service.NotificationPublisherService;
import com.doc.validation.MilestoneValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.doc.repository.documentRepo.DocumentStatusRepository;
import com.doc.entity.document.ProjectDocumentUpload;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProjectMilestoneAssignmentServiceImpl implements ProjectMilestoneAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectMilestoneAssignmentServiceImpl.class);

    private final ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;
    private final UserRepository userRepository;
    private final ProjectDocumentUploadRepository projectDocumentUploadRepository;
    private final MilestoneStatusHistoryRepository milestoneStatusHistoryRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAssignmentHistoryRepository projectAssignmentHistoryRepository;
    private final UserProductMapRepository userProductMapRepository;
    private final UserPerformanceCountRepository userPerformanceCountRepository;
    private final MilestoneStatusRepository milestoneStatusRepository;
    private final ProjectStatusRepository projectStatusRepository;
    private final AutoAssignmentService autoAssignmentService;
    private final MilestoneValidator milestoneValidator;
    private final ProjectService projectService;
    private final ProcurementMilestoneAssignmentRepository procurementMilestoneAssignmentRepository;
    private final NotificationPublisherService notificationPublisherService;
    private final PurchaseOrderRepository purchaseOrderRepository;

    private final ProcurementPaymentRequestRepository procurementPaymentRequestRepository;


    private final DocumentStatusRepository documentStatusRepository;

    private static final long DEFAULT_RENEWAL_LEAD_DAYS = 30L;

    private final MilestoneOnHoldApprovalService milestoneOnHoldApprovalService;

    public ProjectMilestoneAssignmentServiceImpl(
            ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository,
            UserRepository userRepository,
            ProjectDocumentUploadRepository projectDocumentUploadRepository,
            MilestoneStatusHistoryRepository milestoneStatusHistoryRepository,
            ProjectRepository projectRepository,
            ProjectAssignmentHistoryRepository projectAssignmentHistoryRepository,
            UserProductMapRepository userProductMapRepository,
            UserPerformanceCountRepository userPerformanceCountRepository,
            MilestoneStatusRepository milestoneStatusRepository,
            ProjectStatusRepository projectStatusRepository,
            AutoAssignmentService autoAssignmentService,
            MilestoneValidator milestoneValidator,
            @Lazy ProjectService projectService,
            ProcurementMilestoneAssignmentRepository procurementMilestoneAssignmentRepository,
            NotificationPublisherService notificationPublisherService,
            DocumentStatusRepository documentStatusRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            ProcurementPaymentRequestRepository procurementPaymentRequestRepository,
            MilestoneOnHoldApprovalService milestoneOnHoldApprovalService


    ) {
        this.projectMilestoneAssignmentRepository = projectMilestoneAssignmentRepository;
        this.userRepository = userRepository;
        this.projectDocumentUploadRepository = projectDocumentUploadRepository;
        this.milestoneStatusHistoryRepository = milestoneStatusHistoryRepository;
        this.projectRepository = projectRepository;
        this.projectAssignmentHistoryRepository = projectAssignmentHistoryRepository;
        this.userProductMapRepository = userProductMapRepository;
        this.userPerformanceCountRepository = userPerformanceCountRepository;
        this.milestoneStatusRepository = milestoneStatusRepository;
        this.projectStatusRepository = projectStatusRepository;
        this.autoAssignmentService = autoAssignmentService;
        this.milestoneValidator = milestoneValidator;
        this.projectService = projectService;
        this.procurementMilestoneAssignmentRepository = procurementMilestoneAssignmentRepository;
        this.notificationPublisherService = notificationPublisherService;
        this.documentStatusRepository = documentStatusRepository;
        this.purchaseOrderRepository=purchaseOrderRepository;
        this.procurementPaymentRequestRepository=procurementPaymentRequestRepository;
        this.milestoneOnHoldApprovalService = milestoneOnHoldApprovalService;


    }

    @Override
    public void updateMilestoneStatus(UpdateMilestoneStatusDto updateDto) {

        logger.info(
                "Updating milestone assignment ID: {} to status: {} by user ID: {}",
                updateDto.getAssignmentId(),
                updateDto.getNewStatusName(),
                updateDto.getChangedById()
        );

        ProjectMilestoneAssignment assignment =
                projectMilestoneAssignmentRepository
                        .findActiveUserById(updateDto.getAssignmentId())
                        .orElseThrow(() -> {
                            logger.error(
                                    "Milestone assignment ID {} not found or is deleted",
                                    updateDto.getAssignmentId()
                            );

                            return new ResourceNotFoundException(
                                    "Milestone assignment not found",
                                    "MILESTONE_ASSIGNMENT_NOT_FOUND"
                            );
                        });

        User changedBy =
                userRepository
                        .findActiveUserById(updateDto.getChangedById())
                        .orElseThrow(() -> {
                            logger.error(
                                    "User ID {} not found or is deleted",
                                    updateDto.getChangedById()
                            );

                            return new ResourceNotFoundException(
                                    "User not found",
                                    "USER_NOT_FOUND"
                            );
                        });

        MilestoneStatus newStatus =
                milestoneStatusRepository
                        .findByName(updateDto.getNewStatusName())
                        .orElseThrow(() -> {
                            logger.error(
                                    "Milestone status {} not found",
                                    updateDto.getNewStatusName()
                            );

                            return new ResourceNotFoundException(
                                    "Milestone status not found",
                                    "STATUS_NOT_FOUND"
                            );
                        });

        String currentStatusName = assignment.getStatus() != null
                ? assignment.getStatus().getName()
                : null;

        String requestedStatusName = newStatus.getName();

        /*
         * Once a milestone is COMPLETED, no further status change is allowed.
         */
        if ("COMPLETED".equalsIgnoreCase(currentStatusName)) {
            throw new ValidationException(
                    "Completed milestone status cannot be changed again",
                    "COMPLETED_MILESTONE_STATUS_CHANGE_NOT_ALLOWED"
            );
        }

        /*
         * Prevent duplicate same-status update.
         */
        if (currentStatusName != null
                && currentStatusName.equalsIgnoreCase(requestedStatusName)) {

            throw new ValidationException(
                    "Milestone is already in " + requestedStatusName + " status",
                    "MILESTONE_ALREADY_IN_REQUESTED_STATUS"
            );
        }

        /*
         * ON_HOLD approval flow.
         *
         * Do not change the milestone status immediately.
         * Create a pending request for the assigned user's manager.
         */
        if ("ON_HOLD".equalsIgnoreCase(requestedStatusName)) {

            logger.info(
                    "[MILESTONE-ON-HOLD-APPROVAL-REQUEST] " +
                            "assignmentId={}, projectId={}, requestedById={}, " +
                            "currentStatus={}, reason={}",
                    assignment.getId(),
                    assignment.getProject() != null
                            ? assignment.getProject().getId()
                            : null,
                    changedBy.getId(),
                    currentStatusName,
                    updateDto.getStatusReason()
            );

            milestoneOnHoldApprovalService.requestOnHold(updateDto);

            logger.info(
                    "[MILESTONE-ON-HOLD-APPROVAL-SUBMITTED] " +
                            "assignmentId={}, requestedById={}",
                    assignment.getId(),
                    changedBy.getId()
            );

            /*
             * Important:
             * Stop execution so ON_HOLD is not directly written below.
             */
            return;
        }

        /*
         * Business validation before starting Filing/Filling milestone.
         */
        String milestoneName = getMilestoneName(assignment);

        if ("IN_PROGRESS".equalsIgnoreCase(newStatus.getName())
                && isFilingMilestone(milestoneName)) {

            milestoneValidator.validateFillingMilestone(assignment);
        }

        /*
         * Business validations before marking the milestone COMPLETED.
         */
        if ("COMPLETED".equalsIgnoreCase(newStatus.getName())) {

            if ("Documentation".equalsIgnoreCase(milestoneName)) {
                milestoneValidator.validateDocumentMilestone(assignment);
            }

            if ("Legal Verification".equalsIgnoreCase(milestoneName)
                    || "Legal Verfication".equalsIgnoreCase(milestoneName)) {

                milestoneValidator.validateLegalMilestone(assignment);
            }

            if (isFilingMilestone(milestoneName)) {
                milestoneValidator.validateFillingMilestone(assignment);
            }

            if ("Procurement".equalsIgnoreCase(milestoneName)) {
                validateProcurementMilestoneBeforeCompletion(assignment);
            }

            if (isCertificationMilestone(milestoneName)) {
                validateAndSetCertificationDetails(
                        updateDto,
                        assignment
                );
            }
        }

        /*
         * Milestone REJECTED logic has been removed.
         *
         * REWORK should be used when milestone correction is required.
         */

        /*
         * If the milestone is getting completed:
         * 1. Reduce the assigned user's active assignment count.
         * 2. Add time spent.
         * 3. Mark the user-product mapping as unassigned.
         */
        if ("COMPLETED".equalsIgnoreCase(newStatus.getName())) {

            if (assignment.getAssignedUser() != null) {

                User oldUser = assignment.getAssignedUser();

                UserPerformanceCount count =
                        userPerformanceCountRepository
                                .findByUserIdAndProductId(
                                        oldUser.getId(),
                                        assignment.getProject()
                                                .getProduct()
                                                .getId()
                                );

                if (count != null) {

                    count.setTimeSpent(
                            count.getTimeSpent()
                                    + assignment
                                    .getProductMilestoneMap()
                                    .getTatInDays()
                    );

                    count.setAssignmentCount(
                            Math.max(
                                    0,
                                    count.getAssignmentCount() - 1
                            )
                    );

                    count.setLastUpdatedDate(new Date());
                    count.setUpdatedDate(new Date());
                    count.setUpdatedBy(updateDto.getChangedById());

                    userPerformanceCountRepository.save(count);

                    logger.info(
                            "[MILESTONE-COMPLETION-PERFORMANCE-UPDATED] " +
                                    "assignmentId={}, userId={}, assignmentCount={}, timeSpent={}",
                            assignment.getId(),
                            oldUser.getId(),
                            count.getAssignmentCount(),
                            count.getTimeSpent()
                    );
                }

                UserProductMap userMap =
                        userProductMapRepository
                                .findByUserIdAndProductIdAndIsDeletedFalse(
                                        oldUser.getId(),
                                        assignment.getProject()
                                                .getProduct()
                                                .getId()
                                )
                                .orElse(null);

                if (userMap != null) {
                    userMap.setAssigned(false);
                    userMap.setUpdatedDate(new Date());
                    userMap.setUpdatedBy(updateDto.getChangedById());

                    userProductMapRepository.save(userMap);

                    logger.info(
                            "[MILESTONE-COMPLETION-USER-PRODUCT-RELEASED] " +
                                    "assignmentId={}, userId={}, productId={}",
                            assignment.getId(),
                            oldUser.getId(),
                            assignment.getProject().getProduct().getId()
                    );
                }
            }
        }

        /*
         * Save milestone status history before changing the current status.
         */
        MilestoneStatusHistory history = new MilestoneStatusHistory();

        history.setMilestoneAssignment(assignment);
        history.setPreviousStatus(assignment.getStatus());
        history.setNewStatus(newStatus);
        history.setChangeReason(updateDto.getStatusReason());
        history.setChangedBy(changedBy);
        history.setChangeDate(new Date());
        history.setDeleted(false);

        milestoneStatusHistoryRepository.save(history);

        logger.info(
                "[MILESTONE-STATUS-HISTORY-SAVED] " +
                        "assignmentId={}, previousStatus={}, newStatus={}, changedById={}",
                assignment.getId(),
                currentStatusName,
                newStatus.getName(),
                changedBy.getId()
        );

        /*
         * Update the milestone assignment status.
         */
        assignment.setStatus(newStatus);
        assignment.setStatusReason(updateDto.getStatusReason());

        if ("IN_PROGRESS".equalsIgnoreCase(newStatus.getName())) {
            assignment.setStartedDate(new Date());
        }

        if ("COMPLETED".equalsIgnoreCase(newStatus.getName())) {
            assignment.setCompletedDate(new Date());
        }

        assignment.setUpdatedBy(updateDto.getChangedById());
        assignment.setUpdatedDate(new Date());

        assignment =
                projectMilestoneAssignmentRepository.save(assignment);

        logger.info(
                "[MILESTONE-STATUS-UPDATED] " +
                        "assignmentId={}, previousStatus={}, newStatus={}, changedById={}, changedByName={}",
                assignment.getId(),
                currentStatusName,
                newStatus.getName(),
                changedBy.getId(),
                changedBy.getFullName()
        );

        Project project = assignment.getProject();

        /*
         * After completing a milestone, recalculate the visibility of all
         * milestones belonging to the project.
         */
        if ("COMPLETED".equalsIgnoreCase(newStatus.getName())) {

            projectService.updateMilestoneVisibilities(
                    project,
                    updateDto.getChangedById()
            );

            logger.info(
                    "[MILESTONE-VISIBILITY-RECALCULATED] projectId={}, assignmentId={}",
                    project.getId(),
                    assignment.getId()
            );
        }

        /*
         * Recalculate the overall project status.
         */
        updateProjectStatus(
                project,
                updateDto.getChangedById()
        );

        logger.info(
                "[MILESTONE-STATUS-UPDATE-SUCCESS] " +
                        "assignmentId={}, projectId={}, finalStatus={}, changedById={}",
                assignment.getId(),
                project.getId(),
                assignment.getStatus() != null
                        ? assignment.getStatus().getName()
                        : null,
                changedBy.getId()
        );
    }



    private boolean isCertificationMilestone(String milestoneName) {

        return milestoneName != null
                && "Certification".equalsIgnoreCase(
                milestoneName.trim()
        );
    }

    private void validateAndSetCertificationDetails(
            UpdateMilestoneStatusDto updateDto,
            ProjectMilestoneAssignment assignment
    ) {

        if (updateDto.getCertificationTenure() == null
                || updateDto.getCertificationTenure() <= 0) {

            throw new ValidationException(
                    "Certification tenure is required and must be greater than zero",
                    "ERR_CERTIFICATION_TENURE_REQUIRED"
            );
        }

        if (updateDto.getCertificationTenureUnit() == null) {

            throw new ValidationException(
                    "Certification tenure unit is required",
                    "ERR_CERTIFICATION_TENURE_UNIT_REQUIRED"
            );
        }

        if (updateDto.getCertificateExpiryDate() == null) {

            throw new ValidationException(
                    "Certification expiry date is required",
                    "ERR_CERTIFICATE_EXPIRY_DATE_REQUIRED"
            );
        }

        if (updateDto.getCertificateExpiryDate().isBefore(LocalDate.now())) {

            throw new ValidationException(
                    "Certification expiry date cannot be in the past",
                    "ERR_CERTIFICATE_EXPIRY_DATE_IN_PAST"
            );
        }

        if (updateDto.getCertificationAttachmentUrl() == null
                || updateDto.getCertificationAttachmentUrl().isBlank()) {

            throw new ValidationException(
                    "Certification attachment is required",
                    "ERR_CERTIFICATION_ATTACHMENT_REQUIRED"
            );
        }

        LocalDate renewalDueDate =
                updateDto.getCertificateExpiryDate()
                        .minusDays(DEFAULT_RENEWAL_LEAD_DAYS);

        assignment.setCertificationTenure(
                updateDto.getCertificationTenure()
        );

        assignment.setCertificationTenureUnit(
                updateDto.getCertificationTenureUnit()
        );

        assignment.setCertificateExpiryDate(
                updateDto.getCertificateExpiryDate()
        );

        assignment.setRenewalDueDate(
                renewalDueDate
        );

        assignment.setCertificationAttachmentUrl(
                updateDto.getCertificationAttachmentUrl().trim()
        );
    }

    private boolean isFilingMilestone(String milestoneName) {
        return "Filing".equalsIgnoreCase(milestoneName)
                || "Filling".equalsIgnoreCase(milestoneName);
    }


    private void validateProcurementMilestoneBeforeCompletion(
            ProjectMilestoneAssignment assignment
    ) {

        // =========================================================
        // 1. PROCUREMENT ASSIGNMENT
        // =========================================================

        ProcurementMilestoneAssignment procurementAssignment =
                procurementMilestoneAssignmentRepository
                        .findByProjectIdAndMilestoneIdAndIsDeletedFalse(
                                assignment.getProject().getId(),
                                assignment.getMilestone().getId()
                        )
                        .orElseThrow(() ->
                                new ValidationException(
                                        "Procurement assignment is not created for this milestone",
                                        "ERR_PROCUREMENT_ASSIGNMENT_NOT_FOUND"
                                )
                        );

        // =========================================================
        // 2. VENDOR MUST BE SELECTED
        // =========================================================

        if (procurementAssignment.getSelectedVendor() == null) {

            throw new ValidationException(
                    "Please select vendor before completing Procurement milestone",
                    "ERR_VENDOR_NOT_SELECTED"
            );
        }

        // =========================================================
        // 3. PROCUREMENT STATUS MUST BE ELIGIBLE
        // =========================================================

        ProcurementStatus procurementStatus =
                procurementAssignment.getStatus();

        if (!isProcurementEligibleForMilestoneCompletion(
                procurementStatus
        )) {

            throw new ValidationException(
                    "Procurement is not ready for completion. Current status: "
                            + procurementStatus,
                    "ERR_INVALID_PROCUREMENT_STATUS"
            );
        }

        // =========================================================
        // 4. PURCHASE ORDER MUST EXIST
        // =========================================================

        ProcurementOrder purchaseOrder =
                purchaseOrderRepository
                        .findByProcurementAssignmentId(
                                procurementAssignment.getId()
                        )
                        .orElseThrow(() ->
                                new ValidationException(
                                        "Purchase Order must be created before completing Procurement milestone",
                                        "ERR_PO_NOT_CREATED"
                                )
                        );

        if (purchaseOrder.isDeleted()) {

            throw new ValidationException(
                    "Purchase Order is deleted and cannot be used for Procurement completion",
                    "ERR_PO_DELETED"
            );
        }

        // =========================================================
        // 5. PURCHASE ORDER MUST BE APPROVED
        // =========================================================

        if (purchaseOrder.getStatus()
                != ProcurementOrderStatus.APPROVED) {

            throw new ValidationException(
                    "Purchase Order must be approved before completing Procurement milestone",
                    "ERR_PO_NOT_APPROVED"
            );
        }

        // =========================================================
        // 6. PO BASIC AMOUNT
        // =========================================================

        BigDecimal poAmount =
                purchaseOrder.getFinalAmount();

        if (poAmount == null
                || poAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ValidationException(
                    "Purchase Order final amount is missing or invalid",
                    "ERR_PO_FINAL_AMOUNT_INVALID"
            );
        }

        poAmount =
                poAmount.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        // =========================================================
        // 7. TOTAL RELEASED PR BASIC/TAXABLE AMOUNT
        // =========================================================

        BigDecimal totalReleasedPrAmount =
                procurementPaymentRequestRepository
                        .sumReleasedTaxableAmountByOrder(
                                purchaseOrder
                        );

        if (totalReleasedPrAmount == null) {

            totalReleasedPrAmount =
                    BigDecimal.ZERO.setScale(
                            2,
                            RoundingMode.HALF_UP
                    );

        } else {

            totalReleasedPrAmount =
                    totalReleasedPrAmount.setScale(
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        // =========================================================
        // 8. PAYMENT MUST ACTUALLY BE RELEASED
        // =========================================================

        if (totalReleasedPrAmount.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new ValidationException(
                    "Vendor payment must be released before completing Procurement milestone",
                    "ERR_PROCUREMENT_PAYMENT_NOT_RELEASED"
            );
        }

        // =========================================================
        // 9. RELEASED PR BASIC AMOUNT MUST EQUAL PO BASIC AMOUNT
        // =========================================================

        int comparison =
                totalReleasedPrAmount.compareTo(
                        poAmount
                );

        /*
         * Released amount is less than PO.
         */
        if (comparison < 0) {

            BigDecimal remainingAmount =
                    poAmount.subtract(
                            totalReleasedPrAmount
                    );

            throw new ValidationException(
                    "Vendor payment is not fully released. "
                            + "PO Amount: " + poAmount
                            + ", Released PR Amount: " + totalReleasedPrAmount
                            + ", Remaining Amount: " + remainingAmount,
                    "ERR_PROCUREMENT_PAYMENT_INCOMPLETE"
            );
        }

        /*
         * Released amount is greater than PO.
         * Normally PR creation validation should already prevent this,
         * but keep this defensive validation.
         */
        if (comparison > 0) {

            throw new ValidationException(
                    "Released Payment Request amount exceeds Purchase Order amount. "
                            + "PO Amount: " + poAmount
                            + ", Released PR Amount: " + totalReleasedPrAmount,
                    "ERR_PROCUREMENT_PAYMENT_EXCEEDS_PO"
            );
        }

        /*
         * =========================================================
         * SUCCESS
         * =========================================================
         *
         * Vendor selected                   = YES
         * Procurement status eligible       = YES
         * Purchase Order exists             = YES
         * Purchase Order approved           = YES
         * Vendor payment released           = YES
         * Released PR basic amount == PO amount
         *
         * Procurement milestone can now be COMPLETED.
         */
    }

    private boolean isProcurementEligibleForMilestoneCompletion(
            ProcurementStatus status
    ) {

        if (status == null) {
            return false;
        }

        return List.of(
                ProcurementStatus.PO_APPROVED,
                ProcurementStatus.PO_RELEASED,
                ProcurementStatus.ADVANCE_PAID,
                ProcurementStatus.IN_PROGRESS,
                ProcurementStatus.UNDER_REVIEW,
                ProcurementStatus.COMPLETED
        ).contains(status);
    }


    @Override
    public ReassignMilestoneResponseDto reassignMilestone(ReassignMilestoneDto reassignDto) {
        logger.info("Reassigning milestone {} → user {} by {}",
                reassignDto.getAssignmentId(), reassignDto.getNewUserId(), reassignDto.getChangedById());


        ProjectMilestoneAssignment assignment = projectMilestoneAssignmentRepository.findActiveUserById(reassignDto.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Milestone assignment not found", "MILESTONE_ASSIGNMENT_NOT_FOUND"));

        User newUser = userRepository.findActiveUserById(reassignDto.getNewUserId())
                .orElseThrow(() -> new ResourceNotFoundException("New assignee not found", "USER_NOT_FOUND"));

        User changedBy = userRepository.findActiveUserById(reassignDto.getChangedById())
                .orElseThrow(() -> new ResourceNotFoundException("Requesting user not found", "USER_NOT_FOUND"));

        boolean isAdmin = changedBy.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
        boolean isOperationHead = changedBy.getRoles().stream().anyMatch(r -> "OPERATION_HEAD".equals(r.getName()));
        boolean isManager = changedBy.isManagerFlag();

        if (!isAdmin && !isOperationHead && !isManager) {
            throw new ValidationException("Only ADMIN, OPERATION_HEAD, or MANAGER can reassign milestones", "NOT_AUTHORIZED_TO_REASSIGN");
        }

        // Department check for Managers only
        if (isManager && !isAdmin && !isOperationHead) {
            if (!isManagerOfMilestoneDepartment(changedBy, assignment)) {
                throw new ValidationException("You can only reassign milestones in your department(s)", "MANAGER_DEPARTMENT_MISMATCH");
            }
        }

        if (reassignDto.getReassignmentReason() == null || reassignDto.getReassignmentReason().trim().isEmpty()) {
            throw new ValidationException("Reassignment reason is required", "INVALID_REASSIGNMENT_REASON");
        }

        if ("COMPLETED".equals(assignment.getStatus().getName())) {
            throw new ValidationException("Cannot reassign a completed milestone", "COMPLETED_MILESTONE_REASSIGNMENT");
        }

        if (assignment.getAssignedUser() != null && assignment.getAssignedUser().getId().equals(reassignDto.getNewUserId())) {
            throw new ValidationException("Milestone is already assigned to this user", "SAME_USER_REASSIGNMENT");
        }

        ProductMilestoneMap milestoneMap = assignment.getProductMilestoneMap();

        // Department eligibility check (skip for Admin/Operation Head)
        if (!isAdmin && !isOperationHead) {
            List<Long> requiredDeptIds = milestoneMap.getMilestone().getDepartments().stream()
                    .map(Department::getId).toList();
            List<Long> userDeptIds = newUser.getDepartments().stream()
                    .map(Department::getId).toList();

            if (requiredDeptIds.stream().noneMatch(userDeptIds::contains)) {
                throw new ValidationException(
                        "Selected user is not in the required department for this milestone",
                        "INELIGIBLE_USER_DEPARTMENT");
            }
        }

        // Unassign old user
        if (assignment.getAssignedUser() != null) {
            User oldUser = assignment.getAssignedUser();
            UserProductMap oldMap = userProductMapRepository
                    .findByUserIdAndProductIdAndIsDeletedFalse(oldUser.getId(), assignment.getProject().getProduct().getId())
                    .orElse(null);
            if (oldMap != null) {
                oldMap.setAssigned(false);
                userProductMapRepository.save(oldMap);
            }

            UserPerformanceCount oldCount = userPerformanceCountRepository
                    .findByUserIdAndProductId(oldUser.getId(), assignment.getProject().getProduct().getId());
            if (oldCount != null) {
                oldCount.setAssignmentCount(Math.max(0, oldCount.getAssignmentCount() - 1));
                oldCount.setUpdatedDate(new Date());
                oldCount.setUpdatedBy(reassignDto.getChangedById());
                userPerformanceCountRepository.save(oldCount);
            }
        }

        // Assign to new user
        UserProductMap newMap = userProductMapRepository
                .findByUserIdAndProductIdAndIsDeletedFalse(newUser.getId(), assignment.getProject().getProduct().getId())
                .orElseGet(() -> createUserProductMap(newUser, assignment.getProject().getProduct(), reassignDto.getChangedById()));

        newMap.setAssigned(true);
        userProductMapRepository.save(newMap);

        UserPerformanceCount newCount = userPerformanceCountRepository
                .findByUserIdAndProductId(newUser.getId(), assignment.getProject().getProduct().getId());
        if (newCount == null) {
            newCount = new UserPerformanceCount();
            newCount.setUser(newUser);
            newCount.setProduct(assignment.getProject().getProduct());
            newCount.setAssignmentCount(1);
            newCount.setTimeSpent(0.0);
            newCount.setCreatedDate(new Date());
            newCount.setCreatedBy(reassignDto.getChangedById());
        } else {
            newCount.setAssignmentCount(newCount.getAssignmentCount() + 1);
        }
        newCount.setUpdatedDate(new Date());
        newCount.setUpdatedBy(reassignDto.getChangedById());
        userPerformanceCountRepository.save(newCount);

        // Save history
        ProjectAssignmentHistory history = new ProjectAssignmentHistory();
        history.setProject(assignment.getProject());
        history.setMilestoneAssignment(assignment);
        history.setAssignedUser(newUser);
        history.setAssignmentReason(reassignDto.getReassignmentReason());
        history.setCreatedDate(new Date());
        history.setUpdatedDate(new Date());
        history.setCreatedBy(reassignDto.getChangedById());
        history.setUpdatedBy(reassignDto.getChangedById());
        history.setDeleted(false);
        projectAssignmentHistoryRepository.save(history);

        // Final update
        assignment.setAssignedUser(newUser);
        assignment.setStatus(milestoneStatusRepository.findByName("NEW")
                .orElseThrow(() -> new ResourceNotFoundException("Status NEW not found", "STATUS_NOT_FOUND")));
        assignment.setStatusReason("Reassigned by " + (isAdmin ? "ADMIN" : isOperationHead ? "OP HEAD" : "MANAGER"));
        assignment.setUpdatedBy(reassignDto.getChangedById());
        assignment.setUpdatedDate(new Date());
        projectMilestoneAssignmentRepository.save(assignment);

        pushProjectAssignmentNotification(
                assignment,
                newUser,
                changedBy,
                true,
                reassignDto.getReassignmentReason()
        );

        logger.info("Milestone {} successfully reassigned to {} by {}",
                reassignDto.getAssignmentId(), newUser.getFullName(), changedBy.getFullName());

        ReassignMilestoneResponseDto response = new ReassignMilestoneResponseDto();

        response.setAssignmentId(assignment.getId());
        response.setNewUserId(newUser.getId());
        response.setProjectId(assignment.getProject() != null ? assignment.getProject().getId() : null);

        response.setNewUserName(
                newUser.getFullName() != null && !newUser.getFullName().trim().isEmpty()
                        ? newUser.getFullName().trim()
                        : "Unknown User"
        );

        response.setNewUserEmail(
                newUser.getEmail() != null && !newUser.getEmail().trim().isEmpty()
                        ? newUser.getEmail().trim()
                        : ""
        );

        response.setMilestoneName(
                milestoneMap != null
                        && milestoneMap.getMilestone() != null
                        && milestoneMap.getMilestone().getName() != null
                        ? milestoneMap.getMilestone().getName().trim()
                        : "Unknown Milestone"
        );

        response.setReassignmentReason(
                reassignDto.getReassignmentReason() != null
                        && !reassignDto.getReassignmentReason().trim().isEmpty()
                        ? reassignDto.getReassignmentReason().trim()
                        : "No reason provided"
        );



        return response;
    }

    private void pushProjectAssignmentNotification(
            ProjectMilestoneAssignment assignment,
            User assignedUser,
            User actor,
            boolean reassigned,
            String reason
    ) {

        if (assignment == null || assignment.getId() == null) {
            logger.warn(
                    "[MILESTONE-NOTIFICATION-SKIP] assignment is null or assignmentId is null"
            );
            return;
        }

        if (assignedUser == null || assignedUser.getId() == null) {
            logger.warn(
                    "[MILESTONE-NOTIFICATION-SKIP] assignedUser is null or assignedUserId is null. assignmentId={}",
                    assignment.getId()
            );
            return;
        }

        Project project = assignment.getProject();

        String projectName = getProjectName(project);
        String projectNumber = getProjectNumber(project);
        String milestoneName = getMilestoneName(assignment);
        String actorName = getUserDisplayName(actor);

        NotificationEventType eventType = reassigned
                ? NotificationEventType.MILESTONE_REASSIGNED
                : NotificationEventType.MILESTONE_ASSIGNED;

        String title = reassigned
                ? "Project Milestone Reassigned"
                : "Project Milestone Assigned";

        String message = reassigned
                ? "You have been reassigned to milestone \"" + milestoneName
                + "\" for project \"" + projectName
                + "\" by " + actorName + "."
                : "You have been assigned to milestone \"" + milestoneName
                + "\" for project \"" + projectName + "\".";

        String metadataJson =
                "{"
                        + "\"projectId\":" + (project != null ? project.getId() : null) + ","
                        + "\"projectName\":\"" + escapeJson(projectName) + "\","
                        + "\"projectNumber\":\"" + escapeJson(projectNumber) + "\","
                        + "\"milestoneAssignmentId\":" + assignment.getId() + ","
                        + "\"milestoneName\":\"" + escapeJson(milestoneName) + "\","
                        + "\"assignedUserId\":" + assignedUser.getId() + ","
                        + "\"assignedUserName\":\""
                        + escapeJson(getUserDisplayName(assignedUser)) + "\","
                        + "\"assignedById\":" + (actor != null ? actor.getId() : null) + ","
                        + "\"assignedByName\":\"" + escapeJson(actorName) + "\","
                        + "\"reason\":\"" + escapeJson(reason) + "\","
                        + "\"reassigned\":" + reassigned
                        + "}";

        logger.info(
                "[MILESTONE-NOTIFICATION-START] assignmentId={}, projectId={}, assignedUserId={}, actorId={}, eventType={}, reassigned={}",
                assignment.getId(),
                project != null ? project.getId() : null,
                assignedUser.getId(),
                actor != null ? actor.getId() : null,
                eventType,
                reassigned
        );

        logger.debug(
                "[MILESTONE-NOTIFICATION-DATA] title={}, message={}, metadataJson={}",
                title,
                message,
                metadataJson
        );

        notificationPublisherService.sendNotification(
                NotificationCreateRequestDto.builder()
                        .receiverId(assignedUser.getId())
                        .actorId(actor != null ? actor.getId() : null)
                        .actorName(actorName)
                        .module(NotificationModule.PROJECT)
                        .eventType(eventType)
                        .referenceId(
                                project != null
                                        ? project.getId()
                                        : assignment.getId()
                        )
                        .referenceNumber(projectNumber)
                        .title(title)
                        .message(message)
                        .redirectUrl(
                                "/projects/"
                                        + (project != null ? project.getId() : "")
                                        + "/milestones/"
                                        + assignment.getId()
                        )
                        .priority(NotificationPriority.HIGH)
                        .displayType(NotificationDisplayType.INFO)
                        .metadataJson(metadataJson)
                        .build()
        );

        logger.info(
                "[MILESTONE-NOTIFICATION-SUCCESS] assignmentId={}, receiverId={}, eventType={}",
                assignment.getId(),
                assignedUser.getId(),
                eventType
        );
    }

    private String getUserDisplayName(User user) {
        if (user == null) {
            return "System";
        }

        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            return user.getFullName().trim();
        }

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail().trim();
        }

        return "User";
    }

    private String getMilestoneName(ProjectMilestoneAssignment assignment) {
        if (assignment == null) {
            return "Milestone";
        }

        if (assignment.getMilestone() != null
                && assignment.getMilestone().getName() != null
                && !assignment.getMilestone().getName().trim().isEmpty()) {
            return assignment.getMilestone().getName().trim();
        }

        if (assignment.getProductMilestoneMap() != null
                && assignment.getProductMilestoneMap().getMilestone() != null
                && assignment.getProductMilestoneMap().getMilestone().getName() != null
                && !assignment.getProductMilestoneMap().getMilestone().getName().trim().isEmpty()) {
            return assignment.getProductMilestoneMap().getMilestone().getName().trim();
        }

        return "Milestone-" + assignment.getId();
    }

    private String getProjectName(Project project) {
        if (project == null) {
            return "Project";
        }

        try {
            if (project.getName() != null && !project.getName().trim().isEmpty()) {
                return project.getName().trim();
            }
        } catch (Exception ignored) {
        }

        return "Project-" + project.getId();
    }

    private String getProjectNumber(Project project) {
        if (project == null) {
            return "";
        }

        try {
            if (project.getProjectNo() != null && !project.getProjectNo().trim().isEmpty()) {
                return project.getProjectNo().trim();
            }
        } catch (Exception ignored) {
        }

        return "PROJECT-" + project.getId();
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private UserProductMap createUserProductMap(User user, Product product, Long createdBy) {
        UserProductMap map = new UserProductMap();
        map.setUser(user);
        map.setProduct(product);
        map.setRating(0.0);
        map.setAssigned(false);
        map.setDeleted(false);
        map.setCreatedDate(new Date());
        map.setUpdatedDate(new Date());
        map.setCreatedBy(createdBy);
        map.setUpdatedBy(createdBy);
        return userProductMapRepository.save(map);
    }

    private boolean isManagerOfMilestoneDepartment(User manager, ProjectMilestoneAssignment assignment) {
        List<Long> managerDepts = manager.getDepartments().stream().map(Department::getId).toList();
        List<Long> milestoneDepts = assignment.getProductMilestoneMap().getMilestone().getDepartments()
                .stream().map(Department::getId).toList();
        return managerDepts.stream().anyMatch(milestoneDepts::contains);
    }


    private void updateProjectStatus(
            Project project,
            Long updatedById
    ) {
        if (project == null || project.getStatus() == null) {
            return;
        }

        Long currentProjectStatusId =
                project.getStatus().getId();

        /*
         * Do not allow milestone calculation to overwrite
         * administrative/terminal project statuses.
         */
        if (StatusConstants.PROJECT_FORCE_CLOSED_ID
                .equals(currentProjectStatusId)
                || StatusConstants.PROJECT_CANCELLED_ID
                .equals(currentProjectStatusId)
                || StatusConstants.PROJECT_REFUNDED_ID
                .equals(currentProjectStatusId)) {

            logger.info(
                    "Skipping automatic project status update. projectId={}, currentStatus={}",
                    project.getId(),
                    project.getStatus().getName()
            );

            return;
        }

        List<ProjectMilestoneAssignment> assignments =
                projectMilestoneAssignmentRepository
                        .findByProjectIdAndIsDeletedFalse(
                                project.getId()
                        );

        String newStatusName;

        if (assignments.isEmpty()) {
            newStatusName = "OPEN";

        } else if (assignments.stream().allMatch(assignment ->
                assignment.getStatus() != null
                        && "COMPLETED".equalsIgnoreCase(
                        assignment.getStatus().getName()
                )
        )) {
            newStatusName = "COMPLETED";

        } else if (assignments.stream().anyMatch(assignment ->
                assignment.getStatus() != null
                        && List.of(
                        "IN_PROGRESS",
                        "ON_HOLD",
                        "REWORK"
                ).contains(
                        assignment.getStatus()
                                .getName()
                                .toUpperCase()
                )
        )) {
            newStatusName = "IN_PROGRESS";

        } else {
            newStatusName = "OPEN";
        }

        ProjectStatus status =
                projectStatusRepository
                        .findByName(newStatusName)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Project status not found: "
                                                + newStatusName,
                                        "STATUS_NOT_FOUND"
                                )
                        );

        project.setStatus(status);
        project.setUpdatedBy(updatedById);
        project.setUpdatedDate(new Date());

        updateProjectProgress(project);

        projectRepository.save(project);

    }

    private void updateProjectProgress(Project project) {

        List<ProjectMilestoneAssignment> assignments =
                projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());

//        if (assignments.isEmpty()) {
//            project.setProgressPercentage(0.0);
//            return;
//        }

        long completedCount = assignments.stream()
                .filter(a -> a.getStatus() != null)
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus().getName()))
                .count();

        double progress = (completedCount * 100.0) / assignments.size();

//        project.setProgressPercentage(progress);
    }


    @Override
    @Transactional
    public void sendBackToPreviousMilestone(
            SendBackToPreviousMilestoneDto dto
    ) {

        logger.info(
                "[MILESTONE-SEND-BACK-START] " +
                        "currentAssignmentId={}, changedById={}, " +
                        "rejectedDocumentIds={}, reason={}",
                dto != null ? dto.getCurrentAssignmentId() : null,
                dto != null ? dto.getChangedById() : null,
                dto != null ? dto.getRejectedDocumentIds() : null,
                dto != null ? dto.getReason() : null
        );

        /*
         * Request validation
         */
        if (dto == null) {
            throw new ValidationException(
                    "Send-back request is required",
                    "SEND_BACK_REQUEST_REQUIRED"
            );
        }

        if (dto.getCurrentAssignmentId() == null) {
            throw new ValidationException(
                    "Current milestone assignment ID is required",
                    "CURRENT_ASSIGNMENT_ID_REQUIRED"
            );
        }

        if (dto.getChangedById() == null) {
            throw new ValidationException(
                    "Changed-by user ID is required",
                    "CHANGED_BY_ID_REQUIRED"
            );
        }

        if (dto.getReason() == null || dto.getReason().isBlank()) {
            throw new ValidationException(
                    "Reason is required to send a milestone back for rework",
                    "SEND_BACK_REASON_REQUIRED"
            );
        }

        String reworkReason = dto.getReason().trim();

        /*
         * Find the current milestone assignment.
         */
        ProjectMilestoneAssignment currentAssignment =
                projectMilestoneAssignmentRepository
                        .findActiveUserById(dto.getCurrentAssignmentId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Current milestone assignment not found",
                                "MILESTONE_ASSIGNMENT_NOT_FOUND"
                        ));

        /*
         * Find the user performing the send-back action.
         */
        User changedBy =
                userRepository
                        .findActiveUserById(dto.getChangedById())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User not found",
                                "USER_NOT_FOUND"
                        ));

        logger.info(
                "[MILESTONE-SEND-BACK-USER] " +
                        "currentAssignmentId={}, changedById={}, changedByName={}, " +
                        "assignedUserId={}, assignedUserName={}",
                currentAssignment.getId(),
                changedBy.getId(),
                changedBy.getFullName(),
                currentAssignment.getAssignedUser() != null
                        ? currentAssignment.getAssignedUser().getId()
                        : null,
                currentAssignment.getAssignedUser() != null
                        ? currentAssignment.getAssignedUser().getFullName()
                        : null
        );

        /*
         * Only the user assigned to the current milestone can send the
         * immediately previous milestone back to REWORK.
         */
        if (currentAssignment.getAssignedUser() == null
                || !currentAssignment.getAssignedUser()
                .getId()
                .equals(changedBy.getId())) {

            logger.warn(
                    "[MILESTONE-SEND-BACK-DENIED] " +
                            "User is not the current milestone assignee. " +
                            "currentAssignmentId={}, changedById={}, assignedUserId={}",
                    currentAssignment.getId(),
                    changedBy.getId(),
                    currentAssignment.getAssignedUser() != null
                            ? currentAssignment.getAssignedUser().getId()
                            : null
            );

            throw new ValidationException(
                    "Only the currently assigned user can send the previous milestone back for rework",
                    "NOT_CURRENT_ASSIGNEE"
            );
        }

        /*
         * Do not allow send-back while a manually requested ON_HOLD approval
         * is already pending for the current milestone.
         */
        if (milestoneOnHoldApprovalService.hasPendingRequest(
                currentAssignment.getId()
        )) {
            throw new ValidationException(
                    "An ON_HOLD manager approval request is already pending for this milestone",
                    "ON_HOLD_APPROVAL_PENDING"
            );
        }

        if (currentAssignment.getStatus() == null
                || currentAssignment.getStatus().getName() == null) {
            throw new ValidationException(
                    "Current milestone status is not configured",
                    "CURRENT_MILESTONE_STATUS_NOT_CONFIGURED"
            );
        }

        String currentStatus =
                currentAssignment.getStatus().getName();

        /*
         * The current/downstream milestone can request rework only before it
         * is completed.
         */
        if (!"NEW".equalsIgnoreCase(currentStatus)
                && !"IN_PROGRESS".equalsIgnoreCase(currentStatus)) {

            throw new ValidationException(
                    "Only a NEW or IN_PROGRESS milestone can send the previous milestone back for rework",
                    "INVALID_CURRENT_STATUS_FOR_SEND_BACK"
            );
        }

        Project project = currentAssignment.getProject();

        if (project == null || project.getId() == null) {
            throw new ValidationException(
                    "Project is not configured for the current milestone",
                    "MILESTONE_PROJECT_NOT_CONFIGURED"
            );
        }

        if (currentAssignment.getProductMilestoneMap() == null) {
            throw new ValidationException(
                    "Product milestone mapping is not configured for the current milestone",
                    "PRODUCT_MILESTONE_MAPPING_NOT_CONFIGURED"
            );
        }

        Integer currentOrderValue =
                currentAssignment
                        .getProductMilestoneMap()
                        .getOrder();

        if (currentOrderValue == null) {
            throw new ValidationException(
                    "Milestone order is not configured for the current milestone",
                    "MILESTONE_ORDER_NOT_CONFIGURED"
            );
        }

        int currentOrder = currentOrderValue;

        /*
         * Find all milestones for the project in configured order.
         */
        List<ProjectMilestoneAssignment> assignments =
                projectMilestoneAssignmentRepository
                        .findByProjectIdAndIsDeletedFalse(project.getId())
                        .stream()
                        .filter(assignment ->
                                assignment.getProductMilestoneMap() != null
                        )
                        .filter(assignment ->
                                assignment
                                        .getProductMilestoneMap()
                                        .getOrder() > 0
                        )
                        .sorted((first, second) ->
                                Integer.compare(
                                        first.getProductMilestoneMap().getOrder(),
                                        second.getProductMilestoneMap().getOrder()
                                )
                        )
                        .toList();

        /*
         * Find only the immediately previous milestone.
         *
         * Examples:
         * Technical     -> Documentation
         * Liaison       -> Technical
         * Certification -> Liaison
         */
        ProjectMilestoneAssignment previousAssignment =
                assignments.stream()
                        .filter(assignment ->
                                assignment
                                        .getProductMilestoneMap()
                                        .getOrder() < currentOrder
                        )
                        .reduce((first, second) -> second)
                        .orElseThrow(() -> new ValidationException(
                                "No previous milestone exists for the current milestone",
                                "PREVIOUS_MILESTONE_NOT_FOUND"
                        ));

        if (previousAssignment.getStatus() == null
                || previousAssignment.getStatus().getName() == null) {
            throw new ValidationException(
                    "Previous milestone status is not configured",
                    "PREVIOUS_MILESTONE_STATUS_NOT_CONFIGURED"
            );
        }

        String previousStatus =
                previousAssignment.getStatus().getName();

        /*
         * Only completed work can be reopened as REWORK.
         */
        if (!"COMPLETED".equalsIgnoreCase(previousStatus)) {

            throw new ValidationException(
                    "Previous milestone must be COMPLETED before it can be moved to REWORK",
                    "PREVIOUS_MILESTONE_NOT_COMPLETED"
            );
        }

        String currentMilestoneName =
                getMilestoneName(currentAssignment);

        String previousMilestoneName =
                getMilestoneName(previousAssignment);

        logger.info(
                "[MILESTONE-SEND-BACK-PREVIOUS-FOUND] " +
                        "projectId={}, currentAssignmentId={}, currentMilestone={}, " +
                        "currentStatus={}, currentOrder={}, previousAssignmentId={}, " +
                        "previousMilestone={}, previousStatus={}, previousOrder={}",
                project.getId(),
                currentAssignment.getId(),
                currentMilestoneName,
                currentStatus,
                currentOrder,
                previousAssignment.getId(),
                previousMilestoneName,
                previousStatus,
                previousAssignment.getProductMilestoneMap().getOrder()
        );

        /*
         * Validate maximum rework attempts if configured.
         */
        Integer existingReworkAttemptsValue =
                previousAssignment.getReworkAttempts();

        int existingReworkAttempts =
                existingReworkAttemptsValue == null
                        ? 0
                        : existingReworkAttemptsValue;

        Integer maxAttemptsValue =
                previousAssignment
                        .getProductMilestoneMap()
                        .getMaxAttempts();

        int maxAttempts =
                maxAttemptsValue == null
                        ? 0
                        : maxAttemptsValue;

        /*
         * maxAttempts <= 0 means no maximum limit is configured.
         */
        if (maxAttempts > 0
                && existingReworkAttempts >= maxAttempts) {

            logger.warn(
                    "[MILESTONE-SEND-BACK-MAX-ATTEMPTS] " +
                            "previousAssignmentId={}, existingAttempts={}, maxAttempts={}",
                    previousAssignment.getId(),
                    existingReworkAttempts,
                    maxAttempts
            );

            throw new ValidationException(
                    "Maximum rework attempts reached for the previous milestone",
                    "MAX_REWORK_ATTEMPTS_REACHED"
            );
        }

        /*
         * Resolve milestone statuses.
         */
        MilestoneStatus reworkStatus =
                milestoneStatusRepository
                        .findByName("REWORK")
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "REWORK milestone status not found",
                                "STATUS_NOT_FOUND"
                        ));

        MilestoneStatus onHoldStatus =
                milestoneStatusRepository
                        .findByName("ON_HOLD")
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "ON_HOLD milestone status not found",
                                "STATUS_NOT_FOUND"
                        ));

        /*
         * Document rejection is optional.
         *
         * Technical may send Documentation back with document IDs.
         * Liaison or Certification may send the previous milestone back without
         * providing document IDs.
         *
         * VERIFIED documents are also allowed to be rejected here because this
         * action is performed by the authorized current milestone assignee.
         */
        if (dto.getRejectedDocumentIds() != null
                && !dto.getRejectedDocumentIds().isEmpty()) {

            if (dto.getRejectedDocumentIds().contains(null)) {
                throw new ValidationException(
                        "Rejected document IDs cannot contain null values",
                        "INVALID_REJECTED_DOCUMENT_IDS"
                );
            }

            DocumentStatus rejectedDocumentStatus =
                    documentStatusRepository
                            .findByName("REJECTED")
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Document status REJECTED not found",
                                    "DOCUMENT_STATUS_NOT_FOUND"
                            ));

            /*
             * LinkedHashSet prevents processing duplicate document IDs.
             */
            Set<Long> uniqueDocumentIds =
                    new LinkedHashSet<>(
                            dto.getRejectedDocumentIds()
                    );

            for (Long documentId : uniqueDocumentIds) {

                ProjectDocumentUpload documentUpload =
                        projectDocumentUploadRepository
                                .findActiveUserById(documentId)
                                .orElseThrow(() ->
                                        new ResourceNotFoundException(
                                                "Document not found with ID: "
                                                        + documentId,
                                                "DOCUMENT_UPLOAD_NOT_FOUND"
                                        )
                                );

                if (documentUpload.getProject() == null
                        || documentUpload.getProject().getId() == null
                        || !documentUpload
                        .getProject()
                        .getId()
                        .equals(project.getId())) {

                    throw new ValidationException(
                            "Document ID " + documentId
                                    + " does not belong to project ID "
                                    + project.getId(),
                            "DOCUMENT_PROJECT_MISMATCH"
                    );
                }

                String currentDocumentStatus =
                        documentUpload.getStatus() != null
                                ? documentUpload.getStatus().getName()
                                : null;

                logger.info(
                        "[MILESTONE-SEND-BACK-DOCUMENT-CHECK] " +
                                "documentId={}, projectId={}, currentStatus={}, " +
                                "changedById={}, currentMilestone={}, previousMilestone={}",
                        documentUpload.getId(),
                        project.getId(),
                        currentDocumentStatus,
                        changedBy.getId(),
                        currentMilestoneName,
                        previousMilestoneName
                );

                /*
                 * Avoid creating unnecessary duplicate REJECTED updates.
                 */
                if ("REJECTED".equalsIgnoreCase(currentDocumentStatus)) {

                    logger.info(
                            "[MILESTONE-SEND-BACK-DOCUMENT-SKIPPED] " +
                                    "documentId={} is already REJECTED",
                            documentUpload.getId()
                    );

                    continue;
                }

                /*
                 * VERIFIED documents are allowed here.
                 *
                 * This does not use milestone REJECTED.
                 * Only the selected document becomes REJECTED.
                 */
                if ("VERIFIED".equalsIgnoreCase(currentDocumentStatus)) {
                    logger.warn(
                            "[MILESTONE-SEND-BACK-VERIFIED-DOCUMENT] " +
                                    "Authorized current milestone assignee is rejecting " +
                                    "a VERIFIED document. documentId={}, projectId={}, " +
                                    "changedById={}, reason={}",
                            documentUpload.getId(),
                            project.getId(),
                            changedBy.getId(),
                            reworkReason
                    );
                }

                documentUpload.setStatus(rejectedDocumentStatus);
                documentUpload.setRemarks(reworkReason);
                documentUpload.setUpdatedBy(changedBy.getId());
                documentUpload.setUpdatedDate(new Date());

                projectDocumentUploadRepository.save(documentUpload);

                logger.info(
                        "[MILESTONE-SEND-BACK-DOCUMENT-REJECTED] " +
                                "documentId={}, projectId={}, previousStatus={}, " +
                                "newStatus=REJECTED, changedById={}",
                        documentUpload.getId(),
                        project.getId(),
                        currentDocumentStatus,
                        changedBy.getId()
                );
            }
        }

        /*
         * Save previous milestone history:
         * COMPLETED -> REWORK
         */
        saveMilestoneStatusHistory(
                previousAssignment,
                previousAssignment.getStatus(),
                reworkStatus,
                reworkReason,
                changedBy
        );

        /*
         * Move the immediately previous milestone to REWORK.
         */
        previousAssignment.setStatus(reworkStatus);
        previousAssignment.setStatusReason(reworkReason);
        previousAssignment.setVisible(true);
        previousAssignment.setVisibilityReason(null);
        previousAssignment.setCompletedDate(null);
        previousAssignment.setReworkAttempts(
                existingReworkAttempts + 1
        );
        previousAssignment.setUpdatedBy(changedBy.getId());
        previousAssignment.setUpdatedDate(new Date());

        projectMilestoneAssignmentRepository.save(previousAssignment);

        logger.info(
                "[MILESTONE-SEND-BACK-PREVIOUS-REWORK] " +
                        "previousAssignmentId={}, previousMilestone={}, " +
                        "oldStatus={}, newStatus=REWORK, reworkAttempts={}, " +
                        "assignedUserId={}",
                previousAssignment.getId(),
                previousMilestoneName,
                previousStatus,
                previousAssignment.getReworkAttempts(),
                previousAssignment.getAssignedUser() != null
                        ? previousAssignment.getAssignedUser().getId()
                        : null
        );

        /*
         * Reactivate the previous milestone user's assignment count.
         *
         * The count was reduced when this milestone was previously completed.
         * It must be increased again so that the user's active-work count
         * remains correct during REWORK.
         */
        if (previousAssignment.getAssignedUser() != null
                && project.getProduct() != null) {

            User previousAssignedUser =
                    previousAssignment.getAssignedUser();

            UserPerformanceCount performanceCount =
                    userPerformanceCountRepository
                            .findByUserIdAndProductId(
                                    previousAssignedUser.getId(),
                                    project.getProduct().getId()
                            );

            if (performanceCount != null) {
                performanceCount.setAssignmentCount(
                        performanceCount.getAssignmentCount() + 1
                );
                performanceCount.setLastUpdatedDate(new Date());
                performanceCount.setUpdatedDate(new Date());
                performanceCount.setUpdatedBy(changedBy.getId());

                userPerformanceCountRepository.save(performanceCount);

                logger.info(
                        "[MILESTONE-SEND-BACK-PERFORMANCE-REACTIVATED] " +
                                "userId={}, productId={}, assignmentCount={}",
                        previousAssignedUser.getId(),
                        project.getProduct().getId(),
                        performanceCount.getAssignmentCount()
                );
            }

            UserProductMap previousUserProductMap =
                    userProductMapRepository
                            .findByUserIdAndProductIdAndIsDeletedFalse(
                                    previousAssignedUser.getId(),
                                    project.getProduct().getId()
                            )
                            .orElse(null);

            if (previousUserProductMap != null) {
                previousUserProductMap.setAssigned(true);
                previousUserProductMap.setUpdatedDate(new Date());
                previousUserProductMap.setUpdatedBy(changedBy.getId());

                userProductMapRepository.save(
                        previousUserProductMap
                );
            }
        }

        /*
         * The current milestone is automatically put ON_HOLD because its
         * required previous milestone has returned to REWORK.
         *
         * This is a system-generated hold caused by dependency failure.
         * It is different from a user manually requesting ON_HOLD.
         */
        String holdReason =
                "Waiting for rework of "
                        + (previousMilestoneName != null
                        ? previousMilestoneName
                        : "the previous milestone")
                        + ": "
                        + reworkReason;

        saveMilestoneStatusHistory(
                currentAssignment,
                currentAssignment.getStatus(),
                onHoldStatus,
                holdReason,
                changedBy
        );

        currentAssignment.setStatus(onHoldStatus);
        currentAssignment.setStatusReason(holdReason);
        currentAssignment.setUpdatedBy(changedBy.getId());
        currentAssignment.setUpdatedDate(new Date());

        projectMilestoneAssignmentRepository.save(
                currentAssignment
        );

        logger.info(
                "[MILESTONE-SEND-BACK-CURRENT-ON-HOLD] " +
                        "currentAssignmentId={}, currentMilestone={}, " +
                        "oldStatus={}, newStatus=ON_HOLD, reason={}",
                currentAssignment.getId(),
                currentMilestoneName,
                currentStatus,
                holdReason
        );

        /*
         * Recalculate the overall project status.
         */
        updateProjectStatus(
                project,
                changedBy.getId()
        );

        logger.info(
                "[MILESTONE-SEND-BACK-SUCCESS] " +
                        "projectId={}, currentAssignmentId={}, currentMilestone={}, " +
                        "currentStatus=ON_HOLD, previousAssignmentId={}, " +
                        "previousMilestone={}, previousStatus=REWORK, " +
                        "changedById={}, changedByName={}, rejectedDocumentCount={}",
                project.getId(),
                currentAssignment.getId(),
                currentMilestoneName,
                previousAssignment.getId(),
                previousMilestoneName,
                changedBy.getId(),
                changedBy.getFullName(),
                dto.getRejectedDocumentIds() == null
                        ? 0
                        : new LinkedHashSet<>(
                        dto.getRejectedDocumentIds()
                ).size()
        );
    }
    private void saveMilestoneStatusHistory(
            ProjectMilestoneAssignment assignment,
            MilestoneStatus previousStatus,
            MilestoneStatus newStatus,
            String reason,
            User changedBy
    ) {
        MilestoneStatusHistory history = new MilestoneStatusHistory();
        history.setMilestoneAssignment(assignment);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setChangeReason(reason);
        history.setChangedBy(changedBy);
        history.setChangeDate(new Date());
        history.setDeleted(false);

        milestoneStatusHistoryRepository.save(history);
    }






    /**
     * ADMIN validation helper used only for VERIFIED document protection
     * during send-back/rework flow.
     */
    private boolean isAdminUser(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }

        return user.getRoles().stream()
                .anyMatch(role ->
                        role != null
                                && role.getName() != null
                                && "ADMIN".equalsIgnoreCase(role.getName())
                );
    }

    /**
     * Used only for readable authorization logs.
     */
    private List<String> getRoleNamesForLog(User user) {
        if (user == null || user.getRoles() == null) {
            return List.of();
        }

        return user.getRoles().stream()
                .filter(role -> role != null && role.getName() != null)
                .map(role -> role.getName())
                .toList();
    }

}