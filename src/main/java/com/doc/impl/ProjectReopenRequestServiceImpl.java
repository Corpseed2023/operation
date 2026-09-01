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
import com.doc.service.project.ProjectReopenRequestService;
import com.doc.service.project.ProjectService;
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

    private static final Logger logger =
            LogManager.getLogger(ProjectReopenRequestServiceImpl.class);

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

    // =====================================================================
    // CREATE REOPEN REQUEST
    // =====================================================================

    @Override
    public ProjectReopenRequestResponseDto createReopenRequest(
            ProjectReopenCreateRequestDto dto
    ) {

        logger.info(
                "Creating reopen request for projectId: {}, detectedAtAssignmentId: {}, responsibleAssignmentId: {}",
                dto.getProjectId(),
                dto.getDetectedAtAssignmentId(),
                dto.getResponsibleAssignmentId()
        );

        Project project = projectRepository
                .findActiveUserById(dto.getProjectId())
                .orElseThrow(() -> {
                    logger.error(
                            "Project not found with id: {}",
                            dto.getProjectId()
                    );

                    return new ResourceNotFoundException(
                            "Project not found",
                            "PROJECT_NOT_FOUND"
                    );
                });

        ProjectMilestoneAssignment detectedAtAssignment =
                projectMilestoneAssignmentRepository
                        .findActiveUserById(dto.getDetectedAtAssignmentId())
                        .orElseThrow(() -> {
                            logger.error(
                                    "Detected milestone assignment not found with id: {}",
                                    dto.getDetectedAtAssignmentId()
                            );

                            return new ResourceNotFoundException(
                                    "Detected milestone assignment not found",
                                    "DETECTED_ASSIGNMENT_NOT_FOUND"
                            );
                        });

        ProjectMilestoneAssignment responsibleAssignment =
                projectMilestoneAssignmentRepository
                        .findActiveUserById(dto.getResponsibleAssignmentId())
                        .orElseThrow(() -> {
                            logger.error(
                                    "Responsible milestone assignment not found with id: {}",
                                    dto.getResponsibleAssignmentId()
                            );

                            return new ResourceNotFoundException(
                                    "Responsible milestone assignment not found",
                                    "RESPONSIBLE_ASSIGNMENT_NOT_FOUND"
                            );
                        });

        logger.debug(
                "Validating assignments belong to project"
        );

        validateAssignmentBelongsToProject(
                detectedAtAssignment,
                project
        );

        validateAssignmentBelongsToProject(
                responsibleAssignment,
                project
        );

        if (detectedAtAssignment.getId()
                .equals(responsibleAssignment.getId())) {

            logger.warn(
                    "Detected and responsible assignment are the same for projectId: {}",
                    dto.getProjectId()
            );

            throw new ValidationException(
                    "Detected assignment and responsible assignment cannot be same",
                    "INVALID_REOPEN_ASSIGNMENT_SELECTION"
            );
        }

        if (responsibleAssignment.getStatus() == null
                || !"COMPLETED".equalsIgnoreCase(
                responsibleAssignment.getStatus().getName()
        )) {

            logger.warn(
                    "Responsible milestone is not COMPLETED for assignmentId: {}",
                    responsibleAssignment.getId()
            );

            throw new ValidationException(
                    "Responsible milestone must be COMPLETED before project can be reopened",
                    "RESPONSIBLE_MILESTONE_NOT_COMPLETED"
            );
        }

        User requestedBy =
                detectedAtAssignment.getAssignedUser();

        if (requestedBy == null) {

            logger.error(
                    "Detected milestone has no assigned user for assignmentId: {}",
                    detectedAtAssignment.getId()
            );

            throw new ValidationException(
                    "Detected milestone does not have assigned user",
                    "DETECTED_ASSIGNMENT_USER_NOT_FOUND"
            );
        }

        if (requestedBy.isDeleted()
                || !requestedBy.isActive()) {

            logger.warn(
                    "RequestedBy user is not active/deleted. userId: {}",
                    requestedBy.getId()
            );

            throw new ValidationException(
                    "Detected milestone assigned user is not active",
                    "REQUESTED_BY_USER_NOT_ACTIVE"
            );
        }

        User requesterManager =
                requestedBy.getManager();

        if (requesterManager == null) {

            logger.error(
                    "Requester has no manager mapped. userId: {}",
                    requestedBy.getId()
            );

            throw new ValidationException(
                    "Requester user does not have manager mapped",
                    "REQUESTER_MANAGER_NOT_MAPPED"
            );
        }

        logger.debug(
                "Validating requester manager"
        );

        validateManager(
                requesterManager,
                "Requester manager"
        );

        User responsibleManager =
                resolveResponsibleManager(
                        responsibleAssignment
                );

        logger.debug(
                "Resolved responsible manager: {}",
                responsibleManager.getId()
        );

        validateManager(
                responsibleManager,
                "Responsible manager"
        );

        Collection<ProjectReopenRequestStatus> pendingStatuses =
                List.of(
                        ProjectReopenRequestStatus.PENDING_REQUESTER_MANAGER_APPROVAL,
                        ProjectReopenRequestStatus.PENDING_RESPONSIBLE_MANAGER_APPROVAL
                );

        boolean pendingExists =
                projectReopenRequestRepository
                        .existsByProjectIdAndStatusInAndIsDeletedFalse(
                                project.getId(),
                                pendingStatuses
                        );

        if (pendingExists) {

            logger.warn(
                    "Pending reopen request already exists for projectId: {}",
                    project.getId()
            );

            throw new ValidationException(
                    "A reopen request is already pending for this project",
                    "REOPEN_REQUEST_ALREADY_PENDING"
            );
        }

        ProjectReopenRequest request =
                new ProjectReopenRequest();

        request.setProject(
                project
        );

        request.setDetectedAtAssignment(
                detectedAtAssignment
        );

        request.setResponsibleAssignment(
                responsibleAssignment
        );

        request.setRequesterDepartment(
                getFirstDepartment(detectedAtAssignment)
        );

        request.setResponsibleDepartment(
                getFirstDepartment(responsibleAssignment)
        );

        request.setRequestedBy(
                requestedBy
        );

        request.setRequesterManager(
                requesterManager
        );

        request.setResponsibleManager(
                responsibleManager
        );

        request.setRequestReason(
                dto.getReason().trim()
        );

        request.setStatus(
                ProjectReopenRequestStatus.PENDING_REQUESTER_MANAGER_APPROVAL
        );

        request.setRequestedAt(
                new Date()
        );

        request.setCreatedBy(
                requestedBy.getId()
        );

        request.setUpdatedBy(
                requestedBy.getId()
        );

        request.setCreatedDate(
                new Date()
        );

        request.setUpdatedDate(
                new Date()
        );

        request.setDeleted(false);

        ProjectReopenRequest saved =
                projectReopenRequestRepository.save(request);

        logger.info(
                "Reopen request created successfully. requestId: {}",
                saved.getId()
        );

        return mapToResponseDto(saved);
    }

    // =====================================================================
    // RESOLVE RESPONSIBLE MANAGER
    // =====================================================================

    private User resolveResponsibleManager(
            ProjectMilestoneAssignment responsibleAssignment
    ) {

        logger.debug(
                "Resolving responsible manager for assignmentId: {}",
                responsibleAssignment.getId()
        );

        /*
         * First preference:
         * responsible milestone assigned user's manager.
         */
        if (responsibleAssignment.getAssignedUser() != null) {

            User responsibleUser =
                    responsibleAssignment.getAssignedUser();

            if (responsibleUser.getManager() != null) {

                User manager =
                        responsibleUser.getManager();

                if (!manager.isDeleted()
                        && manager.isActive()
                        && manager.isManagerFlag()) {

                    logger.debug(
                            "Found manager from responsible user: {}",
                            manager.getId()
                    );

                    return manager;
                }
            }
        }

        /*
         * Fallback:
         * manager from responsible milestone department.
         */
        Department responsibleDepartment =
                getFirstDepartment(
                        responsibleAssignment
                );

        if (responsibleDepartment == null
                || responsibleDepartment.getId() == null) {

            logger.error(
                    "Responsible milestone department not found for assignmentId: {}",
                    responsibleAssignment.getId()
            );

            throw new ValidationException(
                    "Responsible milestone department not found",
                    "RESPONSIBLE_DEPARTMENT_NOT_FOUND"
            );
        }

        List<User> managers =
                userRepository
                        .findActiveManagersByDepartmentId(
                                responsibleDepartment.getId()
                        );

        if (managers == null
                || managers.isEmpty()) {

            logger.error(
                    "No active manager found for department: {}",
                    responsibleDepartment.getName()
            );

            throw new ValidationException(
                    "No active manager found for responsible department: "
                            + responsibleDepartment.getName(),
                    "RESPONSIBLE_MANAGER_NOT_FOUND"
            );
        }

        logger.debug(
                "Selected responsible manager from department: {}",
                managers.get(0).getId()
        );

        return managers.get(0);
    }

    // =====================================================================
    // REQUESTER MANAGER DECISION
    // =====================================================================

    @Override
    public ProjectReopenRequestResponseDto requesterManagerDecision(
            Long requestId,
            ProjectReopenDecisionDto dto
    ) {

        logger.info(
                "Requester manager decision for requestId: {}, decision: {}",
                requestId,
                dto.getDecision()
        );

        ProjectReopenRequest request =
                getActiveRequest(requestId);

        if (request.getStatus()
                != ProjectReopenRequestStatus.PENDING_REQUESTER_MANAGER_APPROVAL) {

            logger.warn(
                    "Invalid status for requester decision. Current status: {}",
                    request.getStatus()
            );

            throw new ValidationException(
                    "Request is not pending requester manager approval",
                    "INVALID_REOPEN_REQUEST_STATUS"
            );
        }

        User actionBy =
                userRepository
                        .findActiveUserById(dto.getActionById())
                        .orElseThrow(() -> {

                            logger.error(
                                    "Action user not found: {}",
                                    dto.getActionById()
                            );

                            return new ResourceNotFoundException(
                                    "Action user not found",
                                    "USER_NOT_FOUND"
                            );
                        });

        if (!request.getRequesterManager()
                .getId()
                .equals(actionBy.getId())) {

            logger.warn(
                    "Unauthorized requester manager action. actionBy: {}, requesterManager: {}",
                    dto.getActionById(),
                    request.getRequesterManager().getId()
            );

            throw new ValidationException(
                    "Only requester manager can approve or reject this step",
                    "NOT_REQUESTER_MANAGER"
            );
        }

        String decision =
                normalizeDecision(
                        dto.getDecision()
                );

        request.setRequesterManagerRemarks(
                dto.getRemarks()
        );

        request.setRequesterManagerActionAt(
                new Date()
        );

        request.setUpdatedBy(
                dto.getActionById()
        );

        request.setUpdatedDate(
                new Date()
        );

        /*
         * REQUESTER MANAGER REJECTED.
         */
        if ("REJECT".equals(decision)) {

            request.setStatus(
                    ProjectReopenRequestStatus.REJECTED
            );

            logger.info(
                    "Reopen request rejected by requester manager. requestId: {}",
                    requestId
            );

            return mapToResponseDto(
                    projectReopenRequestRepository.save(request)
            );
        }

        /*
         * Both managers are same.
         *
         * No need for same manager to approve twice.
         */
        if (request.getRequesterManager()
                .getId()
                .equals(
                        request.getResponsibleManager().getId()
                )) {

            logger.info(
                    "Auto-approving responsible manager step as managers are same. requestId: {}",
                    requestId
            );

            request.setResponsibleManagerRemarks(
                    "Auto-approved because requester manager and responsible manager are same."
            );

            request.setResponsibleManagerActionAt(
                    new Date()
            );

            request.setStatus(
                    ProjectReopenRequestStatus.APPROVED
            );

            /*
             * Final approval reached.
             */
            reopenProject(
                    request,
                    dto.getActionById()
            );

            return mapToResponseDto(
                    projectReopenRequestRepository.save(request)
            );
        }

        /*
         * Move to responsible manager.
         */
        request.setStatus(
                ProjectReopenRequestStatus.PENDING_RESPONSIBLE_MANAGER_APPROVAL
        );

        logger.info(
                "Reopen request moved to responsible manager approval. requestId: {}",
                requestId
        );

        return mapToResponseDto(
                projectReopenRequestRepository.save(request)
        );
    }

    // =====================================================================
    // RESPONSIBLE MANAGER DECISION
    // =====================================================================

    @Override
    public ProjectReopenRequestResponseDto responsibleManagerDecision(
            Long requestId,
            ProjectReopenDecisionDto dto
    ) {

        logger.info(
                "Responsible manager decision for requestId: {}, decision: {}",
                requestId,
                dto.getDecision()
        );

        ProjectReopenRequest request =
                getActiveRequest(requestId);

        if (request.getStatus()
                != ProjectReopenRequestStatus.PENDING_RESPONSIBLE_MANAGER_APPROVAL) {

            logger.warn(
                    "Invalid status for responsible decision. Current status: {}",
                    request.getStatus()
            );

            throw new ValidationException(
                    "Request is not pending responsible manager approval",
                    "INVALID_REOPEN_REQUEST_STATUS"
            );
        }

        User actionBy =
                userRepository
                        .findActiveUserById(dto.getActionById())
                        .orElseThrow(() -> {

                            logger.error(
                                    "Action user not found: {}",
                                    dto.getActionById()
                            );

                            return new ResourceNotFoundException(
                                    "Action user not found",
                                    "USER_NOT_FOUND"
                            );
                        });

        if (!request.getResponsibleManager()
                .getId()
                .equals(actionBy.getId())) {

            logger.warn(
                    "Unauthorized responsible manager action. actionBy: {}, responsibleManager: {}",
                    dto.getActionById(),
                    request.getResponsibleManager().getId()
            );

            throw new ValidationException(
                    "Only responsible manager can approve or reject this step",
                    "NOT_RESPONSIBLE_MANAGER"
            );
        }

        String decision =
                normalizeDecision(
                        dto.getDecision()
                );

        request.setResponsibleManagerRemarks(
                dto.getRemarks()
        );

        request.setResponsibleManagerActionAt(
                new Date()
        );

        request.setUpdatedBy(
                dto.getActionById()
        );

        request.setUpdatedDate(
                new Date()
        );

        /*
         * RESPONSIBLE MANAGER REJECTED.
         */
        if ("REJECT".equals(decision)) {

            request.setStatus(
                    ProjectReopenRequestStatus.REJECTED
            );

            logger.info(
                    "Reopen request rejected by responsible manager. requestId: {}",
                    requestId
            );

            return mapToResponseDto(
                    projectReopenRequestRepository.save(request)
            );
        }

        /*
         * FINAL APPROVAL.
         */
        request.setStatus(
                ProjectReopenRequestStatus.APPROVED
        );

        logger.info(
                "Reopen request approved. Reopening project. requestId: {}",
                requestId
        );

        reopenProject(
                request,
                dto.getActionById()
        );

        return mapToResponseDto(
                projectReopenRequestRepository.save(request)
        );
    }

    // =====================================================================
    // REQUESTER MANAGER PENDING REQUESTS
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ProjectReopenRequestResponseDto>
    getRequesterManagerPendingRequests(
            Long managerId
    ) {

        logger.debug(
                "Fetching pending requester manager requests for managerId: {}",
                managerId
        );

        return projectReopenRequestRepository
                .findByRequesterManagerIdAndStatusAndIsDeletedFalse(
                        managerId,
                        ProjectReopenRequestStatus.PENDING_REQUESTER_MANAGER_APPROVAL
                )
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =====================================================================
    // RESPONSIBLE MANAGER PENDING REQUESTS
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ProjectReopenRequestResponseDto>
    getResponsibleManagerPendingRequests(
            Long managerId
    ) {

        logger.debug(
                "Fetching pending responsible manager requests for managerId: {}",
                managerId
        );

        return projectReopenRequestRepository
                .findByResponsibleManagerIdAndStatusAndIsDeletedFalse(
                        managerId,
                        ProjectReopenRequestStatus.PENDING_RESPONSIBLE_MANAGER_APPROVAL
                )
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =====================================================================
    // PROJECT REOPEN HISTORY
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ProjectReopenRequestResponseDto>
    getProjectReopenRequests(
            Long projectId
    ) {

        logger.debug(
                "Fetching all reopen requests for projectId: {}",
                projectId
        );

        return projectReopenRequestRepository
                .findByProjectIdAndIsDeletedFalseOrderByCreatedDateDesc(
                        projectId
                )
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =====================================================================
    // REOPEN PROJECT
    //
    // ONLY THIS FLOW IS CHANGED.
    //
    // Before responsible milestone -> KEEP AS-IS
    //
    // Responsible milestone:
    // COMPLETED -> REWORK
    // visible = true
    // completedDate = null
    //
    // Milestones after responsible:
    // -> NEW
    // -> hidden
    // -> visibleDate = null
    // -> startedDate = null
    // -> completedDate = null
    //
    // Project:
    // -> REOPENED
    // =====================================================================

    private void reopenProject(
            ProjectReopenRequest request,
            Long actionById
    ) {

        logger.info(
                "[PROJECT-REOPEN-START] requestId={}, actionById={}",
                request.getId(),
                actionById
        );

        Project project =
                request.getProject();

        ProjectMilestoneAssignment responsibleAssignment =
                request.getResponsibleAssignment();

        ProjectMilestoneAssignment detectedAtAssignment =
                request.getDetectedAtAssignment();

        if (project == null) {

            logger.error(
                    "[PROJECT-REOPEN-FAILED] Project missing in reopen request. requestId={}",
                    request.getId()
            );

            throw new ValidationException(
                    "Project not found in reopen request",
                    "PROJECT_NOT_FOUND"
            );
        }

        if (responsibleAssignment == null) {

            logger.error(
                    "[PROJECT-REOPEN-FAILED] Responsible assignment missing. requestId={}",
                    request.getId()
            );

            throw new ValidationException(
                    "Responsible milestone assignment not found",
                    "RESPONSIBLE_ASSIGNMENT_NOT_FOUND"
            );
        }

        if (detectedAtAssignment == null) {

            logger.error(
                    "[PROJECT-REOPEN-FAILED] Detected assignment missing. requestId={}",
                    request.getId()
            );

            throw new ValidationException(
                    "Detected milestone assignment not found",
                    "DETECTED_ASSIGNMENT_NOT_FOUND"
            );
        }

        /*
         * Status used for all milestones AFTER responsible milestone.
         */
        MilestoneStatus newStatus =
                milestoneStatusRepository
                        .findByName("NEW")
                        .orElseThrow(() -> {

                            logger.error(
                                    "Milestone status NEW not found"
                            );

                            return new ResourceNotFoundException(
                                    "Milestone status NEW not found",
                                    "STATUS_NOT_FOUND"
                            );
                        });

        /*
         * Status used only for responsible milestone.
         */
        MilestoneStatus reworkStatus =
                milestoneStatusRepository
                        .findByName("REWORK")
                        .orElseThrow(() -> {

                            logger.error(
                                    "Milestone status REWORK not found"
                            );

                            return new ResourceNotFoundException(
                                    "Milestone status REWORK not found",
                                    "STATUS_NOT_FOUND"
                            );
                        });

        /*
         * Project status.
         */
        ProjectStatus reopenedStatus =
                projectStatusRepository
                        .findByName("REOPENED")
                        .orElseThrow(() -> {

                            logger.error(
                                    "Project status REOPENED not found"
                            );

                            return new ResourceNotFoundException(
                                    "Project status REOPENED not found",
                                    "STATUS_NOT_FOUND"
                            );
                        });

        /*
         * Manager/action user who finally approved reopen.
         */
        User changedBy =
                userRepository
                        .findActiveUserById(actionById)
                        .orElseThrow(() -> {

                            logger.error(
                                    "Action user not found for reopen: {}",
                                    actionById
                            );

                            return new ResourceNotFoundException(
                                    "Action user not found",
                                    "USER_NOT_FOUND"
                            );
                        });

        /*
         * Validate assignment relationships again before changing data.
         */
        validateAssignmentBelongsToProject(
                responsibleAssignment,
                project
        );

        validateAssignmentBelongsToProject(
                detectedAtAssignment,
                project
        );

        /*
         * Responsible milestone must still be completed when final
         * manager approval happens.
         */
        if (responsibleAssignment.getStatus() == null
                || !"COMPLETED".equalsIgnoreCase(
                responsibleAssignment.getStatus().getName()
        )) {

            logger.error(
                    "[PROJECT-REOPEN-RESPONSIBLE-NOT-COMPLETED] " +
                            "projectId={}, assignmentId={}, milestone={}, status={}",
                    project.getId(),
                    responsibleAssignment.getId(),
                    getMilestoneName(responsibleAssignment),
                    responsibleAssignment.getStatus() != null
                            ? responsibleAssignment.getStatus().getName()
                            : null
            );

            throw new ValidationException(
                    "Responsible milestone must be COMPLETED before project can be reopened",
                    "RESPONSIBLE_MILESTONE_NOT_COMPLETED"
            );
        }

        /*
         * Workflow order is stored in ProductMilestoneMap.
         */
        int responsibleOrder =
                getMilestoneOrder(
                        responsibleAssignment
                );

        int detectedOrder =
                getMilestoneOrder(
                        detectedAtAssignment
                );

        /*
         * According to reopen flow:
         *
         * Responsible milestone must belong to an EARLIER workflow
         * step than the milestone where the problem was detected.
         */
        if (responsibleOrder >= detectedOrder) {

            logger.error(
                    "[PROJECT-REOPEN-INVALID-ORDER] projectId={}, " +
                            "responsibleAssignmentId={}, responsibleOrder={}, " +
                            "detectedAssignmentId={}, detectedOrder={}",
                    project.getId(),
                    responsibleAssignment.getId(),
                    responsibleOrder,
                    detectedAtAssignment.getId(),
                    detectedOrder
            );

            throw new ValidationException(
                    "Responsible milestone must be before the milestone where the issue was detected",
                    "RESPONSIBLE_MILESTONE_MUST_BE_BEFORE_DETECTED_MILESTONE"
            );
        }

        List<ProjectMilestoneAssignment> assignments =
                projectMilestoneAssignmentRepository
                        .findByProjectIdAndIsDeletedFalse(
                                project.getId()
                        );

        String responsibleMilestoneName =
                getMilestoneName(
                        responsibleAssignment
                );

        String reason =
                "Project reopened due to approved reopen request ID: "
                        + request.getId()
                        + ". Responsible milestone: "
                        + responsibleMilestoneName
                        + ". Reason: "
                        + request.getRequestReason();

        String waitingReason =
                "Waiting for rework completion of milestone: "
                        + responsibleMilestoneName;

        Date now =
                new Date();

        logger.info(
                "[PROJECT-REOPEN-FLOW] projectId={}, requestId={}, " +
                        "responsibleAssignmentId={}, responsibleMilestone={}, responsibleOrder={}, " +
                        "detectedAssignmentId={}, detectedMilestone={}, detectedOrder={}, " +
                        "totalAssignments={}",
                project.getId(),
                request.getId(),
                responsibleAssignment.getId(),
                responsibleMilestoneName,
                responsibleOrder,
                detectedAtAssignment.getId(),
                getMilestoneName(detectedAtAssignment),
                detectedOrder,
                assignments.size()
        );

        /*
         * ============================================================
         * PROCESS PROJECT MILESTONES
         * ============================================================
         */
        for (ProjectMilestoneAssignment assignment : assignments) {

            if (assignment == null) {
                continue;
            }

            int assignmentOrder =
                    getMilestoneOrder(
                            assignment
                    );

            String milestoneName =
                    getMilestoneName(
                            assignment
                    );

            String oldStatusName =
                    assignment.getStatus() != null
                            ? assignment.getStatus().getName()
                            : null;

            logger.info(
                    "[PROJECT-REOPEN-MILESTONE-CHECK] " +
                            "assignmentId={}, milestone={}, order={}, oldStatus={}, responsibleOrder={}",
                    assignment.getId(),
                    milestoneName,
                    assignmentOrder,
                    oldStatusName,
                    responsibleOrder
            );

            /*
             * ========================================================
             * 1. MILESTONES BEFORE RESPONSIBLE
             *
             * DO NOT TOUCH.
             *
             * Example:
             *
             * Initial Review COMPLETED
             *
             * remains exactly the same.
             * ========================================================
             */
            if (assignmentOrder < responsibleOrder) {

                logger.info(
                        "[PROJECT-REOPEN-KEEP-AS-IS] " +
                                "assignmentId={}, milestone={}, order={}, status={}",
                        assignment.getId(),
                        milestoneName,
                        assignmentOrder,
                        oldStatusName
                );

                continue;
            }

            /*
             * ========================================================
             * 2. RESPONSIBLE MILESTONE
             *
             * COMPLETED -> REWORK
             *
             * Same assignment.
             * Same assigned user.
             * Visible immediately.
             * ========================================================
             */
            if (assignment.getId()
                    .equals(responsibleAssignment.getId())) {

                logger.info(
                        "[PROJECT-REOPEN-RESPONSIBLE-START] " +
                                "assignmentId={}, milestone={}, oldStatus={}, newStatus=REWORK",
                        assignment.getId(),
                        milestoneName,
                        oldStatusName
                );

                saveMilestoneStatusHistory(
                        assignment,
                        assignment.getStatus(),
                        reworkStatus,
                        reason,
                        changedBy
                );

                assignment.setStatus(
                        reworkStatus
                );

                assignment.setStatusReason(
                        reason
                );

                /*
                 * Responsible milestone becomes visible immediately.
                 */
                assignment.setVisible(true);

                assignment.setVisibilityReason(null);

                /*
                 * Because milestone is visible again for rework.
                 */
                assignment.setVisibleDate(now);

                /*
                 * Previous completion is no longer valid.
                 */
                assignment.setCompletedDate(null);

                /*
                 * IMPORTANT:
                 *
                 * Do NOT clear:
                 *
                 * assignedUser
                 * startedDate
                 * reworkAttempts
                 *
                 * Existing responsible user continues correction.
                 */

                assignment.setUpdatedBy(
                        actionById
                );

                assignment.setUpdatedDate(
                        now
                );

                projectMilestoneAssignmentRepository
                        .save(assignment);

                logger.info(
                        "[PROJECT-REOPEN-RESPONSIBLE-SUCCESS] " +
                                "assignmentId={}, milestone={}, status=REWORK, visible=true, assignedUserId={}",
                        assignment.getId(),
                        milestoneName,
                        assignment.getAssignedUser() != null
                                ? assignment.getAssignedUser().getId()
                                : null
                );

                continue;
            }

            /*
             * If duplicate step order exists unexpectedly,
             * do not touch another milestone having responsible order.
             *
             * Normally ProductMilestoneMap already prevents duplicate
             * order for one product.
             */
            if (assignmentOrder == responsibleOrder) {

                logger.warn(
                        "[PROJECT-REOPEN-SAME-ORDER-SKIPPED] " +
                                "assignmentId={}, milestone={}, order={}, responsibleAssignmentId={}",
                        assignment.getId(),
                        milestoneName,
                        assignmentOrder,
                        responsibleAssignment.getId()
                );

                continue;
            }

            /*
             * ========================================================
             * 3. EVERYTHING AFTER RESPONSIBLE
             *
             * OLD STATE -> NEW
             * visible = false
             *
             * Old workflow dates are cleared.
             *
             * Example:
             *
             * Legal Verification COMPLETED -> NEW
             * Filing COMPLETED             -> NEW
             * Certification IN_PROGRESS    -> NEW
             * ========================================================
             */
            if (assignmentOrder > responsibleOrder) {

                logger.info(
                        "[PROJECT-REOPEN-DOWNSTREAM-RESET-START] " +
                                "assignmentId={}, milestone={}, order={}, oldStatus={}",
                        assignment.getId(),
                        milestoneName,
                        assignmentOrder,
                        oldStatusName
                );

                /*
                 * Save history only if actual status is changing.
                 *
                 * We do not need NEW -> NEW history.
                 */
                if (assignment.getStatus() == null
                        || !"NEW".equalsIgnoreCase(
                        assignment.getStatus().getName()
                )) {

                    saveMilestoneStatusHistory(
                            assignment,
                            assignment.getStatus(),
                            newStatus,
                            reason,
                            changedBy
                    );
                }

                assignment.setStatus(
                        newStatus
                );

                assignment.setStatusReason(
                        reason
                );

                /*
                 * Hide every downstream milestone.
                 */
                assignment.setVisible(false);

                assignment.setVisibilityReason(
                        waitingReason
                );

                /*
                 * Clear old workflow dates.
                 */
                assignment.setVisibleDate(null);
                assignment.setStartedDate(null);
                assignment.setCompletedDate(null);

                /*
                 * IMPORTANT:
                 *
                 * We do NOT clear assignedUser.
                 *
                 * Existing assignment remains intact.
                 * Only its workflow state is rewound.
                 */

                assignment.setUpdatedBy(
                        actionById
                );

                assignment.setUpdatedDate(
                        now
                );

                projectMilestoneAssignmentRepository
                        .save(assignment);

                logger.info(
                        "[PROJECT-REOPEN-DOWNSTREAM-RESET-SUCCESS] " +
                                "assignmentId={}, milestone={}, oldStatus={}, newStatus=NEW, visible=false",
                        assignment.getId(),
                        milestoneName,
                        oldStatusName
                );
            }
        }

        /*
         * ============================================================
         * PROJECT STATUS -> REOPENED
         * ============================================================
         */
        String oldProjectStatus =
                project.getStatus() != null
                        ? project.getStatus().getName()
                        : null;

        project.setStatus(
                reopenedStatus
        );

        project.setUpdatedBy(
                actionById
        );

        project.setUpdatedDate(
                now
        );

        projectRepository.save(
                project
        );

        logger.info(
                "[PROJECT-REOPEN-PROJECT-STATUS] projectId={}, oldStatus={}, newStatus=REOPENED",
                project.getId(),
                oldProjectStatus
        );

        /*
         * ============================================================
         * VERY IMPORTANT
         * ============================================================
         *
         * DO NOT call:
         *
         * projectService.updateMilestoneVisibilities(project, actionById);
         *
         * here.
         *
         * Right now we intentionally need:
         *
         * Responsible milestone = REWORK + visible
         * Downstream milestones = NEW + hidden
         *
         * Later when responsible user completes:
         *
         * REWORK -> COMPLETED
         *
         * normal milestone completion flow will call:
         *
         * updateMilestoneVisibilities(...)
         *
         * and then next eligible milestone becomes visible.
         */

        logger.info(
                "[PROJECT-REOPEN-SUCCESS] projectId={}, requestId={}, " +
                        "responsibleAssignmentId={}, responsibleMilestone={}, " +
                        "responsibleStatus=REWORK, projectStatus=REOPENED",
                project.getId(),
                request.getId(),
                responsibleAssignment.getId(),
                responsibleMilestoneName
        );
    }

    // =====================================================================
    // GET WORKFLOW ORDER
    // =====================================================================

    private int getMilestoneOrder(
            ProjectMilestoneAssignment assignment
    ) {

        if (assignment == null) {

            throw new ValidationException(
                    "Milestone assignment is required",
                    "MILESTONE_ASSIGNMENT_REQUIRED"
            );
        }

        if (assignment.getProductMilestoneMap() == null) {

            logger.error(
                    "Product milestone map missing for assignmentId={}",
                    assignment.getId()
            );

            throw new ValidationException(
                    "Product milestone mapping not found for milestone assignment",
                    "PRODUCT_MILESTONE_MAP_NOT_FOUND"
            );
        }

        return assignment
                .getProductMilestoneMap()
                .getOrder();
    }

    // =====================================================================
    // SAVE STATUS HISTORY
    // =====================================================================

    private void saveMilestoneStatusHistory(
            ProjectMilestoneAssignment assignment,
            MilestoneStatus previousStatus,
            MilestoneStatus newStatus,
            String reason,
            User changedBy
    ) {

        logger.debug(
                "Saving milestone status history for assignmentId: {}",
                assignment.getId()
        );

        MilestoneStatusHistory history =
                new MilestoneStatusHistory();

        history.setMilestoneAssignment(
                assignment
        );

        history.setPreviousStatus(
                previousStatus
        );

        history.setNewStatus(
                newStatus
        );

        history.setChangeReason(
                reason
        );

        history.setChangedBy(
                changedBy
        );

        history.setChangeDate(
                new Date()
        );

        history.setDeleted(false);

        milestoneStatusHistoryRepository.save(
                history
        );
    }

    // =====================================================================
    // GET ACTIVE REOPEN REQUEST
    // =====================================================================

    private ProjectReopenRequest getActiveRequest(
            Long requestId
    ) {

        logger.debug(
                "Fetching active reopen request: {}",
                requestId
        );

        return projectReopenRequestRepository
                .findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> {

                    logger.error(
                            "Project reopen request not found: {}",
                            requestId
                    );

                    return new ResourceNotFoundException(
                            "Project reopen request not found",
                            "PROJECT_REOPEN_REQUEST_NOT_FOUND"
                    );
                });
    }

    // =====================================================================
    // ASSIGNMENT BELONGS TO PROJECT VALIDATION
    // =====================================================================

    private void validateAssignmentBelongsToProject(
            ProjectMilestoneAssignment assignment,
            Project project
    ) {

        if (assignment.getProject() == null
                || !assignment.getProject()
                .getId()
                .equals(project.getId())) {

            logger.error(
                    "Assignment project mismatch. assignmentProjectId: {}, expectedProjectId: {}",
                    assignment.getProject() != null
                            ? assignment.getProject().getId()
                            : null,
                    project.getId()
            );

            throw new ValidationException(
                    "Milestone assignment does not belong to this project",
                    "ASSIGNMENT_PROJECT_MISMATCH"
            );
        }
    }

    // =====================================================================
    // MANAGER VALIDATION
    // =====================================================================

    private void validateManager(
            User manager,
            String label
    ) {

        if (!manager.isManagerFlag()) {

            logger.error(
                    "{} is not a manager. userId: {}",
                    label,
                    manager.getId()
            );

            throw new ValidationException(
                    label + " must be a manager",
                    "USER_IS_NOT_MANAGER"
            );
        }
    }

    // =====================================================================
    // NORMALIZE DECISION
    // =====================================================================

    private String normalizeDecision(
            String decision
    ) {

        if (decision == null
                || decision.trim().isEmpty()) {

            logger.error(
                    "Decision is empty or null"
            );

            throw new ValidationException(
                    "Decision is required",
                    "INVALID_DECISION"
            );
        }

        String value =
                decision.trim().toUpperCase();

        if (!List.of(
                "APPROVE",
                "REJECT"
        ).contains(value)) {

            logger.error(
                    "Invalid decision value: {}",
                    decision
            );

            throw new ValidationException(
                    "Decision must be APPROVE or REJECT",
                    "INVALID_DECISION"
            );
        }

        return value;
    }

    // =====================================================================
    // GET FIRST DEPARTMENT
    // =====================================================================

    private Department getFirstDepartment(
            ProjectMilestoneAssignment assignment
    ) {

        if (assignment == null
                || assignment.getProductMilestoneMap() == null
                || assignment.getProductMilestoneMap().getMilestone() == null
                || assignment.getProductMilestoneMap()
                .getMilestone()
                .getDepartments() == null
                || assignment.getProductMilestoneMap()
                .getMilestone()
                .getDepartments()
                .isEmpty()) {

            logger.debug(
                    "No department found for assignmentId: {}",
                    assignment != null
                            ? assignment.getId()
                            : null
            );

            return null;
        }

        return assignment
                .getProductMilestoneMap()
                .getMilestone()
                .getDepartments()
                .get(0);
    }

    // =====================================================================
    // MAP RESPONSE
    // =====================================================================

    private ProjectReopenRequestResponseDto mapToResponseDto(
            ProjectReopenRequest request
    ) {

        logger.debug(
                "Mapping reopen request to response DTO. requestId: {}",
                request.getId()
        );

        ProjectReopenRequestResponseDto dto =
                new ProjectReopenRequestResponseDto();

        dto.setId(
                request.getId()
        );

        Project project =
                request.getProject();

        if (project != null) {

            dto.setProjectId(
                    project.getId()
            );

            dto.setProjectName(
                    project.getName()
            );

            dto.setProjectNo(
                    project.getProjectNo()
            );
        }

        ProjectMilestoneAssignment detected =
                request.getDetectedAtAssignment();

        if (detected != null) {

            dto.setDetectedAtAssignmentId(
                    detected.getId()
            );

            dto.setDetectedAtMilestoneName(
                    getMilestoneName(detected)
            );
        }

        ProjectMilestoneAssignment responsible =
                request.getResponsibleAssignment();

        if (responsible != null) {

            dto.setResponsibleAssignmentId(
                    responsible.getId()
            );

            dto.setResponsibleMilestoneName(
                    getMilestoneName(responsible)
            );
        }

        User requestedBy =
                request.getRequestedBy();

        if (requestedBy != null) {

            dto.setRequestedById(
                    requestedBy.getId()
            );

            dto.setRequestedByName(
                    getUserDisplayName(requestedBy)
            );
        }

        User requesterManager =
                request.getRequesterManager();

        if (requesterManager != null) {

            dto.setRequesterManagerId(
                    requesterManager.getId()
            );

            dto.setRequesterManagerName(
                    getUserDisplayName(requesterManager)
            );
        }

        User responsibleManager =
                request.getResponsibleManager();

        if (responsibleManager != null) {

            dto.setResponsibleManagerId(
                    responsibleManager.getId()
            );

            dto.setResponsibleManagerName(
                    getUserDisplayName(responsibleManager)
            );
        }

        dto.setRequestReason(
                request.getRequestReason()
        );

        dto.setRequesterManagerRemarks(
                request.getRequesterManagerRemarks()
        );

        dto.setResponsibleManagerRemarks(
                request.getResponsibleManagerRemarks()
        );

        dto.setStatus(
                request.getStatus()
        );

        dto.setRequestedAt(
                request.getRequestedAt()
        );

        dto.setRequesterManagerActionAt(
                request.getRequesterManagerActionAt()
        );

        dto.setResponsibleManagerActionAt(
                request.getResponsibleManagerActionAt()
        );

        dto.setCreatedDate(
                request.getCreatedDate()
        );

        dto.setUpdatedDate(
                request.getUpdatedDate()
        );

        return dto;
    }

    // =====================================================================
    // GET MILESTONE NAME
    // =====================================================================

    private String getMilestoneName(
            ProjectMilestoneAssignment assignment
    ) {

        if (assignment == null) {
            return "Milestone";
        }

        if (assignment.getMilestone() != null
                && assignment.getMilestone().getName() != null
                && !assignment.getMilestone()
                .getName()
                .trim()
                .isEmpty()) {

            return assignment
                    .getMilestone()
                    .getName()
                    .trim();
        }

        if (assignment.getProductMilestoneMap() != null
                && assignment.getProductMilestoneMap()
                .getMilestone() != null
                && assignment.getProductMilestoneMap()
                .getMilestone()
                .getName() != null
                && !assignment.getProductMilestoneMap()
                .getMilestone()
                .getName()
                .trim()
                .isEmpty()) {

            return assignment
                    .getProductMilestoneMap()
                    .getMilestone()
                    .getName()
                    .trim();
        }

        return "Milestone-"
                + assignment.getId();
    }

    // =====================================================================
    // GET USER DISPLAY NAME
    // =====================================================================

    private String getUserDisplayName(
            User user
    ) {

        if (user == null) {
            return "User";
        }

        if (user.getFullName() != null
                && !user.getFullName()
                .trim()
                .isEmpty()) {

            return user
                    .getFullName()
                    .trim();
        }

        if (user.getEmail() != null
                && !user.getEmail()
                .trim()
                .isEmpty()) {

            return user
                    .getEmail()
                    .trim();
        }

        return "User-"
                + user.getId();
    }
}