package com.doc.impl;

import com.doc.dto.project.DocumentResponseDto;
import com.doc.dto.project.ProjectDocumentStatusUpdateDto;
import com.doc.dto.project.ProjectDocumentUploadRequestDto;
import com.doc.em.DocumentExpiryType;
import com.doc.entity.client.Company;
import com.doc.entity.document.CompanyDocument;
import com.doc.entity.document.DocumentStatus;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.milestone.MilestoneStatus;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.*;
import com.doc.repository.documentRepo.*;
import com.doc.service.ProjectDocumentUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProjectDocumentUploadServiceImpl implements ProjectDocumentUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectDocumentUploadServiceImpl.class);

    private final ProjectDocumentUploadRepository projectDocumentUploadRepository;
    private final ProjectRepository projectRepository;
    private final ProductRequiredDocumentsRepository productRequiredDocumentsRepository;
    private final UserRepository userRepository;
    private final DocumentStatusRepository documentStatusRepository;
    @Autowired
    private CompanyDocumentRepository companyDocumentRepository;
    @Autowired
    private ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;

    @Autowired
    private ProjectMilestoneAssignmentRepository milestoneAssignmentRepository;

    @Autowired
    private MilestoneStatusRepository milestoneStatusRepository;


    public ProjectDocumentUploadServiceImpl(
            ProjectDocumentUploadRepository projectDocumentUploadRepository,
            ProjectRepository projectRepository,
            ProductRequiredDocumentsRepository productRequiredDocumentsRepository,
            UserRepository userRepository,
            DocumentStatusRepository documentStatusRepository) {

        this.projectDocumentUploadRepository = projectDocumentUploadRepository;
        this.projectRepository = projectRepository;
        this.productRequiredDocumentsRepository = productRequiredDocumentsRepository;
        this.userRepository = userRepository;
        this.documentStatusRepository = documentStatusRepository;
    }

    @Override
    public DocumentResponseDto uploadDocument(ProjectDocumentUploadRequestDto requestDto) {

        validateUploadRequest(requestDto);

        String fileUrl = requestDto.getFileName(); // full S3 URL

        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new ValidationException("File URL cannot be empty", "INVALID_FILE_URL");
        }

        // ✅ Extract filename from URL
        String extractedFileName;
        try {
            extractedFileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        } catch (Exception e) {
            throw new ValidationException("Invalid file URL format", "INVALID_FILE_URL_FORMAT");
        }

        String fileName = sanitizeFileName(extractedFileName);

        Project project = projectRepository.findActiveUserById(requestDto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "PROJECT_NOT_FOUND"));

        validateProjectMilestoneVisibility(project.getId());

        ProductRequiredDocuments requiredDoc = productRequiredDocumentsRepository
                .findById(requestDto.getRequiredDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Required document not found", "DOCUMENT_NOT_FOUND"));

        User uploadedBy = userRepository.findActiveUserById(requestDto.getUploadedById())
                .orElseThrow(() -> new ResourceNotFoundException("Uploader not found", "USER_NOT_FOUND"));

        DocumentStatus uploadedStatus = documentStatusRepository.findByName("UPLOADED")
                .orElseThrow(() -> new ResourceNotFoundException("Status UPLOADED not found", "STATUS_NOT_FOUND"));

        var existingOpt = projectDocumentUploadRepository
                .findActiveProjectLevelDocument(requestDto.getProjectId(), requestDto.getRequiredDocumentId());

        ProjectDocumentUpload doc;
        boolean isReplacement = existingOpt.isPresent();

        if (isReplacement) {
            doc = existingOpt.get();

            if ("VERIFIED".equals(doc.getStatus().getName())) {
                throw new ValidationException("Cannot replace VERIFIED document", "VERIFIED_DOCUMENT_REPLACEMENT");
            }

            doc.setOldFileUrl(doc.getFileUrl());
            doc.setOldFileName(doc.getFileName());
            doc.setReplacementCount(doc.getReplacementCount() + 1);
        } else {
            doc = new ProjectDocumentUpload();
            doc.setProject(project);
            doc.setRequiredDocument(requiredDoc);
            doc.setCreatedBy(requestDto.getCreatedById());
            doc.setCreatedDate(new Date());
            doc.setReplacementCount(0);
        }

        doc.setFileUrl(fileUrl);
        doc.setFileName(fileName);
        doc.setFileFormat(requestDto.getFileFormat() != null ?
                requestDto.getFileFormat().toLowerCase() : null);
        doc.setFileSizeKb(requestDto.getFileSizeKb());
        doc.setExpiryDate(requestDto.getExpiryDate() != null ?
                java.sql.Date.valueOf(requestDto.getExpiryDate()) : null);
        doc.setPermanent(Boolean.TRUE.equals(requestDto.getIsPermanent()));
        doc.setFromCompanyDoc(Boolean.TRUE.equals(requestDto.getIsFromCompanyDoc()));
        doc.setCompanyDocSourceId(requestDto.getCompanyDocSourceId());
        doc.setRemarks(requestDto.getRemarks());

        doc.setStatus(uploadedStatus);
        doc.setUploadedBy(uploadedBy);
        doc.setUploadTime(new Date());
        doc.setUpdatedBy(requestDto.getCreatedById());
        doc.setUpdatedDate(new Date());
        doc.setDeleted(false);

        doc = projectDocumentUploadRepository.save(doc);

        // Auto-create reusable company document for FIXED expiry type
        handleFixedCompanyDocument(doc);


        return mapToDocumentResponseDto(doc);
    }
    private void validateProjectMilestoneVisibility(Long projectId) {

        boolean hasVisibleMilestone = projectMilestoneAssignmentRepository
                .existsVisibleMilestoneByProjectId(projectId);

        if (!hasVisibleMilestone) {
            throw new ValidationException(
                    "Document upload is not allowed. No visible milestone found for this project.",
                    "MILESTONE_NOT_VISIBLE"
            );
        }
    }

    @Override
    public DocumentResponseDto updateDocumentStatus(Long documentId, ProjectDocumentStatusUpdateDto updateDto) {

        ProjectDocumentUpload documentUpload = projectDocumentUploadRepository
                .findActiveUserById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found", "DOCUMENT_UPLOAD_NOT_FOUND"));

        DocumentStatus newStatus = documentStatusRepository.findByName(updateDto.getNewStatus())
                .orElseThrow(() -> new ResourceNotFoundException("Status not found", "STATUS_NOT_FOUND"));

        validateDocumentStatusTransition(documentUpload.getStatus(), newStatus);

        documentUpload.setStatus(newStatus);
        documentUpload.setRemarks(updateDto.getRemarks());
        documentUpload.setUpdatedBy(updateDto.getChangedById());
        documentUpload.setUpdatedDate(new Date());

        documentUpload = projectDocumentUploadRepository.save(documentUpload);

        // ✅ If document rejected → move milestone to REWORK
        if ("REJECTED".equalsIgnoreCase(newStatus.getName())) {

            Project project = documentUpload.getProject();

            ProjectMilestoneAssignment documentationMilestone =
                    milestoneAssignmentRepository
                            .findByProjectIdAndMilestoneName(project.getId(), "Documentation")
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Documentation milestone not found for project",
                                    "MILESTONE_NOT_FOUND"));

            MilestoneStatus reworkStatus = milestoneStatusRepository.findByName("REWORK")
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Milestone status REWORK not found",
                            "MILESTONE_STATUS_NOT_FOUND"));

            // Update only Documentation milestone
            documentationMilestone.setStatus(reworkStatus);
            documentationMilestone.setStatusReason("Document rejected. Requires correction.");
            documentationMilestone.setCompletedDate(null);
            documentationMilestone.setStartedDate(new Date());
            documentationMilestone.setUpdatedDate(new Date());
            documentationMilestone.setReworkAttempts(documentationMilestone.getReworkAttempts() + 1);

            milestoneAssignmentRepository.save(documentationMilestone);
            System.out.println("Documentation is opened again");
        }


        return mapToDocumentResponseDto(documentUpload);
    }

    private void validateUploadRequest(ProjectDocumentUploadRequestDto requestDto) {

        if (requestDto.getProjectId() == null)
            throw new ValidationException("Project ID required", "INVALID_PROJECT_ID");

        if (requestDto.getRequiredDocumentId() == null)
            throw new ValidationException("Required document ID required", "INVALID_REQUIRED_DOCUMENT_ID");

        if (requestDto.getUploadedById() == null)
            throw new ValidationException("UploadedBy ID required", "INVALID_UPLOADED_BY");
    }

    private void validateDocumentStatusTransition(DocumentStatus currentStatus, DocumentStatus newStatus) {

        if (currentStatus.getName().equals(newStatus.getName())) {
            throw new ValidationException("Already in same status", "INVALID_STATUS_TRANSITION");
        }
    }

    private String sanitizeFileName(String fileName) {

        String sanitized = fileName.trim().replaceAll("[^a-zA-Z0-9\\.\\-_]", "");

        if (sanitized.length() > 255)
            throw new ValidationException("File name too long", "INVALID_FILE_NAME_LENGTH");

        if (sanitized.isEmpty())
            throw new ValidationException("Invalid file name", "INVALID_FILE_NAME_FORMAT");

        return sanitized;
    }

    private DocumentResponseDto mapToDocumentResponseDto(ProjectDocumentUpload doc) {

        DocumentResponseDto dto = new DocumentResponseDto();

        dto.setId(doc.getId());
        dto.setFileUrl(doc.getFileUrl());
        dto.setFileName(doc.getFileName());
        dto.setOldFileUrl(doc.getOldFileUrl());
        dto.setOldFileName(doc.getOldFileName());

        dto.setStatus(doc.getStatus() != null ? doc.getStatus().getName() : null);

        dto.setRemarks(doc.getRemarks());
        dto.setUploadTime(doc.getUploadTime());
        dto.setExpiryDate(doc.getExpiryDate());
        dto.setPermanent(doc.isPermanent());
        dto.setExpired(doc.isExpired());

        dto.setFileSizeKb(doc.getFileSizeKb());
        dto.setFileFormat(doc.getFileFormat());
        dto.setValidationPassed(doc.isValidationPassed());
        dto.setValidationIssues(doc.getValidationIssues());

        dto.setRequiredDocumentId(doc.getRequiredDocument() != null ?
                doc.getRequiredDocument().getId() : null);

        dto.setProjectId(doc.getProject() != null ?
                doc.getProject().getId() : null);

        dto.setUploadedById(doc.getUploadedBy() != null ?
                doc.getUploadedBy().getId() : null);

        dto.setCreatedBy(doc.getCreatedBy());
        dto.setUpdatedBy(doc.getUpdatedBy());
        dto.setCreatedDate(doc.getCreatedDate());
        dto.setUpdatedDate(doc.getUpdatedDate());

        dto.setReplacementCount(doc.getReplacementCount());
        dto.setFromCompanyDoc(doc.isFromCompanyDoc());
        dto.setCompanyDocSourceId(doc.getCompanyDocSourceId());

        return dto;
    }

    private void handleFixedCompanyDocument(ProjectDocumentUpload projectDoc) {

        ProductRequiredDocuments requiredDoc = projectDoc.getRequiredDocument();

        if (requiredDoc == null) return;

        // Only for FIXED expiry documents
        if (requiredDoc.getExpiryType() != DocumentExpiryType.FIXED) {
            return;
        }

        Company company = projectDoc.getProject().getCompany();

        if (company == null) {
            return;
        }

        Optional<CompanyDocument> existingOpt =
                companyDocumentRepository.findByCompanyIdAndRequiredDocumentIdAndIsDeletedFalse(
                        company.getId(), projectDoc.getRequiredDocument().getId()
                );

        CompanyDocument companyDoc;

        if (existingOpt.isPresent()) {

            companyDoc = existingOpt.get();

            // Prevent replacing VERIFIED reusable doc
            if ("VERIFIED".equals(companyDoc.getStatus().getName())) {
                return;
            }

            companyDoc.setOldFileUrl(companyDoc.getFileUrl());
            companyDoc.setOldFileName(companyDoc.getFileName());
            companyDoc.setReplacementCount(companyDoc.getReplacementCount() + 1);

        } else {

            companyDoc = new CompanyDocument();
            companyDoc.setCompany(company);
            companyDoc.setRequiredDocument(requiredDoc);
            companyDoc.setCreatedBy(projectDoc.getCreatedBy());
            companyDoc.setReplacementCount(0);
        }

        companyDoc.setFileUrl(projectDoc.getFileUrl());
        companyDoc.setFileName(projectDoc.getFileName());
        companyDoc.setFileFormat(projectDoc.getFileFormat());
        companyDoc.setFileSizeKb(projectDoc.getFileSizeKb());
        companyDoc.setRemarks(projectDoc.getRemarks());
        companyDoc.setUploadedBy(projectDoc.getUploadedBy());
        companyDoc.setStatus(projectDoc.getStatus());
        companyDoc.setUpdatedBy(projectDoc.getUpdatedBy());

        // FIXED → No expiry
        companyDoc.setExpiryDate(null);
        companyDoc.setPermanent(true);

        companyDocumentRepository.save(companyDoc);
    }

}
