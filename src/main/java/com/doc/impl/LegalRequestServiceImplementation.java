package com.doc.impl;

import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.dto.LegalRequestDto.LegalStatusUpdateDto;
import com.doc.em.LegalStatus;
import com.doc.entity.document.LegalRequestDocument;
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
    public LegalRequestDto createRequest(LegalRequestDto dto) {

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
            User user = userRepository.findActiveUserById(dto.getAssignedToLegal())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Assigned legal user not found",
                            "ERR_USER_NOT_FOUND"
                    ));

            request.setAssignedToLegal(user);
            request.setLegalStatus(LegalStatus.PENDING);
        } else {
            request.setLegalStatus(LegalStatus.INITIATED);
        }

        request.setCreatedBy(dto.getCreatedById());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {

            for (String fileUrl : dto.getDocuments()) {

                if (fileUrl == null || fileUrl.trim().isEmpty()) {
                    continue;
                }

                LegalRequestDocument document = new LegalRequestDocument();

                document.setFileUrl(fileUrl.trim());
                document.setUploadedAt(LocalDateTime.now());

                String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                document.setFileName(fileName);

                request.addDocument(document);
            }
        }

        LegalRequest savedRequest = legalRequestRepository.save(request);

        return mapToResponse(savedRequest);
    }

    @Override
    @Transactional
    public LegalRequestDto updateStatus(Long id, LegalStatusUpdateDto dto) {

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

        // Optional validation for statuses where reason should be compulsory
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

        if (dto.getResolutionSummary() != null && !dto.getResolutionSummary().trim().isEmpty()) {
            request.setResolutionSummary(dto.getResolutionSummary().trim());
        }

        // Mark resolved only for final/closing statuses
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
    public LegalRequestDto getById(Long id) {
        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legal request not found", "ERR_LEGAL_REQUEST_NOT_FOUND"));

        return mapToResponse(request);
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



    public LegalRequestDto mapToResponse(LegalRequest request) {

        LegalRequestDto dto = new LegalRequestDto();

        dto.setProjectId(request.getProject() != null ? request.getProject().getId() : null);

        dto.setStatus(request.getLegalStatus() != null ? request.getLegalStatus().name() : null);

        if (request.getAssignedToLegal() != null) {
            dto.setAssignedToLegal(request.getAssignedToLegal().getId());
        }

        dto.setLegalRequestTitle(request.getLegalRequestTitle());
        dto.setNotes(request.getNotes());


        return dto;
    }
}