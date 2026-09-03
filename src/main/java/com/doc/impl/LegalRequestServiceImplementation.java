package com.doc.impl;

import com.doc.dto.LegalRequestDto.*;
import com.doc.em.LegalStatus;
import com.doc.em.ProjectHistoryEventType;
import com.doc.em.ProjectHistoryReferenceType;
import com.doc.entity.document.LegalRequestDocument;
import com.doc.entity.legalrequest.LegalRequest;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.project.ProjectHistoryEvent;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.*;
import com.doc.service.LegalRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LegalRequestServiceImplementation implements LegalRequestService {

    private static final Logger logger = LoggerFactory.getLogger(LegalRequestServiceImplementation.class);

    private final UserRepository userRepository;
    private final LegalRequestRepository legalRequestRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMilestoneAssignmentRepository milestoneRepo;

    private final LegalRequestDocumentRepository legalRequestDocumentRepository;
    private final ProjectHistoryEventRepository projectHistoryEventRepository;

    @Autowired
    public LegalRequestServiceImplementation(
            UserRepository userRepository,
            LegalRequestRepository legalRequestRepository,
            ProjectRepository projectRepository,
            ProjectMilestoneAssignmentRepository milestoneRepo,
            LegalRequestDocumentRepository legalRequestDocumentRepository,
            ProjectHistoryEventRepository projectHistoryEventRepository) {

        this.userRepository = userRepository;
        this.legalRequestRepository = legalRequestRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepo = milestoneRepo;
        this.legalRequestDocumentRepository = legalRequestDocumentRepository;
        this.projectHistoryEventRepository = projectHistoryEventRepository;
    }

    @Override
    @Transactional
    public LegalRequestResponseDto createRequest(LegalRequestDto dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        if (dto.getProjectId() == null) {
            throw new IllegalArgumentException("projectId is required");
        }

        if (dto.getProjectMilestoneAssignmentId() == null) {
            throw new IllegalArgumentException("projectMilestoneAssignmentId is required");
        }

        if (dto.getLegalRequestTitle() == null || dto.getLegalRequestTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("legalRequestTitle is required");
        }

        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "ERR_PROJECT_NOT_FOUND"
                ));

        ProjectMilestoneAssignment milestoneAssignment =
                milestoneRepo.findByIdAndProjectIdAndIsDeletedFalse(
                        dto.getProjectMilestoneAssignmentId(),
                        dto.getProjectId()
                ).orElseThrow(() -> new ResourceNotFoundException(
                        "Project milestone assignment not found",
                        "ERR_PROJECT_MILESTONE_ASSIGNMENT_NOT_FOUND"
                ));

        LegalRequest request = new LegalRequest();

        request.setProject(project);
        request.setProjectMilestoneAssignment(milestoneAssignment);
        request.setLegalRequestTitle(dto.getLegalRequestTitle().trim());
        request.setNotes(dto.getNotes());
        request.setStatusReason(dto.getStatusReason());

        if (dto.getAssignedToLegal() != null) {
            User assignedLegalUser = userRepository.findActiveUserById(dto.getAssignedToLegal())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Assigned legal user not found",
                            "ERR_USER_NOT_FOUND"
                    ));

            request.setAssignedToLegal(assignedLegalUser);
        }

        request.setLegalStatus(LegalStatus.INITIATED);

        Long currentUserId = dto.getCreatedById() != null
                ? dto.getCreatedById()
                : 1L;

        request.setCreatedBy(currentUserId);
        request.setUpdatedBy(currentUserId);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        /*
         * First save parent legal request.
         * After this, savedRequest.getId() will be available.
         */
        LegalRequest savedRequest = legalRequestRepository.save(request);

        // =========================================================
        // PROJECT HISTORY - LEGAL REQUEST CREATED
        // =========================================================
        saveProjectHistory(
                project,
                milestoneAssignment,
                "LEGAL_REQUEST_CREATED",
                savedRequest.getId(),
                "Legal request created",
                "Legal request '" + savedRequest.getLegalRequestTitle() + "' created",
                savedRequest.getStatusReason(),
                null,
                savedRequest.getLegalStatus() != null
                        ? savedRequest.getLegalStatus().name()
                        : null,
                currentUserId,
                null,
                savedRequest.getAssignedToLegal()
        );

        /*
         * Now save child documents using LegalRequestDocumentRepository.
         */
        if (dto.getLegalRequestDocumentDtoList() != null
                && !dto.getLegalRequestDocumentDtoList().isEmpty()) {

            List<LegalRequestDocument> documentsToSave = new ArrayList<>();

            for (LegalRequestDocumentDto docDto : dto.getLegalRequestDocumentDtoList()) {

                if (docDto == null) {
                    throw new IllegalArgumentException("Document object cannot be null");
                }

                if (docDto.getFileUrl() == null || docDto.getFileUrl().trim().isEmpty()) {
                    throw new IllegalArgumentException("Document fileUrl is required");
                }

                LegalRequestDocument document = new LegalRequestDocument();

                document.setLegalRequest(savedRequest);

                document.setFileName(
                        docDto.getFileName() != null
                                ? docDto.getFileName().trim()
                                : null
                );

                document.setFileUrl(docDto.getFileUrl().trim());

                document.setFileType(
                        docDto.getFileType() != null
                                ? docDto.getFileType().trim()
                                : null
                );

                document.setFileSize(docDto.getFileSize());

                document.setUuid(
                        docDto.getUuid() != null
                                ? docDto.getUuid().trim()
                                : null
                );

                document.setUploadedAt(
                        docDto.getUploadedAt() != null
                                ? docDto.getUploadedAt()
                                : LocalDateTime.now()
                );

                documentsToSave.add(document);
            }

            List<LegalRequestDocument> savedDocuments =
                    legalRequestDocumentRepository.saveAll(documentsToSave);

            return mapToResponse(savedRequest, savedDocuments);
        }

        return mapToResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public LegalRequestSummaryResponseDto getSummary(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        boolean isAdmin = hasAnyRole(user, "ADMIN", "ROLE_ADMIN");
        boolean isOperationHead = hasAnyRole(user, "OPERATION_HEAD", "ROLE_OPERATION_HEAD");

        boolean belongsToLegalDepartment =
                user.getDepartments() != null
                        && user.getDepartments().stream()
                        .filter(d -> d != null && !d.isDeleted())
                        .map(d -> d.getName())
                        .filter(name -> name != null)
                        .map(String::trim)
                        .anyMatch(name -> name.equalsIgnoreCase("LEGAL")
                                || name.equalsIgnoreCase("LEGAL DEPARTMENT"));

        if (!isAdmin && !isOperationHead && !belongsToLegalDepartment) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Only Legal department, Operation Head or Admin can view this summary"
            );
        }

        // Admin / Operation Head see the whole department; individual
        // legal team members only see requests assigned specifically to them.
        List<LegalRequestRepository.LegalStatusCountProjection> rows =
                (isAdmin || isOperationHead)
                        ? legalRequestRepository.countGroupedByStatus()
                        : legalRequestRepository.countGroupedByStatusForUser(userId);

        List<LegalRequestStatusCountDto> statusCounts = rows.stream()
                .map(r -> new LegalRequestStatusCountDto(r.getStatus(), r.getTotal()))
                .toList();

        long totalPending = statusCounts.stream()
                .filter(s -> "INITIATED".equalsIgnoreCase(s.getStatus()))
                .mapToLong(LegalRequestStatusCountDto::getCount)
                .findFirst()
                .orElse(0L);

        return new LegalRequestSummaryResponseDto(totalPending, statusCounts);
    }


    @Override
    @Transactional
    public LegalRequestResponseDto updateStatus(Long id, LegalStatusUpdateDto dto) {

        if (id == null) {
            throw new IllegalArgumentException("Legal request id is required");
        }

        if (dto.getStatus() == null || dto.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("status is required");
        }

        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Legal request not found",
                        "ERR_LEGAL_REQUEST_NOT_FOUND"
                ));

        if (request.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Legal request not found",
                    "ERR_LEGAL_REQUEST_NOT_FOUND"
            );
        }

        LegalStatus newStatus;

        try {
            newStatus = LegalStatus.valueOf(dto.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid legal status: " + dto.getStatus());
        }

        if ((newStatus == LegalStatus.NEED_MORE_INFO
                || newStatus == LegalStatus.DISAPPROVED
                || newStatus == LegalStatus.CANCELLED)
                && (dto.getStatusReason() == null || dto.getStatusReason().trim().isEmpty())) {

            throw new IllegalArgumentException("statusReason is required for status: " + newStatus);
        }

        LegalStatus previousStatus = request.getLegalStatus();

        request.setLegalStatus(newStatus);
        request.setStatusReason(dto.getStatusReason());
        request.setUpdatedAt(LocalDateTime.now());
        request.setUpdatedBy(dto.getUserId());

        if (dto.getResolutionSummary() != null
                && !dto.getResolutionSummary().trim().isEmpty()) {
            request.setResolutionSummary(dto.getResolutionSummary().trim());
        }

        if (newStatus == LegalStatus.APPROVED
                || newStatus == LegalStatus.DISAPPROVED
                || newStatus == LegalStatus.GUIDANCE_GIVEN
                || newStatus == LegalStatus.COMPLETED
                || newStatus == LegalStatus.CANCELLED) {

            request.setResolvedBy(dto.getUserId());
            request.setResolvedAt(LocalDateTime.now());
        }

        LegalRequest updated = legalRequestRepository.save(request);

        // =========================================================
        // PROJECT HISTORY - LEGAL STATUS CHANGED
        // =========================================================
        saveProjectHistory(
                updated.getProject(),
                updated.getProjectMilestoneAssignment(),
                "LEGAL_REQUEST_STATUS_CHANGED",
                updated.getId(),
                "Legal request status changed",
                "Legal request '" + updated.getLegalRequestTitle()
                        + "' status changed from "
                        + (previousStatus != null ? previousStatus.name() : "N/A")
                        + " to "
                        + newStatus.name(),
                dto.getStatusReason(),
                previousStatus != null ? previousStatus.name() : null,
                newStatus.name(),
                dto.getUserId(),
                null,
                updated.getAssignedToLegal()
        );

        return mapToResponse(updated);
    }

    @Override
    public LegalRequestResponseDto getById(Long id) {

        if (id == null) {
            throw new IllegalArgumentException("Legal request id is required");
        }

        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Legal request not found",
                        "ERR_LEGAL_REQUEST_NOT_FOUND"
                ));

        if (request.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Legal request not found",
                    "ERR_LEGAL_REQUEST_NOT_FOUND"
            );
        }

        return mapToResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LegalRequestResponseDto> getAllLegalRequests(
            Long userId,
            LegalStatus status,
            int page,
            int size) {

        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        LegalStatus requestedStatus =
                status != null ? status : LegalStatus.INITIATED;

        // API pagination remains 1-based.
        int pageIndex = page <= 0 ? 0 : page - 1;
        int pageSize = size <= 0 ? 10 : size;

        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        boolean isAdmin = hasAnyRole(
                user,
                "ADMIN",
                "ROLE_ADMIN"
        );

        boolean isOperationHead = hasAnyRole(
                user,
                "OPERATION_HEAD",
                "ROLE_OPERATION_HEAD"
        );

        boolean belongsToLegalDepartment =
                user.getDepartments() != null
                        && user.getDepartments()
                        .stream()
                        .filter(department -> department != null)
                        .filter(department -> !department.isDeleted())
                        .map(department -> department.getName())
                        .filter(name -> name != null)
                        .map(String::trim)
                        .anyMatch(name ->
                                name.equalsIgnoreCase("LEGAL")
                                        || name.equalsIgnoreCase("LEGAL DEPARTMENT")
                        );

        boolean canViewAllRequests =
                isAdmin
                        || isOperationHead
                        || belongsToLegalDepartment;

        Page<LegalRequest> legalRequests;

        if (canViewAllRequests) {
            legalRequests =
                    legalRequestRepository.findAllByStatusNative(
                            requestedStatus.name(),
                            pageable
                    );
        } else {
            legalRequests =
                    legalRequestRepository.findByUserRelatedAndStatusNative(
                            userId,
                            requestedStatus.name(),
                            pageable
                    );
        }

        return legalRequests.map(this::mapToResponse);
    }

    private boolean hasAnyRole(User user, String... allowedRoles) {

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }

        return user.getRoles()
                .stream()
                .filter(role -> role != null && !role.isDeleted())
                .map(role -> role.getName())
                .filter(roleName -> roleName != null)
                .map(String::trim)
                .anyMatch(roleName ->
                        java.util.Arrays.stream(allowedRoles)
                                .anyMatch(allowedRole ->
                                        allowedRole.equalsIgnoreCase(roleName)
                                )
                );
    }



    @Override
    @Transactional
    public LegalRequestResponseDto markAsViewed(Long id, Long userId) {

        if (id == null) {
            throw new IllegalArgumentException("Legal request id is required");
        }

        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Legal request not found",
                        "ERR_LEGAL_REQUEST_NOT_FOUND"
                ));

        if (request.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Legal request not found",
                    "ERR_LEGAL_REQUEST_NOT_FOUND"
            );
        }

        boolean firstView = request.getViewedAt() == null;

        request.setViewedBy(userId);
        request.setViewedAt(LocalDateTime.now());
        request.setUpdatedBy(userId);
        request.setUpdatedAt(LocalDateTime.now());

        LegalRequest saved = legalRequestRepository.save(request);

        // Save only the first view in the project timeline.
        if (firstView) {
            saveProjectHistory(
                    saved.getProject(),
                    saved.getProjectMilestoneAssignment(),
                    "LEGAL_REQUEST_VIEWED",
                    saved.getId(),
                    "Legal request viewed",
                    "Legal request '" + saved.getLegalRequestTitle() + "' viewed",
                    null,
                    "NOT_VIEWED",
                    "VIEWED",
                    userId,
                    null,
                    saved.getAssignedToLegal()
            );
        }

        return mapToResponse(saved);
    }


    /**
     * Saves a legal workflow event in the unified project history timeline.
     * Existing legal-request business logic is not changed by this method.
     */
    private void saveProjectHistory(
            Project project,
            ProjectMilestoneAssignment milestoneAssignment,
            String eventTypeName,
            Long referenceId,
            String eventTitle,
            String description,
            String reason,
            String previousValue,
            String newValue,
            Long performedByUserId,
            User previousAssignee,
            User newAssignee
    ) {

        if (project == null || project.getId() == null) {
            logger.warn(
                    "[PROJECT-HISTORY-SKIPPED] Legal request has no valid project"
            );
            return;
        }

        ProjectHistoryEventType eventType;

        try {
            eventType = ProjectHistoryEventType.valueOf(eventTypeName);
        } catch (IllegalArgumentException ex) {
            logger.warn(
                    "[PROJECT-HISTORY-SKIPPED] Unknown eventType={} | projectId={}",
                    eventTypeName,
                    project.getId()
            );
            return;
        }

        ProjectHistoryReferenceType referenceType;

        try {
            referenceType = ProjectHistoryReferenceType.valueOf("LEGAL_REQUEST");
        } catch (IllegalArgumentException ex) {
            logger.warn(
                    "[PROJECT-HISTORY-SKIPPED] ProjectHistoryReferenceType LEGAL_REQUEST is not configured | projectId={}",
                    project.getId()
            );
            return;
        }

        User performedByUser = null;

        if (performedByUserId != null) {
            performedByUser = userRepository
                    .findActiveUserById(performedByUserId)
                    .orElse(null);
        }

        ProjectHistoryEvent historyEvent = new ProjectHistoryEvent();

        historyEvent.setProject(project);
        historyEvent.setMilestoneAssignment(milestoneAssignment);

        if (milestoneAssignment != null
                && milestoneAssignment.getMilestone() != null) {
            historyEvent.setMilestoneName(
                    milestoneAssignment.getMilestone().getName()
            );
        }

        historyEvent.setEventType(eventType);
        historyEvent.setReferenceType(referenceType);
        historyEvent.setReferenceId(referenceId);

        historyEvent.setEventTitle(eventTitle);
        historyEvent.setDescription(description);
        historyEvent.setReason(reason);

        historyEvent.setPreviousValue(previousValue);
        historyEvent.setNewValue(newValue);

        historyEvent.setPerformedByUserId(performedByUserId);
        historyEvent.setPerformedByName(
                performedByUser != null
                        ? performedByUser.getFullName()
                        : performedByUserId != null
                        ? "User #" + performedByUserId
                        : "System"
        );

        if (previousAssignee != null) {
            historyEvent.setPreviousAssigneeId(previousAssignee.getId());
            historyEvent.setPreviousAssigneeName(previousAssignee.getFullName());
        }

        if (newAssignee != null) {
            historyEvent.setNewAssigneeId(newAssignee.getId());
            historyEvent.setNewAssigneeName(newAssignee.getFullName());
        }

        historyEvent.setOccurredAt(LocalDateTime.now());

        projectHistoryEventRepository.save(historyEvent);

        logger.info(
                "[PROJECT-HISTORY-SAVED] projectId={} | eventType={} | referenceType={} | referenceId={} | performedBy={}",
                project.getId(),
                eventType,
                referenceType,
                referenceId,
                performedByUserId
        );
    }

    private LegalRequestResponseDto mapToResponse(
            LegalRequest request,
            List<LegalRequestDocument> documents) {

        LegalRequestResponseDto dto = new LegalRequestResponseDto();

        dto.setId(request.getId());

        dto.setProjectId(
                request.getProject() != null
                        ? request.getProject().getId()
                        : null
        );

        if (request.getProjectMilestoneAssignment() != null) {
            dto.setProjectMilestoneAssignmentId(
                    request.getProjectMilestoneAssignment().getId()
            );

            if (request.getProjectMilestoneAssignment().getAssignedUser() != null) {
                dto.setMilestoneAssigneeId(
                        request.getProjectMilestoneAssignment().getAssignedUser().getId()
                );
            }
        }

        if (request.getAssignedToLegal() != null) {
            dto.setAssignedToLegal(request.getAssignedToLegal().getId());
        }

        dto.setLegalRequestTitle(request.getLegalRequestTitle());

        dto.setStatus(
                request.getLegalStatus() != null
                        ? request.getLegalStatus().name()
                        : null
        );

        dto.setNotes(request.getNotes());
        dto.setStatusReason(request.getStatusReason());
        dto.setResolutionSummary(request.getResolutionSummary());

        dto.setCreatedById(request.getCreatedBy());
        dto.setUpdatedById(request.getUpdatedBy());

        dto.setViewedBy(request.getViewedBy());
        dto.setViewedAt(request.getViewedAt());

        dto.setResolvedBy(request.getResolvedBy());
        dto.setResolvedAt(request.getResolvedAt());

        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());

        if (documents != null && !documents.isEmpty()) {
            dto.setDocuments(
                    documents.stream()
                            .map(document -> {
                                LegalRequestDocumentResponseDto docDto =
                                        new LegalRequestDocumentResponseDto();

                                docDto.setId(document.getId());
                                docDto.setFileName(document.getFileName());
                                docDto.setFileUrl(document.getFileUrl());
                                docDto.setFileType(document.getFileType());
                                docDto.setFileSize(document.getFileSize());
                                docDto.setUuid(document.getUuid());
                                docDto.setUploadedAt(document.getUploadedAt());

                                return docDto;
                            })
                            .toList()
            );
        }

        return dto;
    }



    private LegalRequestResponseDto mapToResponse(LegalRequest request) {

        List<LegalRequestDocument> documents = new ArrayList<>();

        if (request != null && request.getId() != null) {
            documents = legalRequestDocumentRepository.findByLegalRequestId(request.getId());
        }

        return mapToResponse(request, documents);
    }

}