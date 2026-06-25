package com.doc.impl;

import com.doc.dto.project.reopen.ProjectReopenCreateRequestDto;
import com.doc.dto.project.reopen.ProjectReopenDecisionDto;
import com.doc.dto.project.reopen.ProjectReopenRequestResponseDto;
import com.doc.em.ProjectReopenRequestStatus;
import com.doc.entity.department.Department;
import com.doc.entity.milestone.MilestoneStatus;
import com.doc.entity.milestone.MilestoneStatusHistory;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.project.ProjectReopenRequest;
import com.doc.entity.project.ProjectStatus;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.MilestoneStatusHistoryRepository;
import com.doc.repository.MilestoneStatusRepository;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.repository.ProjectReopenRequestRepository;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.projectRepo.ProjectStatusRepository;
import com.doc.service.ProjectReopenRequestService;
import com.doc.service.ProjectService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ProjectReopenRequestServiceImpl implements ProjectReopenRequestService {

    private static final Logger logger = LogManager.getLogger(ProjectReopenRequestServiceImpl.class);

    private final ProjectReopenRequestRepository projectReopenRequestRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;
    private final UserRepository userRepository;
    private final MilestoneStatusRepository milestoneStatusRepository;
    private final ProjectStatusRepository projectStatusRepository;
    private final MilestoneStatusHistoryRepository milestoneStatusHistoryRepository;
    private final ProjectService projectService;

    public ProjectReopenRequestServiceImpl(
            ProjectReopenRequestRepository projectReopenRequestRepository,
            ProjectRepository projectRepository,
            ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository,
            UserRepository userRepository,
            MilestoneStatusRepository milestoneStatusRepository,
            ProjectStatusRepository projectStatusRepository,
            MilestoneStatusHistoryRepository milestoneStatusHistoryRepository,
            @Lazy ProjectService projectService
    ) {
        this.projectReopenRequestRepository = projectReopenRequestRepository;
        this.projectRepository = projectRepository;
        this.projectMilestoneAssignmentRepository = projectMilestoneAssignmentRepository;
        this.userRepository = userRepository;
        this.milestoneStatusRepository = milestoneStatusRepository;
        this.projectStatusRepository = projectStatusRepository;
        this.milestoneStatusHistoryRepository = milestoneStatusHistoryRepository;
        this.projectService = projectService;
    }

    @Override
    public ProjectReopenRequestResponseDto createReopenRequest(ProjectReopenCreateRequestDto dto) {
        logger.info("Creating reopen request for projectId: {}, detectedAtAssignmentId: {}, responsibleAssignmentId: {}",
                dto.getProjectId(), dto.getDetectedAtAssignmentId(), dto.getResponsibleAssignmentId());

        Project project = projectRepository.findActiveUserById(dto.getProjectId())
                .orElseThrow(() -> {
                    logger.error("Project not found with id: {}", dto.getProjectId());
                    return new ResourceNotFoundException("Project not found", "PROJECT_NOT_FOUND");
                });

        ProjectMilestoneAssignment detectedAtAssignment =
                projectMilestoneAssignmentRepository.findActiveUserById(dto.getDetectedAtAssignmentId())
                        .orElseThrow(() -> {
                            logger.error("Detected milestone assignment not found with id: {}", dto.getDetectedAtAssignmentId());
                            return new ResourceNotFoundException("Detected milestone assignment not found", "DETECTED_ASSIGNMENT_NOT_FOUND");
                        });

        ProjectMilestoneAssignment responsibleAssignment =
                projectMilestoneAssignmentRepository.findActiveUserById(dto.getResponsibleAssignmentId())
                        .orElseThrow(() -> {
                            logger.error("Responsible milestone assignment not found with id: {}", dto.getResponsibleAssignmentId());
                            return new ResourceNotFoundException("Responsible milestone assignment not found", "RESPONSIBLE_ASSIGNMENT_NOT_FOUND");
                        });

        logger.debug("Validating assignments belong to project");
        validateAssignmentBelongsToProject(detectedAtAssignment, project);
        validateAssignmentBelongsToProject(responsibleAssignment, project);

        if (detectedAtAssignment.getId().equals(responsibleAssignment.getId())) {
            logger.warn("Detected and responsible assignment are the same for projectId: {}", dto.getProjectId());
            throw new ValidationException("Detected assignment and responsible assignment cannot be same", "INVALID_REOPEN_ASSIGNMENT_SELECTION");
        }

        if (responsibleAssignment.getStatus() == null || !"COMPLETED".equalsIgnoreCase(responsibleAssignment.getStatus().getName())) {
            logger.warn("Responsible milestone is not COMPLETED for assignmentId: {}", responsibleAssignment.getId());
            throw new ValidationException("Responsible milestone must be COMPLETED before project can be reopened", "RESPONSIBLE_MILESTONE_NOT_COMPLETED");
        }

        User requestedBy = detectedAtAssignment.getAssignedUser();
        if (requestedBy == null) {
            logger.error("Detected milestone has no assigned user for assignmentId: {}", detectedAtAssignment.getId());
            throw new ValidationException("Detected milestone does not have assigned user", "DETECTED_ASSIGNMENT_USER_NOT_FOUND");
        }

        if (requestedBy.isDeleted() || !requestedBy.isActive()) {
            logger.warn("RequestedBy user is not active/deleted. userId: {}", requestedBy.getId());
            throw new ValidationException("Detected milestone assigned user is not active", "REQUESTED_BY_USER_NOT_ACTIVE");
        }

        User requesterManager = requestedBy.getManager();
        if (requesterManager == null) {
            logger.error("Requester has no manager mapped. userId: {}", requestedBy.getId());
            throw new ValidationException("Requester user does not have manager mapped", "REQUESTER_MANAGER_NOT_MAPPED");
        }

        logger.debug("Validating requester manager");
        validateManager(requesterManager, "Requester manager");

        User responsibleManager = resolveResponsibleManager(responsibleAssignment);
        logger.debug("Resolved responsible manager: {}", responsibleManager.getId());

        validateManager(responsibleManager, "Responsible manager");

        Collection<ProjectReopenRequestStatus> pendingStatuses = List.of(
                ProjectReopenRequestStatus.PENDING_REQUESTER_MANAGER_APPROVAL,
                ProjectReopenRequestStatus.PENDING_RESPONSIBLE_MANAGER_APPROVAL
        );

        boolean pendingExists = projectReopenRequestRepository.existsByProjectIdAndStatusInAndIsDeletedFalse(
                project.getId(), pendingStatuses);

        if (pendingExists) {
            logger.warn("Pending reopen request already exists for projectId: {}", project.getId());
            throw new ValidationException("A reopen request is already pending for this project", "REOPEN_REQUEST_ALREADY_PENDING");
        }

        ProjectReopenRequest request = new ProjectReopenRequest();
        request.setProject(project);
        request.setDetectedAtAssignment(detectedAtAssignment);
        request.setResponsibleAssignment(responsibleAssignment);

        request.setRequesterDepartment(getFirstDepartment(detectedAtAssignment));
        request.setResponsibleDepartment(getFirstDepartment(responsibleAssignment));

        request.setRequestedBy(requestedBy);
        request.setRequesterManager(requesterManager);
        request.setResponsibleManager(responsibleManager);

        request.setRequestReason(dto.getReason().trim());
        request.setStatus(ProjectReopenRequestStatus.PENDING_REQUESTER_MANAGER_APPROVAL);

        request.setRequestedAt(new Date());
        request.setCreatedBy(requestedBy.getId());
        request.setUpdatedBy(requestedBy.getId());
        request.setCreatedDate(new Date());
        request.setUpdatedDate(new Date());
        request.setDeleted(false);

        ProjectReopenRequest saved = projectReopenRequestRepository.save(request);
        logger.info("Reopen request created successfully. requestId: {}", saved.getId());

        return mapToResponseDto(saved);
    }

    private User resolveResponsibleManager(ProjectMilestoneAssignment responsibleAssignment) {
        logger.debug("Resolving responsible manager for assignmentId: {}", responsibleAssignment.getId());

        if (responsibleAssignment.getAssignedUser() != null) {
            User responsibleUser = responsibleAssignment.getAssignedUser();
            if (responsibleUser.getManager() != null) {
                User manager = responsibleUser.getManager();
                if (!manager.isDeleted() && manager.isActive() && manager.isManagerFlag()) {
                    logger.debug("Found manager from responsible user: {}", manager.getId());
                    return manager;
                }
            }
        }

        Department responsibleDepartment = getFirstDepartment(responsibleAssignment);
        if (responsibleDepartment == null || responsibleDepartment.getId() == null) {
            logger.error("Responsible milestone department not found for assignmentId: {}", responsibleAssignment.getId());
            throw new ValidationException("Responsible milestone department not found", "RESPONSIBLE_DEPARTMENT_NOT_FOUND");
        }

        List<User> managers = userRepository.findActiveManagersByDepartmentId(responsibleDepartment.getId());
        if (managers == null || managers.isEmpty()) {
            logger.error("No active manager found for department: {}", responsibleDepartment.getName());
            throw new ValidationException("No active manager found for responsible department: " + responsibleDepartment.getName(), "RESPONSIBLE_MANAGER_NOT_FOUND");
        }

        logger.debug("Selected responsible manager from department: {}", managers.get(0).getId());
        return managers.get(0);
    }

    @Override
    public ProjectReopenRequestResponseDto requesterManagerDecision(Long requestId, ProjectReopenDecisionDto dto) {
        logger.info("Requester manager decision for requestId: {}, decision: {}", requestId, dto.getDecision());

        ProjectReopenRequest request = getActiveRequest(requestId);

        if (request.getStatus() != ProjectReopenRequestStatus.PENDING_REQUESTER_MANAGER_APPROVAL) {
            logger.warn("Invalid status for requester decision. Current status: {}", request.getStatus());
            throw new ValidationException("Request is not pending requester manager approval", "INVALID_REOPEN_REQUEST_STATUS");
        }

        User actionBy = userRepository.findActiveUserById(dto.getActionById())
                .orElseThrow(() -> {
                    logger.error("Action user not found: {}", dto.getActionById());
                    return new ResourceNotFoundException("Action user not found", "USER_NOT_FOUND");
                });

        if (!request.getRequesterManager().getId().equals(actionBy.getId())) {
            logger.warn("Unauthorized requester manager action. actionBy: {}, requesterManager: {}", dto.getActionById(), request.getRequesterManager().getId());
            throw new ValidationException("Only requester manager can approve or reject this step", "NOT_REQUESTER_MANAGER");
        }

        String decision = normalizeDecision(dto.getDecision());

        request.setRequesterManagerRemarks(dto.getRemarks());
        request.setRequesterManagerActionAt(new Date());
        request.setUpdatedBy(dto.getActionById());
        request.setUpdatedDate(new Date());

        if ("REJECT".equals(decision)) {
            request.setStatus(ProjectReopenRequestStatus.REJECTED);
            logger.info("Reopen request rejected by requester manager. requestId: {}", requestId);
            return mapToResponseDto(projectReopenRequestRepository.save(request));
        }

        if (request.getRequesterManager().getId().equals(request.getResponsibleManager().getId())) {
            logger.info("Auto-approving responsible manager step as managers are same. requestId: {}", requestId);
            request.setResponsibleManagerRemarks("Auto-approved because requester manager and responsible manager are same.");
            request.setResponsibleManagerActionAt(new Date());
            request.setStatus(ProjectReopenRequestStatus.APPROVED);

            reopenProject(request, dto.getActionById());
            return mapToResponseDto(projectReopenRequestRepository.save(request));
        }

        request.setStatus(ProjectReopenRequestStatus.PENDING_RESPONSIBLE_MANAGER_APPROVAL);
        logger.info("Reopen request moved to responsible manager approval. requestId: {}", requestId);

        return mapToResponseDto(projectReopenRequestRepository.save(request));
    }

    @Override
    public ProjectReopenRequestResponseDto responsibleManagerDecision(Long requestId, ProjectReopenDecisionDto dto) {
        logger.info("Responsible manager decision for requestId: {}, decision: {}", requestId, dto.getDecision());

        ProjectReopenRequest request = getActiveRequest(requestId);

        if (request.getStatus() != ProjectReopenRequestStatus.PENDING_RESPONSIBLE_MANAGER_APPROVAL) {
            logger.warn("Invalid status for responsible decision. Current status: {}", request.getStatus());
            throw new ValidationException("Request is not pending responsible manager approval", "INVALID_REOPEN_REQUEST_STATUS");
        }

        User actionBy = userRepository.findActiveUserById(dto.getActionById())
                .orElseThrow(() -> {
                    logger.error("Action user not found: {}", dto.getActionById());
                    return new ResourceNotFoundException("Action user not found", "USER_NOT_FOUND");
                });

        if (!request.getResponsibleManager().getId().equals(actionBy.getId())) {
            logger.warn("Unauthorized responsible manager action. actionBy: {}, responsibleManager: {}", dto.getActionById(), request.getResponsibleManager().getId());
            throw new ValidationException("Only responsible manager can approve or reject this step", "NOT_RESPONSIBLE_MANAGER");
        }

        String decision = normalizeDecision(dto.getDecision());

        request.setResponsibleManagerRemarks(dto.getRemarks());
        request.setResponsibleManagerActionAt(new Date());
        request.setUpdatedBy(dto.getActionById());
        request.setUpdatedDate(new Date());

        if ("REJECT".equals(decision)) {
            request.setStatus(ProjectReopenRequestStatus.REJECTED);
            logger.info("Reopen request rejected by responsible manager. requestId: {}", requestId);
            return mapToResponseDto(projectReopenRequestRepository.save(request));
        }

        request.setStatus(ProjectReopenRequestStatus.APPROVED);
        logger.info("Reopen request approved. Reopening project. requestId: {}", requestId);

        reopenProject(request, dto.getActionById());

        return mapToResponseDto(projectReopenRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectReopenRequestResponseDto> getRequesterManagerPendingRequests(Long managerId) {
        logger.debug("Fetching pending requester manager requests for managerId: {}", managerId);
        return projectReopenRequestRepository
                .findByRequesterManagerIdAndStatusAndIsDeletedFalse(
                        managerId,
                        ProjectReopenRequestStatus.PENDING_REQUESTER_MANAGER_APPROVAL
                )
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectReopenRequestResponseDto> getResponsibleManagerPendingRequests(Long managerId) {
        logger.debug("Fetching pending responsible manager requests for managerId: {}", managerId);
        return projectReopenRequestRepository
                .findByResponsibleManagerIdAndStatusAndIsDeletedFalse(
                        managerId,
                        ProjectReopenRequestStatus.PENDING_RESPONSIBLE_MANAGER_APPROVAL
                )
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectReopenRequestResponseDto> getProjectReopenRequests(Long projectId) {
        logger.debug("Fetching all reopen requests for projectId: {}", projectId);
        return projectReopenRequestRepository
                .findByProjectIdAndIsDeletedFalseOrderByCreatedDateDesc(projectId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    private void reopenProject(ProjectReopenRequest request, Long actionById) {
        logger.info("Reopening project for requestId: {}", request.getId());

        Project project = request.getProject();

        MilestoneStatus newStatus = milestoneStatusRepository.findByName("NEW")
                .orElseThrow(() -> {
                    logger.error("Milestone status NEW not found");
                    return new ResourceNotFoundException("Milestone status NEW not found", "STATUS_NOT_FOUND");
                });

        ProjectStatus reopenedStatus = projectStatusRepository.findByName("REOPENED")
                .orElseThrow(() -> {
                    logger.error("Project status REOPENED not found");
                    return new ResourceNotFoundException("Project status REOPENED not found", "STATUS_NOT_FOUND");
                });

        User changedBy = userRepository.findActiveUserById(actionById)
                .orElseThrow(() -> {
                    logger.error("Action user not found for reopen: {}", actionById);
                    return new ResourceNotFoundException("Action user not found", "USER_NOT_FOUND");
                });

        List<ProjectMilestoneAssignment> assignments =
                projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());

        String reason = "Project reopened due to approved reopen request ID: "
                + request.getId()
                + ". Reason: "
                + request.getRequestReason();

        logger.debug("Resetting {} milestone assignments", assignments.size());

        for (ProjectMilestoneAssignment assignment : assignments) {
            if (assignment.getStatus() != null && "NEW".equalsIgnoreCase(assignment.getStatus().getName())) {
                continue;
            }

            saveMilestoneStatusHistory(
                    assignment,
                    assignment.getStatus(),
                    newStatus,
                    reason,
                    changedBy
            );

            assignment.setStatus(newStatus);
            assignment.setStatusReason(reason);
            assignment.setStartedDate(null);
            assignment.setCompletedDate(null);
            assignment.setVisible(false);
            assignment.setVisibilityReason("Hidden because project was reopened. Visibility will be recalculated.");
            assignment.setUpdatedBy(actionById);
            assignment.setUpdatedDate(new Date());

            projectMilestoneAssignmentRepository.save(assignment);
        }

        project.setStatus(reopenedStatus);
        project.setUpdatedBy(actionById);
        project.setUpdatedDate(new Date());

        projectRepository.save(project);

        logger.info("Project reopened successfully. projectId: {}", project.getId());
        projectService.updateMilestoneVisibilities(project, actionById);
    }

    private void saveMilestoneStatusHistory(
            ProjectMilestoneAssignment assignment,
            MilestoneStatus previousStatus,
            MilestoneStatus newStatus,
            String reason,
            User changedBy
    ) {
        logger.debug("Saving milestone status history for assignmentId: {}", assignment.getId());
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

    private ProjectReopenRequest getActiveRequest(Long requestId) {
        logger.debug("Fetching active reopen request: {}", requestId);
        return projectReopenRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> {
                    logger.error("Project reopen request not found: {}", requestId);
                    return new ResourceNotFoundException("Project reopen request not found", "PROJECT_REOPEN_REQUEST_NOT_FOUND");
                });
    }

    private void validateAssignmentBelongsToProject(ProjectMilestoneAssignment assignment, Project project) {
        if (assignment.getProject() == null || !assignment.getProject().getId().equals(project.getId())) {
            logger.error("Assignment project mismatch. assignmentProjectId: {}, expectedProjectId: {}",
                    assignment.getProject() != null ? assignment.getProject().getId() : null, project.getId());
            throw new ValidationException("Milestone assignment does not belong to this project", "ASSIGNMENT_PROJECT_MISMATCH");
        }
    }

    private void validateManager(User manager, String label) {
        if (!manager.isManagerFlag()) {
            logger.error("{} is not a manager. userId: {}", label, manager.getId());
            throw new ValidationException(label + " must be a manager", "USER_IS_NOT_MANAGER");
        }
    }

    private String normalizeDecision(String decision) {
        if (decision == null || decision.trim().isEmpty()) {
            logger.error("Decision is empty or null");
            throw new ValidationException("Decision is required", "INVALID_DECISION");
        }

        String value = decision.trim().toUpperCase();

        if (!List.of("APPROVE", "REJECT").contains(value)) {
            logger.error("Invalid decision value: {}", decision);
            throw new ValidationException("Decision must be APPROVE or REJECT", "INVALID_DECISION");
        }

        return value;
    }

    private Department getFirstDepartment(ProjectMilestoneAssignment assignment) {
        if (assignment == null ||
                assignment.getProductMilestoneMap() == null ||
                assignment.getProductMilestoneMap().getMilestone() == null ||
                assignment.getProductMilestoneMap().getMilestone().getDepartments() == null ||
                assignment.getProductMilestoneMap().getMilestone().getDepartments().isEmpty()) {
            logger.debug("No department found for assignmentId: {}", assignment != null ? assignment.getId() : null);
            return null;
        }

        return assignment.getProductMilestoneMap()
                .getMilestone()
                .getDepartments()
                .get(0);
    }

    private ProjectReopenRequestResponseDto mapToResponseDto(ProjectReopenRequest request) {
        logger.debug("Mapping reopen request to response DTO. requestId: {}", request.getId());
        ProjectReopenRequestResponseDto dto = new ProjectReopenRequestResponseDto();

        dto.setId(request.getId());

        Project project = request.getProject();
        if (project != null) {
            dto.setProjectId(project.getId());
            dto.setProjectName(project.getName());
            dto.setProjectNo(project.getProjectNo());
        }

        ProjectMilestoneAssignment detected = request.getDetectedAtAssignment();
        if (detected != null) {
            dto.setDetectedAtAssignmentId(detected.getId());
            dto.setDetectedAtMilestoneName(getMilestoneName(detected));
        }

        ProjectMilestoneAssignment responsible = request.getResponsibleAssignment();
        if (responsible != null) {
            dto.setResponsibleAssignmentId(responsible.getId());
            dto.setResponsibleMilestoneName(getMilestoneName(responsible));
        }

        User requestedBy = request.getRequestedBy();
        if (requestedBy != null) {
            dto.setRequestedById(requestedBy.getId());
            dto.setRequestedByName(getUserDisplayName(requestedBy));
        }

        User requesterManager = request.getRequesterManager();
        if (requesterManager != null) {
            dto.setRequesterManagerId(requesterManager.getId());
            dto.setRequesterManagerName(getUserDisplayName(requesterManager));
        }

        User responsibleManager = request.getResponsibleManager();
        if (responsibleManager != null) {
            dto.setResponsibleManagerId(responsibleManager.getId());
            dto.setResponsibleManagerName(getUserDisplayName(responsibleManager));
        }

        dto.setRequestReason(request.getRequestReason());
        dto.setRequesterManagerRemarks(request.getRequesterManagerRemarks());
        dto.setResponsibleManagerRemarks(request.getResponsibleManagerRemarks());

        dto.setStatus(request.getStatus());

        dto.setRequestedAt(request.getRequestedAt());
        dto.setRequesterManagerActionAt(request.getRequesterManagerActionAt());
        dto.setResponsibleManagerActionAt(request.getResponsibleManagerActionAt());

        dto.setCreatedDate(request.getCreatedDate());
        dto.setUpdatedDate(request.getUpdatedDate());

        return dto;
    }

    private String getMilestoneName(ProjectMilestoneAssignment assignment) {
        if (assignment == null) {
            return "Milestone";
        }

        if (assignment.getMilestone() != null &&
                assignment.getMilestone().getName() != null &&
                !assignment.getMilestone().getName().trim().isEmpty()) {
            return assignment.getMilestone().getName().trim();
        }

        if (assignment.getProductMilestoneMap() != null &&
                assignment.getProductMilestoneMap().getMilestone() != null &&
                assignment.getProductMilestoneMap().getMilestone().getName() != null &&
                !assignment.getProductMilestoneMap().getMilestone().getName().trim().isEmpty()) {
            return assignment.getProductMilestoneMap().getMilestone().getName().trim();
        }

        return "Milestone-" + assignment.getId();
    }

    private String getUserDisplayName(User user) {
        if (user == null) {
            return "User";
        }

        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            return user.getFullName().trim();
        }

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail().trim();
        }

        return "User-" + user.getId();
    }
}