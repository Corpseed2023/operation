package com.doc.impl.project;

import com.doc.dto.ProjectMilestoneassignment.ReassignMilestoneDto;
import com.doc.dto.ProjectMilestoneassignment.ReassignMilestoneResponseDto;
import com.doc.dto.ProjectMilestoneassignment.UpdateMilestoneStatusDto;
import com.doc.entity.milestone.MilestoneStatus;
import com.doc.entity.milestone.MilestoneStatusHistory;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.project.*;
import com.doc.entity.department.Department;
import com.doc.entity.user.User;
import com.doc.entity.user.UserProductMap;
import com.doc.entity.vendor.ProcurementMilestoneAssignment;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.notification.*;
import com.doc.repository.*;
import com.doc.repository.documentRepo.ProjectDocumentUploadRepository;
import com.doc.repository.projectRepo.ProjectStatusRepository;
import com.doc.service.AutoAssignmentService;
import com.doc.service.NotificationPublisherService;
import com.doc.service.ProjectMilestoneAssignmentService;
import com.doc.service.ProjectService;
import com.doc.validation.MilestoneValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ProjectMilestoneAssignmentServiceImpl implements ProjectMilestoneAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectMilestoneAssignmentServiceImpl.class);

    @Autowired private ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectDocumentUploadRepository projectDocumentUploadRepository;
    @Autowired private MilestoneStatusHistoryRepository milestoneStatusHistoryRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectAssignmentHistoryRepository projectAssignmentHistoryRepository;
    @Autowired private UserProductMapRepository userProductMapRepository;
    @Autowired private UserPerformanceCountRepository userPerformanceCountRepository;
    @Autowired private MilestoneStatusRepository milestoneStatusRepository;
    @Autowired private ProjectStatusRepository projectStatusRepository;
    @Autowired private AutoAssignmentService autoAssignmentService;
    @Autowired private MilestoneValidator milestoneValidator;

    @Lazy
    @Autowired private ProjectService projectService;

    @Autowired
    private ProcurementMilestoneAssignmentRepository procurementMilestoneAssignmentRepository;
    @Autowired
    private NotificationPublisherService notificationPublisherService;


    @Override
    public void updateMilestoneStatus(UpdateMilestoneStatusDto updateDto) {

        logger.info("Updating milestone assignment ID: {} to status: {} by user ID: {}",
                updateDto.getAssignmentId(),
                updateDto.getNewStatusName(),
                updateDto.getChangedById());

        ProjectMilestoneAssignment assignment =
                projectMilestoneAssignmentRepository.findActiveUserById(updateDto.getAssignmentId())
                        .orElseThrow(() -> {
                            logger.error("Milestone assignment ID {} not found or is deleted",
                                    updateDto.getAssignmentId());
                            return new ResourceNotFoundException(
                                    "Milestone assignment not found",
                                    "MILESTONE_ASSIGNMENT_NOT_FOUND"
                            );
                        });

        User changedBy =
                userRepository.findActiveUserById(updateDto.getChangedById())
                        .orElseThrow(() -> {
                            logger.error("User ID {} not found or is deleted",
                                    updateDto.getChangedById());
                            return new ResourceNotFoundException(
                                    "User not found",
                                    "USER_NOT_FOUND"
                            );
                        });

        boolean isAdmin = changedBy.getRoles()
                .stream()
                .anyMatch(r -> "ADMIN".equals(r.getName()));

        boolean isOperationHead = changedBy.getRoles()
                .stream()
                .anyMatch(r -> "OPERATION_HEAD".equals(r.getName()));

        boolean isManager = changedBy.isManagerFlag();

        // Manager can update only milestones of his/her department
        if (isManager && !isAdmin && !isOperationHead) {
            if (!isManagerOfMilestoneDepartment(changedBy, assignment)) {
                logger.warn("Manager ID {} attempted to update milestone {} from another department",
                        changedBy.getId(),
                        assignment.getId());

                throw new ValidationException(
                        "You can only update milestones that belong to your department(s)",
                        "MANAGER_DEPARTMENT_MISMATCH"
                );
            }
        }

        // Only ADMIN, OPERATION_HEAD, MANAGER can update milestone status
        if (!isAdmin && !isOperationHead && !isManager) {
            logger.warn("User ID {} is not authorized to update milestone status",
                    changedBy.getId());

            throw new ValidationException(
                    "Only ADMIN, OPERATION_HEAD, or MANAGER can update milestone status",
                    "NOT_AUTHORIZED"
            );
        }

        MilestoneStatus newStatus =
                milestoneStatusRepository.findByName(updateDto.getNewStatusName())
                        .orElseThrow(() -> {
                            logger.error("Milestone status {} not found",
                                    updateDto.getNewStatusName());
                            return new ResourceNotFoundException(
                                    "Milestone status not found",
                                    "STATUS_NOT_FOUND"
                            );
                        });

        validateMilestoneStatusTransition(
                assignment,
                newStatus,
                updateDto.getStatusReason()
        );

        /*
         * Business validation before marking COMPLETED
         */
        if ("COMPLETED".equalsIgnoreCase(newStatus.getName())) {

            String milestoneName = assignment
                    .getProductMilestoneMap()
                    .getMilestone()
                    .getName();

            if ("Documentation".equalsIgnoreCase(milestoneName)) {
                milestoneValidator.validateDocumentMilestone(assignment);
            }

            if ("Legal Verfication".equalsIgnoreCase(milestoneName)) {
                milestoneValidator.validateLegalMilestone(assignment);
            }

            if ("Filling".equalsIgnoreCase(milestoneName)) {
                milestoneValidator.validateFillingMilestone(assignment);
            }

            if ("Procurement".equalsIgnoreCase(milestoneName)) {
                validateProcurementMilestoneBeforeCompletion(assignment);
            }
        }

        /*
         * If milestone is completed:
         * 1. Reduce old user's active assignment count
         * 2. Add time spent
         * 3. Mark user-product map as unassigned
         */
        if ("COMPLETED".equalsIgnoreCase(newStatus.getName())) {

            if (assignment.getAssignedUser() != null) {

                User oldUser = assignment.getAssignedUser();

                UserPerformanceCount count =
                        userPerformanceCountRepository.findByUserIdAndProductId(
                                oldUser.getId(),
                                assignment.getProject().getProduct().getId()
                        );

                if (count != null) {
                    count.setTimeSpent(
                            count.getTimeSpent()
                                    + assignment.getProductMilestoneMap().getTatInDays()
                    );
                    count.setAssignmentCount(
                            Math.max(0, count.getAssignmentCount() - 1)
                    );
                    count.setLastUpdatedDate(new Date());
                    count.setUpdatedDate(new Date());
                    count.setUpdatedBy(updateDto.getChangedById());

                    userPerformanceCountRepository.save(count);
                }

                UserProductMap userMap =
                        userProductMapRepository
                                .findByUserIdAndProductIdAndIsDeletedFalse(
                                        oldUser.getId(),
                                        assignment.getProject().getProduct().getId()
                                )
                                .orElse(null);

                if (userMap != null) {
                    userMap.setAssigned(false);
                    userProductMapRepository.save(userMap);
                }
            }
        }

        /*
         * Rollback handling: REJECTED -> NEW
         */
        if ("NEW".equalsIgnoreCase(newStatus.getName())
                && "REJECTED".equalsIgnoreCase(assignment.getStatus().getName())) {

            if (!assignment.getProductMilestoneMap().isAllowRollback()) {
                throw new ValidationException(
                        "Rollback not allowed for this milestone",
                        "ROLLBACK_NOT_ALLOWED"
                );
            }

            if (assignment.getReworkAttempts()
                    >= assignment.getProductMilestoneMap().getMaxAttempts()) {
                throw new ValidationException(
                        "Maximum rework attempts reached",
                        "MAX_REWORK_ATTEMPTS_REACHED"
                );
            }

            assignment.setReworkAttempts(assignment.getReworkAttempts() + 1);
        }

        /*
         * Save status history before changing assignment status
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

        /*
         * Update assignment status
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

        assignment = projectMilestoneAssignmentRepository.save(assignment);

        logger.info("Milestone assignment ID {} status updated to {} by user {}",
                updateDto.getAssignmentId(),
                newStatus.getName(),
                changedBy.getFullName());

        Project project = assignment.getProject();

        /*
         * IMPORTANT FIX:
         * After completing one milestone, recalculate visibility of all project milestones.
         * This will make the next milestone visible if payment and previous milestone conditions are satisfied.
         */
        if ("COMPLETED".equalsIgnoreCase(newStatus.getName())) {
            projectService.updateMilestoneVisibilities(
                    project,
                    updateDto.getChangedById()
            );

            logger.info("Milestone visibility recalculated after completion. Project ID: {}",
                    project.getId());
        }

        /*
         * Update project status after milestone status/visibility update
         */
        updateProjectStatus(project, updateDto.getChangedById());
    }
    private void validateProcurementMilestoneBeforeCompletion(ProjectMilestoneAssignment assignment) {

        ProcurementMilestoneAssignment procurementAssignment =
                procurementMilestoneAssignmentRepository
                        .findByProjectIdAndMilestoneIdAndIsDeletedFalse(
                                assignment.getProject().getId(),
                                assignment.getMilestone().getId()
                        )
                        .orElseThrow(() -> new ValidationException(
                                "Procurement assignment is not created for this milestone",
                                "ERR_PROCUREMENT_ASSIGNMENT_NOT_FOUND"
                        ));

        if (procurementAssignment.getSelectedVendor() == null) {
            throw new ValidationException(
                    "Please select vendor before completing Procurement milestone",
                    "ERR_VENDOR_NOT_SELECTED"
            );
        }

        ProcurementStatus currentProcurementStatus = procurementAssignment.getStatus();

        if (currentProcurementStatus == ProcurementStatus.VENDOR_REQUIRED) {
            throw new ValidationException(
                    "No vendor available for this service. Please create vendor and map it with this service.",
                    "ERR_VENDOR_REQUIRED"
            );
        }

        if (!isProcurementEligibleForMilestoneCompletion(currentProcurementStatus)) {
            throw new ValidationException(
                    "Procurement milestone cannot be completed until vendor is finalized",
                    "ERR_INVALID_PROCUREMENT_STATUS"
            );
        }
    }

    private boolean isProcurementEligibleForMilestoneCompletion(ProcurementStatus status) {
        return List.of(
                ProcurementStatus.VENDOR_FINALIZED,
                ProcurementStatus.PO_CREATED,
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
            return;
        }

        if (assignedUser == null || assignedUser.getId() == null) {
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
                ? "You have been reassigned to milestone \"" + milestoneName + "\" for project \"" + projectName + "\" by " + actorName + "."
                : "You have been assigned to milestone \"" + milestoneName + "\" for project \"" + projectName + "\".";

        notificationPublisherService.sendNotification(
                NotificationCreateRequestDto.builder()
                        .receiverId(assignedUser.getId())
                        .actorId(actor != null ? actor.getId() : null)
                        .actorName(actorName)
                        .module(NotificationModule.PROJECT)
                        .eventType(eventType)
                        .referenceId(project != null ? project.getId() : assignment.getId())
                        .referenceNumber(projectNumber)
                        .title(title)
                        .message(message)
                        .redirectUrl("/projects/" + (project != null ? project.getId() : "") + "/milestones/" + assignment.getId())
                        .priority(NotificationPriority.HIGH)
                        .displayType(NotificationDisplayType.INFO)
                        .metadataJson(
                                "{"
                                        + "\"projectId\":" + (project != null ? project.getId() : null) + ","
                                        + "\"projectName\":\"" + escapeJson(projectName) + "\","
                                        + "\"projectNumber\":\"" + escapeJson(projectNumber) + "\","
                                        + "\"milestoneAssignmentId\":" + assignment.getId() + ","
                                        + "\"milestoneName\":\"" + escapeJson(milestoneName) + "\","
                                        + "\"assignedUserId\":" + assignedUser.getId() + ","
                                        + "\"assignedUserName\":\"" + escapeJson(getUserDisplayName(assignedUser)) + "\","
                                        + "\"assignedById\":" + (actor != null ? actor.getId() : null) + ","
                                        + "\"assignedByName\":\"" + escapeJson(actorName) + "\","
                                        + "\"reason\":\"" + escapeJson(reason) + "\","
                                        + "\"reassigned\":" + reassigned
                                        + "}"
                        )
                        .build()
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

    private void validateMilestoneStatusTransition(ProjectMilestoneAssignment assignment, MilestoneStatus newStatus, String reason) {
        String current = assignment.getStatus().getName();
        String next = newStatus.getName();

        System.out.println("current: "+current);
        System.out.println("next: "+next);

        if (current.equals(next)) {
            throw new ValidationException("Milestone is already in status: " + next, "SAME_STATUS");
        }

        if (reason == null || reason.trim().isEmpty()) {
            if (List.of("COMPLETED", "REJECTED", "ON_HOLD").contains(next)) {
                throw new ValidationException("Reason is required for status: " + next, "REASON_REQUIRED");
            }
        }

        switch (current) {
            case "NEW", "MANUAL_PENDING" -> {
                if (!List.of("IN_PROGRESS", "ON_HOLD").contains(next)) {
                    throw new ValidationException("From " + current + " → only IN_PROGRESS or ON_HOLD allowed", "INVALID_TRANSITION");
                }
            }
            case "IN_PROGRESS" -> {
                if (!List.of("COMPLETED", "ON_HOLD", "REJECTED").contains(next)) {
                    throw new ValidationException("From IN_PROGRESS → only COMPLETED, ON_HOLD, REJECTED allowed", "INVALID_TRANSITION");
                }
            }
            case "ON_HOLD" -> {
                if (!"IN_PROGRESS".equals(next)) {
                    throw new ValidationException("From ON_HOLD → only IN_PROGRESS allowed", "INVALID_TRANSITION");
                }
            }
            case "REJECTED" -> {
                if (!"NEW".equals(next)) {
                    throw new ValidationException("From REJECTED → only NEW allowed", "INVALID_TRANSITION");
                }
            }
            case "REWORK" -> {
                if (!"COMPLETED".equals(next)) {
                    throw new ValidationException("From REJECTED → only COMPLETED allowed", "INVALID_TRANSITION");
                }
            }
            case "COMPLETED" -> throw new ValidationException("Cannot change status from COMPLETED", "COMPLETED_FINAL");
            default -> throw new ValidationException("Invalid current status: " + current, "INVALID_CURRENT_STATUS");
        }

        if (!assignment.isVisible() && !"NEW".equals(next)) {
            throw new ValidationException("Milestone must be visible to change status", "MILESTONE_NOT_VISIBLE");
        }
    }

    private void updateProjectStatus(Project project, Long updatedById) {
        List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository
                .findByProjectIdAndIsDeletedFalse(project.getId());

        String newStatusName;
        if (assignments.isEmpty()) {
            newStatusName = "OPEN";
        } else if (assignments.stream().allMatch(a -> "COMPLETED".equals(a.getStatus().getName()))) {
            newStatusName = "COMPLETED";
        } else if (assignments.stream().anyMatch(a -> List.of("IN_PROGRESS", "ON_HOLD").contains(a.getStatus().getName()))) {
            newStatusName = "IN_PROGRESS";
        } else {
            newStatusName = "OPEN";
        }

        ProjectStatus status = projectStatusRepository.findByName(newStatusName)
                .orElseThrow(() -> new ResourceNotFoundException("Project status not found: " + newStatusName, "STATUS_NOT_FOUND"));

        project.setStatus(status);
        project.setUpdatedBy(updatedById);
        project.setUpdatedDate(new Date());
        projectRepository.save(project);
    }
}