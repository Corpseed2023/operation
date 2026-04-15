package com.doc.impl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.doc.config.S3Service;
import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.entity.LegalRequest.LegalRequest;
import com.doc.em.LegalStatus;
import com.doc.entity.document.LegalRequestDocument;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.repository.*;
import com.doc.service.LegalRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LegalRequestServiceImplementation implements LegalRequestService {

    @Autowired
    private LegalRequestRepository legalRequestRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMilestoneAssignmentRepository milestoneRepo;

    @Autowired
    private LegalRequestDocumentRepository documentRepository;


    private final S3Service s3Service;

    public LegalRequestServiceImplementation(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @Override
    public LegalRequestDto createRequest(Long projectId,
                                         Long milestoneId,
                                         double tatInDays,
                                         MultipartFile[] files) throws IOException {


        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));


        ProjectMilestoneAssignment milestone = milestoneRepo
                .findByIdAndProjectIdAndIsDeletedFalse(milestoneId, projectId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));


        if (milestone.getAssignedUser() == null) {
            throw new RuntimeException("Milestone is not assigned to any user");
        }


        LegalRequest request = new LegalRequest();
        request.setProject(project);
        request.setProjectMilestoneAssignment(milestone);
        request.setTatInDays(tatInDays);
        request.setLegalStatus(LegalStatus.INITIATED);


        Long currentUserId = 1L;
        request.setCreatedBy(currentUserId);
        request.setUpdatedBy(currentUserId);
        request.setAssignedTo(milestone.getAssignedUser().getId());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());


        LegalRequest savedRequest = legalRequestRepository.save(request);


        if (files != null) {
            for (MultipartFile file : files) {

                if (file.isEmpty()) continue;

                String uuid = UUID.randomUUID().toString();


                String s3Key = s3Service.uploadFile(file);

                LegalRequestDocument doc = new LegalRequestDocument();
                doc.setFileName(file.getOriginalFilename());
                doc.setFileUrl(s3Service.getFullUrl(s3Key)); // FULL URL now from S3
                doc.setFileType(file.getContentType());
                doc.setFileSize(file.getSize());
                doc.setUuid(uuid);
                doc.setUploadedAt(LocalDateTime.now());
                doc.setLegalRequest(savedRequest);

                documentRepository.save(doc);
            }
        }


        return mapToResponse(savedRequest);
    }

    @Override
    public LegalRequestDto mapToResponse(LegalRequest request) {

        LegalRequestDto res = new LegalRequestDto();

        res.setId(request.getId());
        res.setProjectName(request.getProject().getName());
        res.setTatInDays(request.getTatInDays());
        res.setStatus(request.getLegalStatus().name());
        res.setStatusReason(request.getStatusReason());


        res.setMilestoneName(
                request.getProjectMilestoneAssignment()
                        .getMilestone()
                        .getName()
        );


        res.setAssignedToName(
                request.getProjectMilestoneAssignment()
                        .getAssignedUser()
                        .getFullName()
        );


        res.setCreatedByName("User-" + request.getCreatedBy());
        res.setUpdatedByName("User-" + request.getUpdatedBy());

        res.setCreatedAt(request.getCreatedAt());
        res.setUpdatedAt(request.getUpdatedAt());


        List<LegalRequestDocument> docList =
                documentRepository.findByLegalRequestId(request.getId());

        List<String> docs = docList.stream()
                .map(LegalRequestDocument::getFileUrl)
                .collect(Collectors.toList());

        res.setDocuments(docs);

        return res;
    }

    @Override
    public LegalRequestDto updateStatus(Long id, LegalStatus status, String reason) {

        LegalRequest request = legalRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Legal request not found"));

        request.setLegalStatus(status);
        request.setUpdatedAt(LocalDateTime.now());
        if (status == LegalStatus.APPROVED || status == LegalStatus.DISAPPROVED) {
            if (reason == null || reason.trim().isEmpty()) {
                throw new RuntimeException("Reason is required for this status");
            }
            request.setStatusReason(reason);
        }
        legalRequestRepository.save(request);

        return mapToResponse(request);
    }

    public List<LegalRequestDto> getAllRequests() {

        List<LegalRequest> list = legalRequestRepository.findAll();

        return list.stream().map(this::mapToResponse).toList();
    }
    @Override
    public Page<LegalRequestDto> getLegalRequests(Long userId, String role, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<LegalRequest> result;

        if ("ADMIN".equalsIgnoreCase(role)) {
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
}