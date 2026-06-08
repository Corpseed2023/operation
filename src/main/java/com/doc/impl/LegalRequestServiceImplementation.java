            package com.doc.impl;

            import com.doc.dto.LegalRequestDto.LegalRequestDocumentDto;
            import com.doc.dto.LegalRequestDto.LegalRequestDocumentResponseDto;
            import com.doc.dto.LegalRequestDto.LegalRequestDto;
            import com.doc.dto.LegalRequestDto.LegalRequestResponseDto;
            import com.doc.dto.LegalRequestDto.LegalStatusUpdateDto;
            import com.doc.em.LegalStatus;
            import com.doc.entity.document.LegalRequestDocument;
            import com.doc.entity.legalrequest.LegalRequest;
            import com.doc.entity.project.Project;
            import com.doc.entity.project.ProjectMilestoneAssignment;
            import com.doc.entity.user.User;
            import com.doc.exception.ResourceNotFoundException;
            import com.doc.repository.*;
            import com.doc.service.LegalRequestService;
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

                private final UserRepository userRepository;
                private final LegalRequestRepository legalRequestRepository;
                private final ProjectRepository projectRepository;
                private final ProjectMilestoneAssignmentRepository milestoneRepo;

                private final LegalRequestDocumentRepository legalRequestDocumentRepository;

                @Autowired
                public LegalRequestServiceImplementation(
                        UserRepository userRepository,
                        LegalRequestRepository legalRequestRepository,
                        ProjectRepository projectRepository,
                        ProjectMilestoneAssignmentRepository milestoneRepo,
                        LegalRequestDocumentRepository legalRequestDocumentRepository) {

                    this.userRepository = userRepository;
                    this.legalRequestRepository = legalRequestRepository;
                    this.projectRepository = projectRepository;
                    this.milestoneRepo = milestoneRepo;
                    this.legalRequestDocumentRepository = legalRequestDocumentRepository;
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
                public Page<LegalRequestResponseDto> getAllLegalRequests(
                        Long userId,
                        LegalStatus status,
                        int page,
                        int size) {

                    if (userId == null) {
                        throw new IllegalArgumentException("userId is required");
                    }

                    if (status == null) {
                        status = LegalStatus.INITIATED;
                    }

                    int pageIndex = page <= 0 ? 0 : page - 1;
                    int pageSize = size <= 0 ? 10 : size;

                    Pageable pageable = PageRequest.of(pageIndex, pageSize);

                    User user = userRepository.findActiveUserById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "User not found",
                                    "ERR_USER_NOT_FOUND"
                            ));

                    String designationName = user.getUserDesignation() != null
                            ? user.getUserDesignation().getName()
                            : null;

                    boolean isAdmin = designationName != null
                            && "ADMIN".equalsIgnoreCase(designationName.trim());

                    Page<LegalRequest> result;

                    if (isAdmin) {
                        result = legalRequestRepository.findAllByStatusNative(
                                status.name(),
                                pageable
                        );
                    } else {
                        result = legalRequestRepository.findByUserRelatedAndStatusNative(
                                userId,
                                status.name(),
                                pageable
                        );
                    }

                    return result.map(this::mapToResponse);
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

                    request.setViewedBy(userId);
                    request.setViewedAt(LocalDateTime.now());
                    request.setUpdatedBy(userId);
                    request.setUpdatedAt(LocalDateTime.now());

                    LegalRequest saved = legalRequestRepository.save(request);

                    return mapToResponse(saved);
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