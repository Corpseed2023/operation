package com.doc.impl.milestone;

import com.doc.constants.StatusConstants;
import com.doc.dto.ProjectMilestoneassignment.MilestoneOnHoldDecisionDto;
import com.doc.dto.ProjectMilestoneassignment.MilestoneOnHoldResponseDto;
import com.doc.dto.ProjectMilestoneassignment.UpdateMilestoneStatusDto;
import com.doc.em.ProjectHistoryEventType;
import com.doc.em.ProjectHistoryReferenceType;
import com.doc.entity.milestone.*;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.MilestoneOnHoldRequestRepository;
import com.doc.repository.MilestoneStatusHistoryRepository;
import com.doc.repository.MilestoneStatusRepository;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.repository.UserRepository;
import com.doc.service.MilestoneOnHoldApprovalService;
import com.doc.service.project.ProjectHistoryEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class MilestoneOnHoldApprovalServiceImpl
        implements MilestoneOnHoldApprovalService {

    private static final Logger log =
            LoggerFactory.getLogger(MilestoneOnHoldApprovalServiceImpl.class);

    private final MilestoneOnHoldRequestRepository requestRepository;
    private final ProjectMilestoneAssignmentRepository assignmentRepository;
    private final MilestoneStatusRepository milestoneStatusRepository;
    private final MilestoneStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ProjectHistoryEventService historyEventService;

    public MilestoneOnHoldApprovalServiceImpl(
            MilestoneOnHoldRequestRepository requestRepository,
            ProjectMilestoneAssignmentRepository assignmentRepository,
            MilestoneStatusRepository milestoneStatusRepository,
            MilestoneStatusHistoryRepository historyRepository,
            UserRepository userRepository,
            ProjectHistoryEventService historyEventService
    ) {
        this.requestRepository = requestRepository;
        this.assignmentRepository = assignmentRepository;
        this.milestoneStatusRepository = milestoneStatusRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.historyEventService = historyEventService;
    }

    @Override
    public MilestoneOnHoldResponseDto requestOnHold(UpdateMilestoneStatusDto dto) {
        log.info("[MILESTONE-ON-HOLD-REQUEST-START] assignmentId={}, requestedById={}, reason={}",
                dto.getAssignmentId(), dto.getChangedById(), dto.getStatusReason());

        if (dto.getStatusReason() == null || dto.getStatusReason().isBlank()) {
            throw new ValidationException(
                    "Reason is required to put a milestone on hold",
                    "ON_HOLD_REASON_REQUIRED"
            );
        }

        ProjectMilestoneAssignment assignment = assignmentRepository
                .findActiveUserById(dto.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone assignment not found",
                        "MILESTONE_ASSIGNMENT_NOT_FOUND"
                ));

        User requester = userRepository.findActiveUserById(dto.getChangedById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "USER_NOT_FOUND"
                ));

        validateRequesterCanRequestOnHold(assignment, requester);

        String currentStatus = assignment.getStatus() == null
                ? null
                : assignment.getStatus().getName();

        if ("COMPLETED".equalsIgnoreCase(currentStatus)) {
            throw new ValidationException(
                    "Completed milestone cannot be put on hold",
                    "COMPLETED_MILESTONE_ON_HOLD_NOT_ALLOWED"
            );
        }

        if ("ON_HOLD".equalsIgnoreCase(currentStatus)) {
            throw new ValidationException(
                    "Milestone is already on hold",
                    "MILESTONE_ALREADY_ON_HOLD"
            );
        }

        /*
         * A PENDING request is only active when the milestone is still in the
         * same status that existed when the request was created.
         *
         * If another workflow already changed the milestone status, the old
         * ON_HOLD request is stale and is automatically closed as REJECTED.
         */
        if (hasActivePendingRequest(assignment)) {
            throw new ValidationException(
                    "An ON_HOLD approval request is already pending for this milestone",
                    "ON_HOLD_REQUEST_ALREADY_PENDING"
            );
        }

        /*
         * Resolve approver for the ON_HOLD request.
         *
         * Normal user:
         * - Must be current assignee.
         * - Assignee's manager becomes approver.
         *
         * ADMIN / ROLE_ADMIN / OPERATION_HEAD / ROLE_OPERATION_HEAD:
         * - Can request ON_HOLD even when the milestone has no assigned user.
         * - Can also request when the assignee has no manager configured.
         * - In those fallback cases, the privileged requester becomes approver.
         */
        boolean isAdmin = hasRole(requester, "ADMIN");
        boolean isOperationHead = hasRole(requester, "OPERATION_HEAD");
        boolean isPrivilegedRequester = isAdmin || isOperationHead;

        User assignedUser = assignment.getAssignedUser();
        User manager;

        /*
         * IMPORTANT:
         * ADMIN / OPERATION_HEAD are resolved FIRST.
         * They do not depend on milestone assignee or assignee manager.
         */
        if (isPrivilegedRequester) {

            manager = requester;

            log.info(
                    "[MILESTONE-ON-HOLD-PRIVILEGED-DIRECT] "
                            + "assignmentId={}, requesterId={}, admin={}, "
                            + "operationHead={}, assignedUserId={}, approverId={}",
                    assignment.getId(),
                    requester.getId(),
                    isAdmin,
                    isOperationHead,
                    assignedUser != null ? assignedUser.getId() : null,
                    manager.getId()
            );

        } else {

            /*
             * Normal user flow remains unchanged.
             */
            if (assignedUser == null) {
                throw new ValidationException(
                        "No user is currently assigned to this milestone",
                        "MILESTONE_ASSIGNEE_NOT_CONFIGURED"
                );
            }

            if (assignedUser.getManager() == null
                    || assignedUser.getManager().getId() == null) {
                throw new ValidationException(
                        "No manager is configured for the assigned user",
                        "REQUESTER_MANAGER_NOT_CONFIGURED"
                );
            }

            manager = userRepository
                    .findActiveUserById(assignedUser.getManager().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Assigned user's manager was not found or is inactive",
                            "MANAGER_NOT_FOUND"
                    ));
        }

        if (manager.getId().equals(requester.getId())
                && !isPrivilegedRequester) {
            throw new ValidationException(
                    "User cannot approve their own ON_HOLD request",
                    "SELF_APPROVAL_NOT_ALLOWED"
            );
        }

        MilestoneOnHoldRequest request = new MilestoneOnHoldRequest();
        request.setMilestoneAssignment(assignment);
        request.setRequestedBy(requester);
        request.setApprover(manager);
        request.setPreviousStatus(assignment.getStatus());
        request.setApprovalStatus(MilestoneOnHoldApprovalStatus.PENDING);
        request.setRequestReason(dto.getStatusReason().trim());
        request.setRequestedAt(LocalDateTime.now());

        MilestoneOnHoldRequest saved = requestRepository.save(request);

        log.info("[MILESTONE-ON-HOLD-REQUEST-CREATED] requestId={}, assignmentId={}, " +
                        "projectId={}, requestedById={}, managerId={}, status=PENDING",
                saved.getId(), assignment.getId(), assignment.getProject().getId(),
                requester.getId(), manager.getId());

        historyEventService.saveHistory(
                assignment.getProject().getId(),
                assignment.getId(),
                ProjectHistoryEventType.MILESTONE_ON_HOLD_REQUESTED,
                ProjectHistoryReferenceType.ON_HOLD_REQUEST,
                saved.getId(),
                "Milestone ON_HOLD requested",
                "ON_HOLD requested for milestone " + resolveMilestoneName(assignment),
                saved.getRequestReason(),
                currentStatus,
                "ON_HOLD (PENDING APPROVAL)",
                requester.getId(),
                requester.getFullName()
        );

        // Keep the milestone's current status unchanged until the manager approves.
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MilestoneOnHoldResponseDto decide(
            Long requestId,
            MilestoneOnHoldDecisionDto dto
    ) {
        log.info(
                "[MILESTONE-ON-HOLD-DECISION-START] requestId={}, userId={}, decision={}",
                requestId,
                dto.getManagerId(),
                dto.getDecision()
        );

        MilestoneOnHoldRequest request = requestRepository
                .findByIdForDecision(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ON_HOLD approval request not found",
                        "ON_HOLD_REQUEST_NOT_FOUND"
                ));

        if (request.getApprovalStatus()
                != MilestoneOnHoldApprovalStatus.PENDING) {

            throw new ValidationException(
                    "This ON_HOLD request has already been decided",
                    "ON_HOLD_REQUEST_ALREADY_DECIDED"
            );
        }

        User decisionBy = userRepository
                .findActiveUserById(dto.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Decision user not found",
                        "USER_NOT_FOUND"
                ));

        boolean isAssignedManager =
                request.getApprover() != null
                        && request.getApprover().getId() != null
                        && request.getApprover()
                        .getId()
                        .equals(decisionBy.getId());

        boolean isAdmin =
                decisionBy.getRoles() != null
                        && decisionBy.getRoles()
                        .stream()
                        .anyMatch(role ->
                                role != null
                                        && role.getName() != null
                                        && (
                                        "ADMIN".equalsIgnoreCase(
                                                role.getName().trim()
                                        )
                                                || "ROLE_ADMIN".equalsIgnoreCase(
                                                role.getName().trim()
                                        )
                                )
                        );

        boolean isOperationHead =
                decisionBy.getRoles() != null
                        && decisionBy.getRoles()
                        .stream()
                        .anyMatch(role ->
                                role != null
                                        && role.getName() != null
                                        && (
                                        "OPERATION_HEAD".equalsIgnoreCase(
                                                role.getName().trim()
                                        )
                                                || "ROLE_OPERATION_HEAD".equalsIgnoreCase(
                                                role.getName().trim()
                                        )
                                )
                        );

        boolean canDecide =
                isAssignedManager
                        || isAdmin
                        || isOperationHead;

        log.info(
                "[MILESTONE-ON-HOLD-DECISION-AUTHORIZATION] " +
                        "requestId={}, decisionById={}, assignedManagerId={}, " +
                        "assignedManager={}, admin={}, operationHead={}, allowed={}",
                requestId,
                decisionBy.getId(),
                request.getApprover() != null
                        ? request.getApprover().getId()
                        : null,
                isAssignedManager,
                isAdmin,
                isOperationHead,
                canDecide
        );

        if (!canDecide) {
            log.warn(
                    "[MILESTONE-ON-HOLD-DECISION-DENIED] " +
                            "requestId={}, expectedManagerId={}, actualUserId={}",
                    requestId,
                    request.getApprover() != null
                            ? request.getApprover().getId()
                            : null,
                    decisionBy.getId()
            );

            throw new ValidationException(
                    "Only ADMIN, OPERATION_HEAD, or the assigned manager can decide this request",
                    "NOT_AUTHORIZED_ON_HOLD_APPROVER"
            );
        }

        if (dto.getDecision() == null) {
            throw new ValidationException(
                    "Decision is required",
                    "ON_HOLD_DECISION_REQUIRED"
            );
        }

        if (dto.getDecision() == MilestoneOnHoldDecision.REJECT
                && (
                dto.getDecisionReason() == null
                        || dto.getDecisionReason().isBlank()
        )) {
            throw new ValidationException(
                    "Decision reason is required when rejecting an ON_HOLD request",
                    "ON_HOLD_REJECTION_REASON_REQUIRED"
            );
        }

        if (dto.getDecision() == MilestoneOnHoldDecision.APPROVE) {

            /*
             * The request may have become stale after it was submitted.
             *
             * Example:
             * request.previousStatus = IN_PROGRESS
             * current milestone status = NEW / REWORK / another status
             *
             * Previously approveRequest() threw an exception here and left the
             * request PENDING forever. That PENDING row then blocked send-back.
             *
             * Now stale requests are closed first and the API returns the closed
             * request instead of leaving an impossible PENDING state.
             */
            if (isRequestStatusStale(request)) {
                closeStaleRequest(
                        request,
                        decisionBy,
                        buildStaleStatusReason(request)
                );
            } else {
                approveRequest(
                        request,
                        decisionBy,
                        dto.getDecisionReason()
                );
            }

        } else if (dto.getDecision() == MilestoneOnHoldDecision.REJECT) {

            rejectRequest(
                    request,
                    dto.getDecisionReason(),
                    decisionBy
            );

        } else {
            throw new ValidationException(
                    "Invalid ON_HOLD decision",
                    "INVALID_ON_HOLD_DECISION"
            );
        }

        request.setDecidedAt(LocalDateTime.now());

        MilestoneOnHoldRequest saved =
                requestRepository.save(request);

        log.info(
                "[MILESTONE-ON-HOLD-DECISION-SUCCESS] " +
                        "requestId={}, assignmentId={}, decisionById={}, " +
                        "assignedManagerId={}, approvalStatus={}",
                saved.getId(),
                saved.getMilestoneAssignment().getId(),
                decisionBy.getId(),
                saved.getApprover().getId(),
                saved.getApprovalStatus()
        );

        return toResponse(saved);
    }


    private void approveRequest(
            MilestoneOnHoldRequest request,
            User manager,
            String decisionReason
    ) {
        ProjectMilestoneAssignment assignment = request.getMilestoneAssignment();

        boolean requestedByPrivilegedUser =
                hasRole(request.getRequestedBy(), "ADMIN")
                        || hasRole(request.getRequestedBy(), "OPERATION_HEAD");

        /*
         * Preserve the original assignee-protection for normal users.
         * ADMIN / OPERATION_HEAD may create a request on behalf of the current
         * assignee, so requestedBy does not have to equal assignedUser for them.
         */
        if (!requestedByPrivilegedUser
                && (
                assignment.getAssignedUser() == null
                        || !assignment.getAssignedUser()
                        .getId()
                        .equals(request.getRequestedBy().getId())
        )) {
            throw new ValidationException(
                    "Milestone assignee changed after the request was submitted",
                    "ON_HOLD_REQUEST_ASSIGNEE_CHANGED"
            );
        }

        if (assignment.getStatus() == null
                || !assignment.getStatus().getId().equals(request.getPreviousStatus().getId())) {
            throw new ValidationException(
                    "Milestone status changed after the request was submitted",
                    "ON_HOLD_REQUEST_STATUS_CHANGED"
            );
        }

        MilestoneStatus onHoldStatus = milestoneStatusRepository
                .findById(StatusConstants.MILESTONE_ON_HOLD_ID)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ON_HOLD milestone status not found",
                        "STATUS_NOT_FOUND"
                ));

        MilestoneStatusHistory history = new MilestoneStatusHistory();
        history.setMilestoneAssignment(assignment);
        history.setPreviousStatus(assignment.getStatus());
        history.setNewStatus(onHoldStatus);
        history.setChangeReason(request.getRequestReason());
        history.setChangedBy(manager);
        history.setChangeDate(new Date());
        history.setDeleted(false);
        historyRepository.save(history);

        assignment.setStatus(onHoldStatus);
        assignment.setStatusReason(request.getRequestReason());
        assignment.setUpdatedBy(manager.getId());
        assignment.setUpdatedDate(new Date());
        assignmentRepository.save(assignment);

        request.setApprovalStatus(MilestoneOnHoldApprovalStatus.APPROVED);
        request.setDecisionReason(blankToNull(decisionReason));

        historyEventService.saveHistory(
                assignment.getProject().getId(),
                assignment.getId(),
                ProjectHistoryEventType.MILESTONE_ON_HOLD_APPROVED,
                ProjectHistoryReferenceType.ON_HOLD_REQUEST,
                request.getId(),
                "Milestone ON_HOLD approved",
                "ON_HOLD request approved for milestone "
                        + resolveMilestoneName(assignment),
                blankToNull(decisionReason) != null
                        ? decisionReason.trim()
                        : request.getRequestReason(),
                request.getPreviousStatus() != null
                        ? request.getPreviousStatus().getName()
                        : null,
                onHoldStatus.getName(),
                manager.getId(),
                manager.getFullName()
        );
    }

    private void rejectRequest(
            MilestoneOnHoldRequest request,
            String decisionReason,
            User decisionBy
    ) {
        // The milestone was never changed, so rejection only closes the request.
        request.setApprovalStatus(MilestoneOnHoldApprovalStatus.REJECTED);
        request.setDecisionReason(decisionReason.trim());

        ProjectMilestoneAssignment assignment =
                request.getMilestoneAssignment();

        historyEventService.saveHistory(
                assignment.getProject().getId(),
                assignment.getId(),
                ProjectHistoryEventType.MILESTONE_ON_HOLD_REJECTED,
                ProjectHistoryReferenceType.ON_HOLD_REQUEST,
                request.getId(),
                "Milestone ON_HOLD rejected",
                "ON_HOLD request rejected for milestone "
                        + resolveMilestoneName(assignment),
                decisionReason.trim(),
                request.getPreviousStatus() != null
                        ? request.getPreviousStatus().getName()
                        : null,
                assignment.getStatus() != null
                        ? assignment.getStatus().getName()
                        : null,
                decisionBy.getId(),
                decisionBy.getFullName()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MilestoneOnHoldResponseDto> getManagerQueue(
            Long userId,
            MilestoneOnHoldApprovalStatus approvalStatus,
            Pageable pageable
    ) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "USER_NOT_FOUND"
                ));

        boolean isAdmin = hasRole(user, "ADMIN");
        boolean isOperationHead = hasRole(user, "OPERATION_HEAD");

        log.info(
                "[MILESTONE-ON-HOLD-QUEUE-ACCESS] userId={}, admin={}, " +
                        "operationHead={}, manager={}",
                userId,
                isAdmin,
                isOperationHead,
                user.isManagerFlag()
        );

        /*
         * ADMIN and OPERATION_HEAD can see every request.
         */
        if (isAdmin || isOperationHead) {
            return requestRepository
                    .findAllRequestQueue(approvalStatus, pageable)
                    .map(this::toResponse);
        }

        /*
         * Other users must be managers.
         */
        if (!user.isManagerFlag()) {
            log.warn(
                    "[MILESTONE-ON-HOLD-QUEUE-DENIED] userId={}, roles={}",
                    userId,
                    getRoleNames(user)
            );

            throw new ValidationException(
                    "Only ADMIN, OPERATION_HEAD, or a manager can view ON_HOLD requests",
                    "NOT_AUTHORIZED_TO_VIEW_ON_HOLD_REQUESTS"
            );
        }

        /*
         * A normal manager sees only requests where they are the approver.
         */
        return requestRepository
                .findManagerQueue(userId, approvalStatus, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public boolean hasPendingRequest(Long assignmentId) {

        ProjectMilestoneAssignment assignment = assignmentRepository
                .findActiveUserById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone assignment not found",
                        "MILESTONE_ASSIGNMENT_NOT_FOUND"
                ));

        /*
         * Important:
         * Do not treat a stale PENDING row as an active workflow.
         *
         * This is what caused:
         * 1. ON_HOLD approval -> ON_HOLD_REQUEST_STATUS_CHANGED
         * 2. send-back       -> ON_HOLD_APPROVAL_PENDING
         *
         * forever for the same milestone.
         */
        return hasActivePendingRequest(assignment);
    }

    private boolean hasActivePendingRequest(
            ProjectMilestoneAssignment assignment
    ) {

        MilestoneOnHoldRequest pendingRequest = requestRepository
                .findTopByMilestoneAssignment_IdAndApprovalStatusOrderByRequestedAtDesc(
                        assignment.getId(),
                        MilestoneOnHoldApprovalStatus.PENDING
                )
                .orElse(null);

        if (pendingRequest == null) {
            return false;
        }

        if (!isRequestStatusStale(pendingRequest)) {
            return true;
        }

        String staleReason = buildStaleStatusReason(pendingRequest);

        closeStaleRequest(
                pendingRequest,
                null,
                staleReason
        );

        pendingRequest.setDecidedAt(LocalDateTime.now());
        requestRepository.save(pendingRequest);

        log.warn(
                "[MILESTONE-ON-HOLD-STALE-PENDING-CLOSED] " +
                        "requestId={}, assignmentId={}, previousStatus={}, currentStatus={}, reason={}",
                pendingRequest.getId(),
                assignment.getId(),
                pendingRequest.getPreviousStatus() != null
                        ? pendingRequest.getPreviousStatus().getName()
                        : null,
                assignment.getStatus() != null
                        ? assignment.getStatus().getName()
                        : null,
                staleReason
        );

        return false;
    }

    private boolean isRequestStatusStale(
            MilestoneOnHoldRequest request
    ) {

        if (request == null
                || request.getMilestoneAssignment() == null) {
            return true;
        }

        MilestoneStatus requestedFromStatus =
                request.getPreviousStatus();

        MilestoneStatus currentStatus =
                request.getMilestoneAssignment().getStatus();

        Long requestedFromStatusId =
                requestedFromStatus != null
                        ? requestedFromStatus.getId()
                        : null;

        Long currentStatusId =
                currentStatus != null
                        ? currentStatus.getId()
                        : null;

        return !Objects.equals(
                requestedFromStatusId,
                currentStatusId
        );
    }

    private String buildStaleStatusReason(
            MilestoneOnHoldRequest request
    ) {

        String previousStatusName =
                request != null
                        && request.getPreviousStatus() != null
                        ? request.getPreviousStatus().getName()
                        : null;

        String currentStatusName =
                request != null
                        && request.getMilestoneAssignment() != null
                        && request.getMilestoneAssignment().getStatus() != null
                        ? request.getMilestoneAssignment()
                        .getStatus()
                        .getName()
                        : null;

        return "ON_HOLD request automatically closed because milestone status changed from "
                + previousStatusName
                + " to "
                + currentStatusName
                + " after the request was submitted";
    }

    private void closeStaleRequest(
            MilestoneOnHoldRequest request,
            User closedBy,
            String reason
    ) {

        ProjectMilestoneAssignment assignment =
                request.getMilestoneAssignment();

        request.setApprovalStatus(
                MilestoneOnHoldApprovalStatus.REJECTED
        );
        request.setDecisionReason(reason);

        /*
         * Use the existing REJECTED event type so no enum/schema change is
         * required. The description clearly records that this was an automatic
         * stale-request closure, not a manager rejection.
         */
        historyEventService.saveHistory(
                assignment.getProject().getId(),
                assignment.getId(),
                ProjectHistoryEventType.MILESTONE_ON_HOLD_REJECTED,
                ProjectHistoryReferenceType.ON_HOLD_REQUEST,
                request.getId(),
                "Stale milestone ON_HOLD request closed",
                "Pending ON_HOLD request automatically closed because the milestone status changed after submission",
                reason,
                request.getPreviousStatus() != null
                        ? request.getPreviousStatus().getName()
                        : null,
                assignment.getStatus() != null
                        ? assignment.getStatus().getName()
                        : null,
                closedBy != null
                        ? closedBy.getId()
                        : null,
                closedBy != null
                        ? closedBy.getFullName()
                        : "System"
        );
    }

    private void validateRequesterCanRequestOnHold(
            ProjectMilestoneAssignment assignment,
            User requester
    ) {
        boolean isCurrentAssignee =
                assignment.getAssignedUser() != null
                        && assignment.getAssignedUser().getId() != null
                        && assignment.getAssignedUser()
                        .getId()
                        .equals(requester.getId());

        boolean isAdmin = hasRole(requester, "ADMIN");
        boolean isOperationHead = hasRole(requester, "OPERATION_HEAD");

        boolean allowed =
                isCurrentAssignee
                        || isAdmin
                        || isOperationHead;

        log.info(
                "[MILESTONE-ON-HOLD-REQUEST-AUTHORIZATION] "
                        + "assignmentId={}, requesterId={}, currentAssignee={}, "
                        + "admin={}, operationHead={}, allowed={}",
                assignment.getId(),
                requester.getId(),
                isCurrentAssignee,
                isAdmin,
                isOperationHead,
                allowed
        );

        if (!allowed) {
            throw new ValidationException(
                    "Only the currently assigned user, ADMIN, or OPERATION_HEAD can request ON_HOLD",
                    "NOT_AUTHORIZED_TO_REQUEST_ON_HOLD"
            );
        }
    }

    private MilestoneOnHoldResponseDto toResponse(MilestoneOnHoldRequest request) {
        ProjectMilestoneAssignment assignment = request.getMilestoneAssignment();
        Project project = assignment.getProject();

        return MilestoneOnHoldResponseDto.builder()
                .requestId(request.getId())
                .assignmentId(assignment.getId())
                .projectId(project.getId())
                .projectNumber(project.getProjectNo())
                .projectName(project.getName())
                .milestoneName(resolveMilestoneName(assignment))
                .currentMilestoneStatus(
                        assignment.getStatus() == null ? null : assignment.getStatus().getName()
                )
                .requestedById(request.getRequestedBy().getId())
                .requestedByName(request.getRequestedBy().getFullName())
                .managerId(request.getApprover().getId())
                .managerName(request.getApprover().getFullName())
                .approvalStatus(request.getApprovalStatus())
                .requestReason(request.getRequestReason())
                .decisionReason(request.getDecisionReason())
                .requestedAt(request.getRequestedAt())
                .decidedAt(request.getDecidedAt())
                .build();
    }

    private boolean hasRole(User user, String expectedRole) {
        if (user == null || user.getRoles() == null || expectedRole == null) {
            return false;
        }

        return user.getRoles()
                .stream()
                .filter(role -> role != null && role.getName() != null)
                .map(role -> normalizeRoleName(role.getName()))
                .anyMatch(expectedRole::equalsIgnoreCase);
    }

    private String normalizeRoleName(String roleName) {
        String normalized = roleName.trim();

        if (normalized.regionMatches(
                true,
                0,
                "ROLE_",
                0,
                "ROLE_".length()
        )) {
            normalized = normalized.substring("ROLE_".length());
        }

        return normalized;
    }

    private List<String> getRoleNames(User user) {
        if (user == null || user.getRoles() == null) {
            return List.of();
        }

        return user.getRoles()
                .stream()
                .filter(role -> role != null && role.getName() != null)
                .map(role -> role.getName())
                .toList();
    }

    private String resolveMilestoneName(ProjectMilestoneAssignment assignment) {
        if (assignment.getProductMilestoneMap() == null
                || assignment.getProductMilestoneMap().getMilestone() == null) {
            return null;
        }
        return assignment.getProductMilestoneMap().getMilestone().getName();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}