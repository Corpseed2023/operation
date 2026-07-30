package com.doc.impl.project;

import com.doc.constants.StatusConstants;
import com.doc.dto.project.lifecycle.CreateProjectLifecycleRequestDto;
import com.doc.dto.project.lifecycle.ProjectLifecycleDecisionDto;
import com.doc.dto.project.lifecycle.ProjectLifecycleResponseDto;
import com.doc.entity.project.*;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.projectRepo.ProjectLifecycleRequestRepository;
import com.doc.repository.projectRepo.ProjectStatusRepository;
import com.doc.service.ProjectLifecycleRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProjectLifecycleRequestServiceImpl
        implements ProjectLifecycleRequestService {

    private static final String CRT_ROLE = "CRT";
    private static final String ADMIN_ROLE = "ADMIN";

    private final ProjectLifecycleRequestRepository lifecycleRequestRepository;
    private final ProjectRepository projectRepository;
    private final ProjectStatusRepository projectStatusRepository;
    private final UserRepository userRepository;

    /**
     * CRT submits FORCE_CLOSE or REOPEN request.
     *
     * This method does not change the project status.
     * Project status changes only after ADMIN approval.
     */
    @Override
    @Transactional
    public ProjectLifecycleResponseDto createRequest(
            CreateProjectLifecycleRequestDto requestDto
    ) {
        log.info("[LIFECYCLE-CREATE-START] projectId={}, actionType={}, requestedById={}",
                requestDto != null ? requestDto.getProjectId() : null,
                requestDto != null ? requestDto.getActionType() : null,
                requestDto != null ? requestDto.getRequestedById() : null);

        validateCreateDto(requestDto);

        User requestedBy = userRepository
                .findActiveUserById(requestDto.getRequestedById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requesting user not found",
                        "ERR_REQUESTING_USER_NOT_FOUND"
                ));

        validateCrtOrAdminUser(requestedBy);

        Project project = projectRepository
                .findByIdForLifecycleUpdate(requestDto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "ERR_PROJECT_NOT_FOUND"
                ));

        validateProjectForNewRequest(
                project,
                requestDto.getActionType()
        );

        boolean pendingRequestExists =
                lifecycleRequestRepository
                        .existsByProjectIdAndRequestStatusAndDeletedFalse(
                                project.getId(),
                                ProjectLifecycleRequestStatus.PENDING
                        );

        if (pendingRequestExists) {
            throw new ValidationException(
                    "A lifecycle request is already pending for this project",
                    "ERR_PROJECT_LIFECYCLE_REQUEST_ALREADY_PENDING"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        ProjectLifecycleRequest request =
                new ProjectLifecycleRequest();

        request.setProject(project);
        request.setActionType(requestDto.getActionType());
        request.setRequestStatus(
                ProjectLifecycleRequestStatus.PENDING
        );

        request.setRequestedBy(requestedBy);
        request.setRequestReason(
                requestDto.getRequestReason().trim()
        );

        request.setPreviousProjectStatus(project.getStatus());

        request.setRequestedAt(now);
        request.setCreatedDate(now);
        request.setUpdatedDate(now);

        request.setCreatedBy(requestedBy.getId());
        request.setUpdatedBy(requestedBy.getId());
        request.setDeleted(false);

        ProjectLifecycleRequest savedRequest =
                lifecycleRequestRepository.save(request);

        log.info(
                "[LIFECYCLE-CREATE-COMPLETED] requestId={}, projectId={}, action={}, requestedBy={}",
                savedRequest.getId(),
                project.getId(),
                savedRequest.getActionType(),
                requestedBy.getId()
        );

        return mapToResponseDto(savedRequest);
    }

    /**
     * ADMIN approves or rejects a pending lifecycle request.
     */
    @Override
    @Transactional
    public ProjectLifecycleResponseDto reviewRequest(
            Long requestId,
            ProjectLifecycleDecisionDto decisionDto
    ) {
        log.info("[LIFECYCLE-REVIEW-START] requestId={}, decision={}, reviewedById={}",
                requestId,
                decisionDto != null ? decisionDto.getDecision() : null,
                decisionDto != null ? decisionDto.getReviewedById() : null);

        if (requestId == null) {
            throw new ValidationException(
                    "Request ID is required",
                    "ERR_REQUEST_ID_REQUIRED"
            );
        }

        if (decisionDto == null) {
            throw new ValidationException(
                    "Decision request is required",
                    "ERR_DECISION_REQUEST_REQUIRED"
            );
        }

        if (decisionDto.getDecision() == null) {
            throw new ValidationException(
                    "Decision is required",
                    "ERR_DECISION_REQUIRED"
            );
        }

        if (decisionDto.getReviewedById() == null) {
            throw new ValidationException(
                    "Reviewed by user ID is required",
                    "ERR_REVIEWED_BY_REQUIRED"
            );
        }

        User reviewedBy = userRepository
                .findActiveUserById(decisionDto.getReviewedById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reviewing user not found",
                        "ERR_REVIEWING_USER_NOT_FOUND"
                ));

        validateAdminUser(reviewedBy);

        ProjectLifecycleRequest request =
                lifecycleRequestRepository
                        .findByIdForReview(requestId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Project lifecycle request not found",
                                "ERR_PROJECT_LIFECYCLE_REQUEST_NOT_FOUND"
                        ));

        if (request.getRequestStatus()
                != ProjectLifecycleRequestStatus.PENDING) {

            throw new ValidationException(
                    "Only a PENDING lifecycle request can be reviewed",
                    "ERR_LIFECYCLE_REQUEST_ALREADY_REVIEWED"
            );
        }

        Project project = projectRepository
                .findByIdForLifecycleUpdate(
                        request.getProject().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "ERR_PROJECT_NOT_FOUND"
                ));

        LocalDateTime now = LocalDateTime.now();

        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(now);
        request.setReviewRemark(
                StringUtils.hasText(decisionDto.getReviewRemark())
                        ? decisionDto.getReviewRemark().trim()
                        : null
        );
        request.setUpdatedBy(reviewedBy.getId());
        request.setUpdatedDate(now);

        if (decisionDto.getDecision()
                == ProjectLifecycleDecision.REJECT) {

            request.setRequestStatus(
                    ProjectLifecycleRequestStatus.REJECTED
            );

            ProjectLifecycleRequest rejectedRequest =
                    lifecycleRequestRepository.save(request);

            log.info(
                    "[LIFECYCLE-REVIEW-REJECTED] requestId={}, projectId={}, action={}, reviewedBy={}",
                    request.getId(),
                    project.getId(),
                    request.getActionType(),
                    reviewedBy.getId()
            );

            return mapToResponseDto(rejectedRequest);
        }

        applyApprovedAction(
                project,
                request,
                reviewedBy
        );

        request.setRequestStatus(
                ProjectLifecycleRequestStatus.APPROVED
        );

        projectRepository.save(project);

        ProjectLifecycleRequest approvedRequest =
                lifecycleRequestRepository.save(request);

        log.info(
                "[LIFECYCLE-REVIEW-APPROVED] requestId={}, projectId={}, action={}, newStatus={}, reviewedBy={}",
                request.getId(),
                project.getId(),
                request.getActionType(),
                project.getStatus() != null
                        ? project.getStatus().getName()
                        : null,
                reviewedBy.getId()
        );

        return mapToResponseDto(approvedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectLifecycleResponseDto> getPendingRequests(
            Long adminUserId,
            int page,
            int size
    ) {
        log.debug("[LIFECYCLE-PENDING-LIST-START] adminUserId={}, page={}, size={}",
                adminUserId, page, size);

        User adminUser = userRepository
                .findActiveUserById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Admin user not found",
                        "ERR_ADMIN_USER_NOT_FOUND"
                ));

        validateAdminUser(adminUser);
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "requestedAt"
                )
        );

        log.debug("[LIFECYCLE-PENDING-LIST-QUERY] status={}, page={}, size={}",
                ProjectLifecycleRequestStatus.PENDING, page, size);

        return lifecycleRequestRepository
                .findByRequestStatusAndDeletedFalseOrderByRequestedAtDesc(
                        ProjectLifecycleRequestStatus.PENDING,
                        pageable
                )
                .map(this::mapToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectLifecycleResponseDto> getMyRequests(
            Long userId,
            int page,
            int size
    ) {
        log.debug("[LIFECYCLE-MY-REQUESTS-START] userId={}, page={}, size={}",
                userId, page, size);

        User user = userRepository
                .findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        validatePagination(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "requestedAt"
                )
        );

        log.debug("[LIFECYCLE-MY-REQUESTS-QUERY] userId={}, page={}, size={}",
                user.getId(), page, size);

        return lifecycleRequestRepository
                .findByRequestedByIdAndDeletedFalseOrderByRequestedAtDesc(
                        user.getId(),
                        pageable
                )
                .map(this::mapToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectLifecycleResponseDto> getProjectRequestHistory(
            Long projectId,
            Long userId
    ) {
        log.debug("[LIFECYCLE-HISTORY-START] projectId={}, userId={}", projectId, userId);

        if (projectId == null) {
            throw new ValidationException(
                    "Project ID is required",
                    "ERR_PROJECT_ID_REQUIRED"
            );
        }

        User user = userRepository
                .findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        boolean allowed =
                hasRole(user, ADMIN_ROLE)
                        || hasRole(user, CRT_ROLE)
                        || hasRole(user, "OPERATION_HEAD");

        if (!allowed) {
            throw new ValidationException(
                    "You are not authorized to view project lifecycle history",
                    "ERR_UNAUTHORIZED_LIFECYCLE_HISTORY"
            );
        }

        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException(
                    "Project not found",
                    "ERR_PROJECT_NOT_FOUND"
            );
        }

        log.debug("[LIFECYCLE-HISTORY-QUERY] projectId={}", projectId);

        return lifecycleRequestRepository
                .findByProjectIdAndDeletedFalseOrderByRequestedAtDesc(
                        projectId
                )
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectLifecycleResponseDto getRequestById(
            Long requestId,
            Long userId
    ) {
        log.debug("[LIFECYCLE-GET-BY-ID-START] requestId={}, userId={}", requestId, userId);

        User user = userRepository
                .findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        ProjectLifecycleRequest request =
                lifecycleRequestRepository
                        .findById(requestId)
                        .filter(item -> !item.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Project lifecycle request not found",
                                "ERR_PROJECT_LIFECYCLE_REQUEST_NOT_FOUND"
                        ));

        boolean isAdmin = hasRole(user, ADMIN_ROLE);
        boolean isCrt = hasRole(user, CRT_ROLE);
        boolean isRequester =
                request.getRequestedBy() != null
                        && request.getRequestedBy()
                        .getId()
                        .equals(user.getId());

        if (!isAdmin && !isCrt && !isRequester) {
            throw new ValidationException(
                    "You are not authorized to view this lifecycle request",
                    "ERR_UNAUTHORIZED_LIFECYCLE_REQUEST"
            );
        }

        log.debug("[LIFECYCLE-GET-BY-ID-COMPLETED] requestId={}, status={}",
                request.getId(), request.getRequestStatus());

        return mapToResponseDto(request);
    }

    private void applyApprovedAction(
            Project project,
            ProjectLifecycleRequest request,
            User reviewedBy
    ) {
        ProjectLifecycleAction action =
                request.getActionType();

        log.info("[LIFECYCLE-APPLY-ACTION] requestId={}, projectId={}, action={}, reviewedBy={}",
                request.getId(), project.getId(), action, reviewedBy.getId());

        if (action == ProjectLifecycleAction.FORCE_CLOSE) {
            applyForceClose(project, reviewedBy);
            return;
        }

        if (action == ProjectLifecycleAction.REOPEN) {
            applyReopen(project, reviewedBy);
            return;
        }

        throw new ValidationException(
                "Unsupported lifecycle action: " + action,
                "ERR_UNSUPPORTED_LIFECYCLE_ACTION"
        );
    }

    private void applyForceClose(
            Project project,
            User reviewedBy
    ) {
        log.info("[PROJECT-FORCE-CLOSE-START] projectId={}, currentStatusId={}, reviewedBy={}",
                project.getId(),
                project.getStatus() != null ? project.getStatus().getId() : null,
                reviewedBy.getId());

        validateProjectForForceClose(project);

        ProjectStatus forceClosedStatus =
                projectStatusRepository
                        .findById(
                                StatusConstants
                                        .PROJECT_FORCE_CLOSED_ID
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "System project status FORCE_CLOSED is missing",
                                        "ERR_FORCE_CLOSED_STATUS_MISSING"
                                )
                        );

        project.setStatus(forceClosedStatus);

        /*
         * Do not set cancelled=true.
         *
         * CANCELLED is a separate existing business flow.
         * Many current repository methods filter isCancelled=false.
         */
        project.setCancelled(false);
        project.setActive(false);

        project.setUpdatedBy(reviewedBy.getId());
        project.setUpdatedDate(new java.util.Date());

        log.info("[PROJECT-FORCE-CLOSE-COMPLETED] projectId={}, newStatusId={}, active={}",
                project.getId(), forceClosedStatus.getId(), project.isActive());
    }

    private void applyReopen(
            Project project,
            User reviewedBy
    ) {
        log.info("[PROJECT-REOPEN-START] projectId={}, currentStatusId={}, reviewedBy={}",
                project.getId(),
                project.getStatus() != null ? project.getStatus().getId() : null,
                reviewedBy.getId());

        validateProjectForReopen(project);

        ProjectStatus reopenedStatus =
                projectStatusRepository
                        .findById(
                                StatusConstants.PROJECT_REOPENED_ID
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "System project status REOPENED is missing",
                                        "ERR_REOPENED_STATUS_MISSING"
                                )
                        );

        project.setStatus(reopenedStatus);
        project.setCancelled(false);
        project.setActive(true);
        project.setCancellerId(null);

        project.setUpdatedBy(reviewedBy.getId());
        project.setUpdatedDate(new java.util.Date());

        log.info("[PROJECT-REOPEN-COMPLETED] projectId={}, newStatusId={}, active={}",
                project.getId(), reopenedStatus.getId(), project.isActive());
    }

    private void validateProjectForNewRequest(
            Project project,
            ProjectLifecycleAction action
    ) {
        if (project.isDeleted()) {
            throw new ValidationException(
                    "Deleted project cannot be used for lifecycle request",
                    "ERR_PROJECT_DELETED"
            );
        }

        if (project.getStatus() == null) {
            throw new ValidationException(
                    "Project status is missing",
                    "ERR_PROJECT_STATUS_MISSING"
            );
        }

        if (action == ProjectLifecycleAction.FORCE_CLOSE) {
            validateProjectForForceClose(project);
            return;
        }

        if (action == ProjectLifecycleAction.REOPEN) {
            validateProjectForReopen(project);
            return;
        }

        throw new ValidationException(
                "Unsupported lifecycle action",
                "ERR_UNSUPPORTED_LIFECYCLE_ACTION"
        );
    }

    private void validateProjectForForceClose(
            Project project
    ) {
        Long currentStatusId =
                project.getStatus() != null
                        ? project.getStatus().getId()
                        : null;

        if (StatusConstants.PROJECT_FORCE_CLOSED_ID
                .equals(currentStatusId)) {

            throw new ValidationException(
                    "Project is already force closed",
                    "ERR_PROJECT_ALREADY_FORCE_CLOSED"
            );
        }

        if (StatusConstants.PROJECT_CANCELLED_ID
                .equals(currentStatusId)
                || project.isCancelled()) {

            throw new ValidationException(
                    "Cancelled project cannot be force closed",
                    "ERR_CANCELLED_PROJECT_FORCE_CLOSE_NOT_ALLOWED"
            );
        }

        if (StatusConstants.PROJECT_REFUNDED_ID
                .equals(currentStatusId)) {

            throw new ValidationException(
                    "Refunded project cannot be force closed",
                    "ERR_REFUNDED_PROJECT_FORCE_CLOSE_NOT_ALLOWED"
            );
        }

        boolean allowedStatus =
                StatusConstants.PROJECT_OPEN_ID
                        .equals(currentStatusId)
                        || StatusConstants.PROJECT_IN_PROGRESS_ID
                        .equals(currentStatusId)
                        || StatusConstants.PROJECT_COMPLETED_ID
                        .equals(currentStatusId)
                        || StatusConstants.PROJECT_REOPENED_ID
                        .equals(currentStatusId);

        if (!allowedStatus) {
            throw new ValidationException(
                    "Project cannot be force closed from current status",
                    "ERR_INVALID_FORCE_CLOSE_STATUS"
            );
        }
    }

    private void validateProjectForReopen(
            Project project
    ) {
        Long currentStatusId =
                project.getStatus() != null
                        ? project.getStatus().getId()
                        : null;

        if (!StatusConstants.PROJECT_FORCE_CLOSED_ID
                .equals(currentStatusId)) {

            throw new ValidationException(
                    "Only FORCE_CLOSED project can be reopened",
                    "ERR_REOPEN_ONLY_FORCE_CLOSED_PROJECT"
            );
        }
    }

    private void validateCreateDto(
            CreateProjectLifecycleRequestDto requestDto
    ) {
        if (requestDto == null) {
            throw new ValidationException(
                    "Lifecycle request is required",
                    "ERR_LIFECYCLE_REQUEST_REQUIRED"
            );
        }

        if (requestDto.getProjectId() == null) {
            throw new ValidationException(
                    "Project ID is required",
                    "ERR_PROJECT_ID_REQUIRED"
            );
        }

        if (requestDto.getRequestedById() == null) {
            throw new ValidationException(
                    "Requested by user ID is required",
                    "ERR_REQUESTED_BY_REQUIRED"
            );
        }

        if (requestDto.getActionType() == null) {
            throw new ValidationException(
                    "Lifecycle action is required",
                    "ERR_LIFECYCLE_ACTION_REQUIRED"
            );
        }

        if (!StringUtils.hasText(
                requestDto.getRequestReason()
        )) {
            throw new ValidationException(
                    "Request reason is required",
                    "ERR_REQUEST_REASON_REQUIRED"
            );
        }
    }

    private void validateCrtOrAdminUser(User user) {

        if (user == null) {
            throw new ValidationException(
                    "User is required",
                    "ERR_USER_REQUIRED"
            );
        }

        boolean isCrtDepartment =
                user.getDepartments() != null
                        && user.getDepartments()
                        .stream()
                        .anyMatch(department ->
                                department != null
                                        && !department.isDeleted()
                                        && department.getName() != null
                                        && "CRT".equalsIgnoreCase(
                                        department.getName().trim()
                                )
                        );

        boolean isAdmin =
                user.getRoles() != null
                        && user.getRoles()
                        .stream()
                        .anyMatch(role ->
                                role != null
                                        && !role.isDeleted()
                                        && role.getName() != null
                                        && "ADMIN".equalsIgnoreCase(
                                        role.getName().trim()
                                )
                        );

        if (!isCrtDepartment && !isAdmin) {
            throw new ValidationException(
                    "Only CRT department users or ADMIN users can submit force-close or reopen requests",
                    "ERR_ONLY_CRT_OR_ADMIN_CAN_CREATE_LIFECYCLE_REQUEST"
            );
        }
    }


    private void validateAdminUser(User user) {
        if (!hasRole(user, ADMIN_ROLE)) {
            throw new ValidationException(
                    "Only ADMIN can approve or reject lifecycle request",
                    "ERR_ONLY_ADMIN_CAN_REVIEW_LIFECYCLE_REQUEST"
            );
        }
    }

    private boolean hasRole(
            User user,
            String roleName
    ) {
        return user != null
                && user.getRoles() != null
                && user.getRoles()
                .stream()
                .anyMatch(role ->
                        role != null
                                && role.getName() != null
                                && roleName.equalsIgnoreCase(
                                role.getName()
                        )
                );
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new ValidationException(
                    "Page number cannot be negative",
                    "ERR_INVALID_PAGE_NUMBER"
            );
        }

        if (size < 1) {
            throw new ValidationException(
                    "Page size must be greater than zero",
                    "ERR_INVALID_PAGE_SIZE"
            );
        }
    }

    private ProjectLifecycleResponseDto mapToResponseDto(
            ProjectLifecycleRequest request
    ) {
        ProjectLifecycleResponseDto dto =
                new ProjectLifecycleResponseDto();

        dto.setId(request.getId());

        if (request.getProject() != null) {
            dto.setProjectId(request.getProject().getId());
            dto.setProjectNumber(
                    request.getProject().getProjectNo()
            );
            dto.setProjectName(
                    request.getProject().getName()
            );

            if (request.getProject().getStatus() != null) {
                dto.setCurrentProjectStatusId(
                        request.getProject()
                                .getStatus()
                                .getId()
                );
                dto.setCurrentProjectStatusName(
                        request.getProject()
                                .getStatus()
                                .getName()
                );
            }
        }

        dto.setActionType(request.getActionType());
        dto.setRequestStatus(request.getRequestStatus());

        if (request.getRequestedBy() != null) {
            dto.setRequestedById(
                    request.getRequestedBy().getId()
            );
            dto.setRequestedByName(
                    request.getRequestedBy().getFullName()
            );
        }

        if (request.getReviewedBy() != null) {
            dto.setReviewedById(
                    request.getReviewedBy().getId()
            );
            dto.setReviewedByName(
                    request.getReviewedBy().getFullName()
            );
        }

        dto.setRequestReason(request.getRequestReason());
        dto.setReviewRemark(request.getReviewRemark());

        if (request.getPreviousProjectStatus() != null) {
            dto.setPreviousProjectStatusId(
                    request.getPreviousProjectStatus().getId()
            );
            dto.setPreviousProjectStatusName(
                    request.getPreviousProjectStatus().getName()
            );
        }

        dto.setRequestedAt(request.getRequestedAt());
        dto.setReviewedAt(request.getReviewedAt());

        return dto;
    }
}