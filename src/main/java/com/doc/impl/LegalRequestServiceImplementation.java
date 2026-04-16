package com.doc.impl;
import com.doc.dto.LegalRequestDto.LegalStatusUpdateDto;
import com.doc.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.entity.LegalRequest.LegalRequest;
import com.doc.em.LegalStatus;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.repository.*;
import com.doc.service.LegalRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class LegalRequestServiceImplementation implements LegalRequestService {

    @Autowired
    private  UserRepository userRepository;

    @Autowired
    private LegalRequestRepository legalRequestRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMilestoneAssignmentRepository milestoneRepo;

    @Autowired
    private LegalRequestDocumentRepository documentRepository;


    @Override
    public LegalRequestDto createRequest(LegalRequestDto dto) {

        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectMilestoneAssignment milestone = milestoneRepo
                .findByIdAndProjectIdAndIsDeletedFalse(dto.getMilestoneId(), dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        LegalRequest request = new LegalRequest();
        request.setProject(project);
        request.setProjectMilestoneAssignment(milestone);
//        request.setTatInDays(dto.getTatInDays());
        request.setAssignedTo(dto.getMilestoneAssigneeId());

        if (dto.getAssignedToLegal() != null) {
            User user = userRepository.findById(dto.getAssignedToLegal())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            request.setAssignedToLegal(user);
        }
        request.setLegalRequestTitle(dto.getLegalRequestTitle());
        request.setNotes(dto.getNotes());

        request.setLegalStatus(LegalStatus.INITIATED);

        Long currentUserId = 1L;
        request.setCreatedBy(currentUserId);
        request.setUpdatedBy(currentUserId);

        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        LegalRequest saved = legalRequestRepository.save(request);

        return mapToResponse(saved);
    }

    public LegalRequestDto mapToResponse(LegalRequest request) {

        LegalRequestDto dto = new LegalRequestDto();

        dto.setId(request.getId());
        dto.setProjectId(request.getProject().getId());
        dto.setMilestoneId(request.getProjectMilestoneAssignment().getId());
        dto.setTatInDays(request.getTatInDays());
        dto.setTatReason(request.getTatReason());
        dto.setStatus(request.getLegalStatus().name());

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
    public LegalRequestDto updateStatus(Long id, LegalStatusUpdateDto dto) {

        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        LegalStatus status = LegalStatus.valueOf(dto.getStatus());

        request.setLegalStatus(status);
        request.setStatusReason(dto.getStatusReason());
        request.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(legalRequestRepository.save(request));
    }
    @Override
    public Page<LegalRequestDto> getLegalRequests(Long userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<LegalRequest> result;

        if (user.getUserDesignation() != null &&
                "ADMIN".equalsIgnoreCase(user.getUserDesignation().getName())) {

            result = legalRequestRepository.findAll(pageable);
        }
        else {
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
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<LegalRequest> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("legalStatus"), status));
        }

        if (projectId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("project").get("id"), projectId));
        }

        if (assignedTo != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("assignedTo"), assignedTo));
        }


        if (createdBy != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("createdBy"), createdBy));
        }

        if (startDate != null && endDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.between(root.get("createdAt"), startDate, endDate));

        } else if (startDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));

        } else if (endDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }

        if (projectName != null && !projectName.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("project").get("name")),
                            "%" + projectName.toLowerCase() + "%"
                    ));
        }

        if (milestoneName != null && !milestoneName.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("projectMilestoneAssignment")
                                    .get("milestone")
                                    .get("name")),
                            "%" + milestoneName.toLowerCase() + "%"
                    ));
        }

        Page<LegalRequest> result =
                legalRequestRepository.findAll(spec, pageable);

        return result.map(this::mapToResponse);
    }

    @Override
    public LegalRequestDto getById(Long id) {

        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Legal request not found"));

        return mapToResponse(request);
    }

    @Override
    public LegalRequestDto assignRequest(Long requestId, Long assignedToLegal, String note) {

        LegalRequest request = legalRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Legal request not found"));

        User assignedUser = userRepository.findById(assignedToLegal)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long previousAssignee = request.getAssignedTo() != null
                ? request.getId(): null;

        request.setAssignedTo(request.getAssignedTo());

        return mapToResponse(request);
    }
    public LegalRequestDto markAsViewed(Long id, Long userId) {

        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        request.setViewedBy(userId);
        request.setViewedAt(LocalDateTime.now());

        return mapToResponse(legalRequestRepository.save(request));
    }

    public LegalRequestDto updateTat(Long id, LegalRequestDto dto) {

        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        request.setTatInDays(dto.getTatInDays());
        request.setStatusReason(dto.getTatReason());

        request.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(legalRequestRepository.save(request));
    }
}