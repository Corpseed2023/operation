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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ProjectReopenRequestServiceImpl implements ProjectReopenRequestService {

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

        Project project = projectRepository.findActiveUserById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "PROJECT_NOT_FOUND"
                ));

        ProjectMilestoneAssignment detectedAtAssignment =
                projectMilestoneAssignmentRepository.findActiveUserById(dto.getDetectedAtAssignmentId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Detected milestone assignment not found",
                                "DETECTED_ASSIGNMENT_NOT_FOUND"
                        ));

        ProjectMilestoneAssignment responsibleAssignment =
                projectMilestoneAssignmentRepository.findActiveUserById(dto.getResponsibleAssignmentId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Responsible milestone assignment not found",
                                "RESPONSIBLE_ASSIGNMENT_NOT_FOUND"
                        ));

        validateAssignmentBelongsToProject(detectedAtAssignment, project);
        validateAssignmentBelongsToProject(responsibleAssignment, project);

        if (detectedAtAssignment.getId().equals(responsibleAssignment.getId())) {
            throw new ValidationException(
                    "Detected assignment and responsible assignment cannot be same",
                    "INVALID_REOPEN_ASSIGNMENT_SELECTION"
            );
        }

        /*
         * Project reopen means mistake is found after responsible milestone was completed.
         *
         * Example:
         * Filing / Technical = COMPLETED
         * Liaison / Certification finds mistake later.
         */
        if (responsibleAssignment.getStatus() == null ||
                !"COMPLETED".equalsIgnoreCase(responsibleAssignment.getStatus().getName())) {
            throw new ValidationException(
                    "Responsible milestone must be COMPLETED before project can be reopened",
                    "RESPONSIBLE_MILESTONE_NOT_COMPLETED"
            );
        }

        User requestedBy = userRepository.findActiveUserById(dto.getRequestedById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requested by user not found",
                        "USER_NOT_FOUND"
                ));

        User requesterManager = userRepository.findActiveUserById(dto.getRequesterManagerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requester manager not found",
                        "REQUESTER_MANAGER_NOT_FOUND"
                ));

        User responsibleManager = userRepository.findActiveUserById(dto.getResponsibleManagerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Responsible manager not found",
                        "RESPONSIBLE_MANAGER_NOT_FOUND"
                ));

        validateManager(requesterManager, "Requester manager");
        validateManager(responsibleManager, "Responsible manager");

        Collection<ProjectReopenRequestStatus> pendingStatuses = List.of(
                ProjectReopenRequestStatus.PENDING_REQUESTER_MANAGER_APPROVAL,
                ProjectReopenRequestStatus.PENDING_RESPONSIBLE_MANAGER_APPROVAL
        );

        boolean pendingExists =
                projectReopenRequestRepository.existsByProjectIdAndStatusInAndIsDeletedFalse(
                        project.getId(),
                        pendingStatuses
                );

        if (pendingExists) {
            throw new ValidationException(
                    "A reopen request is already pending for this project",
                    "REOPEN_REQUEST_ALREADY_PENDING"
            );
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
        request.setCreatedBy(dto.getRequestedById());
        request.setUpdatedBy(dto.getRequestedById());
        request.setCreatedDate(new Date());
        request.setUpdatedDate(new Date());
        request.setDeleted(false);

        ProjectReopenRequest saved = projectReopenRequestRepository.save(request);

        return mapToResponseDto(saved);
    }

    @Override
    public ProjectReopenRequestResponseDto requesterManagerDecision(
            Long requestId,
            ProjectReopenDecisionDto dto
    ) {
        ProjectReopenRequest request = getActiveRequest(requestId);

        if (request.getStatus() != ProjectReopenRequestStatus.PENDING_REQUESTER_MANAGER_APPROVAL) {
            throw new ValidationException(
                    "Request is not pending requester manager approval",
                    "INVALID_REOPEN_REQUEST_STATUS"
            );
        }

        User actionBy = userRepository.findActiveUserById(dto.getActionById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Action user not found",
                        "USER_NOT_FOUND"
                ));

        if (!request.getRequesterManager().getId().equals(actionBy.getId())) {
            throw new ValidationException(
                    "Only requester manager can approve or reject this step",
                    "NOT_REQUESTER_MANAGER"
            );
        }

        String decision = normalizeDecision(dto.getDecision());

        request.setRequesterManagerRemarks(dto.getRemarks());
        request.setRequesterManagerActionAt(new Date());
        request.setUpdatedBy(dto.getActionById());
        request.setUpdatedDate(new Date());

        if ("REJECT".equals(decision)) {
            request.setStatus(ProjectReopenRequestStatus.REJECTED);
            return mapToResponseDto(projectReopenRequestRepository.save(request));
        }

        /*
         * If requester manager and responsible manager are same,
         * do not force same user to approve twice.
         */
        if (request.getRequesterManager().getId().equals(request.getResponsibleManager().getId())) {
            request.setResponsibleManagerRemarks(
                    "Auto-approved because requester manager and responsible manager are same."
            );
            request.setResponsibleManagerActionAt(new Date());
            request.setStatus(ProjectReopenRequestStatus.APPROVED);

            reopenProject(request, dto.getActionById());

            return mapToResponseDto(projectReopenRequestRepository.save(request));
        }

        request.setStatus(ProjectReopenRequestStatus.PENDING_RESPONSIBLE_MANAGER_APPROVAL);

        return mapToResponseDto(projectReopenRequestRepository.save(request));
    }

    @Override
    public ProjectReopenRequestResponseDto responsibleManagerDecision(
            Long requestId,
            ProjectReopenDecisionDto dto
    ) {
        ProjectReopenRequest request = getActiveRequest(requestId);

        if (request.getStatus() != ProjectReopenRequestStatus.PENDING_RESPONSIBLE_MANAGER_APPROVAL) {
            throw new ValidationException(
                    "Request is not pending responsible manager approval",
                    "INVALID_REOPEN_REQUEST_STATUS"
            );
        }

        User actionBy = userRepository.findActiveUserById(dto.getActionById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Action user not found",
                        "USER_NOT_FOUND"
                ));

        if (!request.getResponsibleManager().getId().equals(actionBy.getId())) {
            throw new ValidationException(
                    "Only responsible manager can approve or reject this step",
                    "NOT_RESPONSIBLE_MANAGER"
            );
        }

        String decision = normalizeDecision(dto.getDecision());

        request.setResponsibleManagerRemarks(dto.getRemarks());
        request.setResponsibleManagerActionAt(new Date());
        request.setUpdatedBy(dto.getActionById());
        request.setUpdatedDate(new Date());

        if ("REJECT".equals(decision)) {
            request.setStatus(ProjectReopenRequestStatus.REJECTED);
            return mapToResponseDto(projectReopenRequestRepository.save(request));
        }

        request.setStatus(ProjectReopenRequestStatus.APPROVED);

        reopenProject(request, dto.getActionById());

        return mapToResponseDto(projectReopenRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectReopenRequestResponseDto> getRequesterManagerPendingRequests(Long managerId) {
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
        return projectReopenRequestRepository
                .findByProjectIdAndIsDeletedFalseOrderByCreatedDateDesc(projectId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    private void reopenProject(ProjectReopenRequest request, Long actionById) {

        Project project = request.getProject();

        MilestoneStatus newStatus = milestoneStatusRepository.findByName("NEW")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone status NEW not found",
                        "STATUS_NOT_FOUND"
                ));

        ProjectStatus reopenedStatus = projectStatusRepository.findByName("REOPENED")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project status REOPENED not found",
                        "STATUS_NOT_FOUND"
                ));

        User changedBy = userRepository.findActiveUserById(actionById)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Action user not found",
                        "USER_NOT_FOUND"
                ));

        List<ProjectMilestoneAssignment> assignments =
                projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());

        String reason = "Project reopened due to approved reopen request ID: "
                + request.getId()
                + ". Reason: "
                + request.getRequestReason();

        for (ProjectMilestoneAssignment assignment : assignments) {

            if (assignment.getStatus() != null &&
                    "NEW".equalsIgnoreCase(assignment.getStatus().getName())) {
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

            /*
             * Full project reopen means current progress becomes 0 dynamically
             * because no milestone remains COMPLETED.
             *
             * Hide all first, then visibility calculation will open only eligible milestone.
             */
            assignment.setVisible(false);
            assignment.setVisibilityReason(
                    "Hidden because project was reopened. Visibility will be recalculated."
            );

            assignment.setUpdatedBy(actionById);
            assignment.setUpdatedDate(new Date());

            projectMilestoneAssignmentRepository.save(assignment);
        }

        project.setStatus(reopenedStatus);
        project.setUpdatedBy(actionById);
        project.setUpdatedDate(new Date());

        projectRepository.save(project);

        /*
         * Recalculate milestone visibility after reset.
         * This should open only the eligible first milestone based on your existing logic.
         */
        projectService.updateMilestoneVisibilities(project, actionById);
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

    private ProjectReopenRequest getActiveRequest(Long requestId) {
        return projectReopenRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project reopen request not found",
                        "PROJECT_REOPEN_REQUEST_NOT_FOUND"
                ));
    }

    private void validateAssignmentBelongsToProject(
            ProjectMilestoneAssignment assignment,
            Project project
    ) {
        if (assignment.getProject() == null ||
                !assignment.getProject().getId().equals(project.getId())) {
            throw new ValidationException(
                    "Milestone assignment does not belong to this project",
                    "ASSIGNMENT_PROJECT_MISMATCH"
            );
        }
    }

    private void validateManager(User manager, String label) {
        if (!manager.isManagerFlag()) {
            throw new ValidationException(
                    label + " must be a manager",
                    "USER_IS_NOT_MANAGER"
            );
        }
    }

    private String normalizeDecision(String decision) {
        if (decision == null || decision.trim().isEmpty()) {
            throw new ValidationException(
                    "Decision is required",
                    "INVALID_DECISION"
            );
        }

        String value = decision.trim().toUpperCase();

        if (!List.of("APPROVE", "REJECT").contains(value)) {
            throw new ValidationException(
                    "Decision must be APPROVE or REJECT",
                    "INVALID_DECISION"
            );
        }

        return value;
    }

    private Department getFirstDepartment(ProjectMilestoneAssignment assignment) {
        if (assignment == null ||
                assignment.getProductMilestoneMap() == null ||
                assignment.getProductMilestoneMap().getMilestone() == null ||
                assignment.getProductMilestoneMap().getMilestone().getDepartments() == null ||
                assignment.getProductMilestoneMap().getMilestone().getDepartments().isEmpty()) {
            return null;
        }

        return assignment.getProductMilestoneMap()
                .getMilestone()
                .getDepartments()
                .get(0);
    }

    private ProjectReopenRequestResponseDto mapToResponseDto(ProjectReopenRequest request) {

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