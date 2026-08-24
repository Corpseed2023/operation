package com.doc.impl.milestone;

import com.doc.constants.StatusConstants;
import com.doc.dto.ProjectMilestoneassignment.MilestoneOnHoldDecisionDto;
import com.doc.dto.ProjectMilestoneassignment.MilestoneOnHoldResponseDto;
import com.doc.dto.ProjectMilestoneassignment.UpdateMilestoneStatusDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

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

    public MilestoneOnHoldApprovalServiceImpl(
            MilestoneOnHoldRequestRepository requestRepository,
            ProjectMilestoneAssignmentRepository assignmentRepository,
            MilestoneStatusRepository milestoneStatusRepository,
            MilestoneStatusHistoryRepository historyRepository,
            UserRepository userRepository
    ) {
        this.requestRepository = requestRepository;
        this.assignmentRepository = assignmentRepository;
        this.milestoneStatusRepository = milestoneStatusRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
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

        validateRequesterIsCurrentAssignee(assignment, requester);

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

        if (requestRepository.existsByMilestoneAssignment_IdAndApprovalStatus(
                assignment.getId(), MilestoneOnHoldApprovalStatus.PENDING)) {
            throw new ValidationException(
                    "An ON_HOLD approval request is already pending for this milestone",
                    "ON_HOLD_REQUEST_ALREADY_PENDING"
            );
        }

        if (requester.getManager() == null || requester.getManager().getId() == null) {
            throw new ValidationException(
                    "No manager is configured for the assigned user",
                    "REQUESTER_MANAGER_NOT_CONFIGURED"
            );
        }

        User manager = userRepository.findActiveUserById(requester.getManager().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assigned user's manager was not found or is inactive",
                        "MANAGER_NOT_FOUND"
                ));

        if (manager.getId().equals(requester.getId())) {
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

        // Keep the milestone's current status unchanged until the manager approves.
        return toResponse(saved);
    }

    @Override
    public MilestoneOnHoldResponseDto decide(
            Long requestId,
            MilestoneOnHoldDecisionDto dto
    ) {
        log.info("[MILESTONE-ON-HOLD-DECISION-START] requestId={}, managerId={}, decision={}",
                requestId, dto.getManagerId(), dto.getDecision());

        MilestoneOnHoldRequest request = requestRepository.findByIdForDecision(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ON_HOLD approval request not found",
                        "ON_HOLD_REQUEST_NOT_FOUND"
                ));

        if (request.getApprovalStatus() != MilestoneOnHoldApprovalStatus.PENDING) {
            throw new ValidationException(
                    "This ON_HOLD request has already been decided",
                    "ON_HOLD_REQUEST_ALREADY_DECIDED"
            );
        }

        User manager = userRepository.findActiveUserById(dto.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Manager not found",
                        "MANAGER_NOT_FOUND"
                ));

        if (!request.getApprover().getId().equals(manager.getId())) {
            log.warn("[MILESTONE-ON-HOLD-DECISION-DENIED] requestId={}, expectedManagerId={}, actualUserId={}",
                    requestId, request.getApprover().getId(), manager.getId());
            throw new ValidationException(
                    "Only the requester's manager can decide this request",
                    "NOT_AUTHORIZED_ON_HOLD_APPROVER"
            );
        }

        if (dto.getDecision() == MilestoneOnHoldDecision.REJECT
                && (dto.getDecisionReason() == null || dto.getDecisionReason().isBlank())) {
            throw new ValidationException(
                    "Decision reason is required when rejecting an ON_HOLD request",
                    "ON_HOLD_REJECTION_REASON_REQUIRED"
            );
        }

        if (dto.getDecision() == MilestoneOnHoldDecision.APPROVE) {
            approveRequest(request, manager, dto.getDecisionReason());
        } else {
            rejectRequest(request, dto.getDecisionReason());
        }

        request.setDecidedAt(LocalDateTime.now());
        MilestoneOnHoldRequest saved = requestRepository.save(request);

        log.info("[MILESTONE-ON-HOLD-DECISION-SUCCESS] requestId={}, assignmentId={}, " +
                        "managerId={}, approvalStatus={}",
                saved.getId(), saved.getMilestoneAssignment().getId(), manager.getId(),
                saved.getApprovalStatus());

        return toResponse(saved);
    }

    private void approveRequest(
            MilestoneOnHoldRequest request,
            User manager,
            String decisionReason
    ) {
        ProjectMilestoneAssignment assignment = request.getMilestoneAssignment();

        if (assignment.getAssignedUser() == null
                || !assignment.getAssignedUser().getId().equals(request.getRequestedBy().getId())) {
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
    }

    private void rejectRequest(
            MilestoneOnHoldRequest request,
            String decisionReason
    ) {
        // The milestone was never changed, so rejection only closes the request.
        request.setApprovalStatus(MilestoneOnHoldApprovalStatus.REJECTED);
        request.setDecisionReason(decisionReason.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MilestoneOnHoldResponseDto> getManagerQueue(
            Long managerId,
            MilestoneOnHoldApprovalStatus approvalStatus,
            Pageable pageable
    ) {
        userRepository.findActiveUserById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Manager not found",
                        "MANAGER_NOT_FOUND"
                ));

        return requestRepository.findManagerQueue(managerId, approvalStatus, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPendingRequest(Long assignmentId) {
        return requestRepository.existsByMilestoneAssignment_IdAndApprovalStatus(
                assignmentId,
                MilestoneOnHoldApprovalStatus.PENDING
        );
    }

    private void validateRequesterIsCurrentAssignee(
            ProjectMilestoneAssignment assignment,
            User requester
    ) {
        if (assignment.getAssignedUser() == null
                || !assignment.getAssignedUser().getId().equals(requester.getId())) {
            throw new ValidationException(
                    "Only the currently assigned user can request ON_HOLD",
                    "ONLY_ASSIGNEE_CAN_REQUEST_ON_HOLD"
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
