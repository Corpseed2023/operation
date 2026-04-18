package com.doc.impl;

import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.dto.LegalRequestDto.LegalStatusUpdateDto;
import com.doc.em.LegalStatus;
import com.doc.entity.legalrequest.LegalRequest;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.LegalRequestDocumentRepository;
import com.doc.repository.LegalRequestRepository;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.service.LegalRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LegalRequestServiceImplementation implements LegalRequestService {

    private final UserRepository userRepository;
    private final LegalRequestRepository legalRequestRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMilestoneAssignmentRepository milestoneRepo;
    private final LegalRequestDocumentRepository documentRepository;

    @Autowired
    public LegalRequestServiceImplementation(
            UserRepository userRepository,
            LegalRequestRepository legalRequestRepository,
            ProjectRepository projectRepository,
            ProjectMilestoneAssignmentRepository milestoneRepo,
            LegalRequestDocumentRepository documentRepository) {

        this.userRepository = userRepository;
        this.legalRequestRepository = legalRequestRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepo = milestoneRepo;
        this.documentRepository = documentRepository;
    }

    @Override
    @Transactional
    public LegalRequestDto createRequest(LegalRequestDto dto) {

        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));

        ProjectMilestoneAssignment milestone = milestoneRepo
                .findByIdAndProjectIdAndIsDeletedFalse(dto.getMilestoneId(), dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found", "ERR_MILESTONE_NOT_FOUND"));

        LegalRequest request = new LegalRequest();
        request.setProject(project);
        request.setProjectMilestoneAssignment(milestone);

        if (dto.getAssignedToLegal() != null) {
            User user = userRepository.findById(dto.getAssignedToLegal())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned legal user not found", "ERR_USER_NOT_FOUND"));
            request.setAssignedToLegal(user);
        }

        request.setLegalRequestTitle(dto.getLegalRequestTitle());
        request.setNotes(dto.getNotes());
        request.setTatInDays(dto.getTatInDays() != null ? dto.getTatInDays() : 0.0);
        request.setTatReason(dto.getTatReason());

        request.setLegalStatus(LegalStatus.INITIATED);

        // TODO: Replace hardcoded user with Spring Security context later
        Long currentUserId = 1L; // Replace with actual logged-in user
        request.setCreatedBy(currentUserId);
        request.setUpdatedBy(currentUserId);

        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        LegalRequest savedRequest = legalRequestRepository.save(request);

        // If documents are sent in DTO, you can handle them here later

        return mapToResponse(savedRequest);
    }

    @Override
    @Transactional
    public LegalRequestDto updateStatus(Long id, LegalStatusUpdateDto dto) {

        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legal request not found", "ERR_LEGAL_REQUEST_NOT_FOUND"));

        LegalStatus newStatus = LegalStatus.valueOf(dto.getStatus().toUpperCase());

        request.setLegalStatus(newStatus);
        request.setStatusReason(dto.getStatusReason());
        request.setUpdatedAt(LocalDateTime.now());
        request.setUpdatedBy(1L); // Replace with current user

        LegalRequest updated = legalRequestRepository.save(request);
        return mapToResponse(updated);
    }

    @Override
    public Page<LegalRequestDto> getLegalRequests(Long userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        Page<LegalRequest> result;

        if (user.getUserDesignation() != null && "ADMIN".equalsIgnoreCase(user.getUserDesignation().getName())) {
            result = legalRequestRepository.findAll(pageable);
        } else {
            result = legalRequestRepository.findByAssignedTo(userId, pageable);
        }

        return result.map(this::mapToResponse);
    }

    @Override
    public Page<LegalRequestDto> searchRequests(
            LegalStatus status,
            Long projectId,
            Long assignedTo,
            Long createdBy,
            String projectName,
            String milestoneName,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<LegalRequest> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("legalStatus"), status));
        }

        if (projectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("project").get("id"), projectId));
        }

        if (assignedTo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignedTo"), assignedTo));
        }

        if (createdBy != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("createdBy"), createdBy));
        }

        if (startDate != null && endDate != null) {
            spec = spec.and((root, query, cb) -> cb.between(root.get("createdAt"), startDate, endDate));
        } else if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        } else if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }

        if (projectName != null && !projectName.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("project").get("name")), "%" + projectName.toLowerCase() + "%"));
        }

        if (milestoneName != null && !milestoneName.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("projectMilestoneAssignment")
                                    .get("milestone")
                                    .get("name")),
                            "%" + milestoneName.toLowerCase() + "%"));
        }

        Page<LegalRequest> result = legalRequestRepository.findAll(spec, pageable);
        return result.map(this::mapToResponse);
    }

    @Override
    public LegalRequestDto getById(Long id) {
        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legal request not found", "ERR_LEGAL_REQUEST_NOT_FOUND"));

        return mapToResponse(request);
    }

    @Override
    @Transactional
    public LegalRequestDto assignRequest(Long requestId, Long assignedToLegal, String note) {

        LegalRequest request = legalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Legal request not found", "ERR_LEGAL_REQUEST_NOT_FOUND"));

        User assignedUser = userRepository.findById(assignedToLegal)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        // Fixed logic
        request.setAssignedTo(assignedToLegal);   // Corrected
        request.setAssignedToLegal(assignedUser);

        if (note != null && !note.trim().isEmpty()) {
            request.setNotes(note);
        }

        request.setUpdatedAt(LocalDateTime.now());
        request.setUpdatedBy(1L); // Replace with current user

        LegalRequest saved = legalRequestRepository.save(request);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public LegalRequestDto markAsViewed(Long id, Long userId) {

        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found", "ERR_LEGAL_REQUEST_NOT_FOUND"));

        request.setViewedBy(userId);
        request.setViewedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        LegalRequest saved = legalRequestRepository.save(request);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public LegalRequestDto updateTat(Long id, LegalRequestDto dto) {

        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found", "ERR_LEGAL_REQUEST_NOT_FOUND"));

        request.setTatInDays(dto.getTatInDays());
        request.setTatReason(dto.getTatReason());
        request.setUpdatedAt(LocalDateTime.now());
        request.setUpdatedBy(1L); // Replace with current user

        LegalRequest saved = legalRequestRepository.save(request);
        return mapToResponse(saved);
    }

    public LegalRequestDto mapToResponse(LegalRequest request) {

        LegalRequestDto dto = new LegalRequestDto();

        dto.setId(request.getId());
        dto.setProjectId(request.getProject() != null ? request.getProject().getId() : null);
        dto.setMilestoneId(request.getProjectMilestoneAssignment() != null ?
                request.getProjectMilestoneAssignment().getId() : null);

        dto.setTatInDays(request.getTatInDays());
        dto.setTatReason(request.getTatReason());
        dto.setStatus(request.getLegalStatus() != null ? request.getLegalStatus().name() : null);

        dto.setCreatedById(request.getCreatedBy());
        dto.setUpdatedById(request.getUpdatedBy());
        dto.setMilestoneAssigneeId(request.getAssignedTo());

        if (request.getAssignedToLegal() != null) {
            dto.setAssignedToLegal(request.getAssignedToLegal().getId());
        }

        dto.setLegalRequestTitle(request.getLegalRequestTitle());
        dto.setNotes(request.getNotes());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        dto.setViewedBy(request.getViewedBy());
        dto.setViewedAt(request.getViewedAt());

        return dto;
    }
}