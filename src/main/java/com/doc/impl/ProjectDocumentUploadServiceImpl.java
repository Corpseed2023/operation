package com.doc.impl;

import com.doc.dto.project.DocumentResponseDto;
import com.doc.dto.project.ProjectDocumentStatusUpdateDto;
import com.doc.dto.project.ProjectDocumentUploadRequestDto;
import com.doc.entity.client.Company;
import com.doc.entity.client.CompanyUnit;
import com.doc.entity.document.CompanyDocument;
import com.doc.entity.document.DocumentStatus;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.project.Project;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.*;
import com.doc.repository.documentRepo.*;
import com.doc.service.ProjectDocumentUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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
    private final CompanyDocumentRepository companyDocumentRepository;

    public ProjectDocumentUploadServiceImpl(
            ProjectDocumentUploadRepository projectDocumentUploadRepository,
            ProjectRepository projectRepository,
            ProductRequiredDocumentsRepository productRequiredDocumentsRepository,
            UserRepository userRepository,
            DocumentStatusRepository documentStatusRepository, CompanyDocumentRepository companyDocumentRepository) {

        this.projectDocumentUploadRepository = projectDocumentUploadRepository;
        this.projectRepository = projectRepository;
        this.productRequiredDocumentsRepository = productRequiredDocumentsRepository;
        this.userRepository = userRepository;
        this.documentStatusRepository = documentStatusRepository;
        this.companyDocumentRepository = companyDocumentRepository;
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

        return mapToDocumentResponseDto(doc);
    }

    @Override
    @Transactional
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
        if ("VERIFIED".equalsIgnoreCase(newStatus.getName())) {

            Project project = documentUpload.getProject();

            if (project == null) {
                throw new RuntimeException("Project not found for document");
            }

            Company company = project.getCompany();
            CompanyUnit unit = project.getUnit();

            if (company == null || unit == null) {
                throw new RuntimeException("Company or Unit missing for project");
            }

            ProductRequiredDocuments requiredDoc = documentUpload.getRequiredDocument();

            // ===============================
            // CHECK EXISTING COMPANY DOCUMENT
            // ===============================
            Optional<CompanyDocument> existingOpt =
                    companyDocumentRepository
                            .findByCompanyIdAndRequiredDocumentIdAndIsDeletedFalse(
                                    company.getId(),
                                    requiredDoc.getId()
                            );

            CompanyDocument companyDoc;

            if (existingOpt.isPresent()) {

                // ===============================
                // UPDATE (REPLACEMENT FLOW)
                // ===============================
                companyDoc = existingOpt.get();

                companyDoc.setOldFileUrl(companyDoc.getFileUrl());
                companyDoc.setOldFileName(companyDoc.getFileName());

                companyDoc.setFileUrl(documentUpload.getFileUrl());
                companyDoc.setFileName(documentUpload.getFileName());

                companyDoc.setReplacementCount(companyDoc.getReplacementCount() + 1);

            } else {

                // ===============================
                // CREATE NEW
                // ===============================
                companyDoc = new CompanyDocument();

                companyDoc.setCompany(company);
                companyDoc.setCompanyUnit(unit);
                companyDoc.setRequiredDocument(requiredDoc);

                companyDoc.setFileUrl(documentUpload.getFileUrl());
                companyDoc.setFileName(documentUpload.getFileName());

                companyDoc.setReplacementCount(0);
            }

            // ===============================
            // COMMON FIELDS
            // ===============================
            companyDoc.setStatus(newStatus);
            companyDoc.setRemarks(documentUpload.getRemarks());

            companyDoc.setUploadedBy(documentUpload.getUploadedBy());
            companyDoc.setCreatedBy(documentUpload.getCreatedBy());
            companyDoc.setUpdatedBy(documentUpload.getUpdatedBy());

            companyDoc.setFileSizeKb(documentUpload.getFileSizeKb());
            companyDoc.setFileFormat(documentUpload.getFileFormat());

            companyDoc.setValidationPassed(documentUpload.isValidationPassed());
            companyDoc.setValidationIssues(documentUpload.getValidationIssues());

            // VERIFIED INFO
            companyDoc.setVerifiedBy(documentUpload.getUploadedBy());
            companyDoc.setVerifiedDate(new Date());

            companyDocumentRepository.save(companyDoc);
        }


        return mapToDocumentResponseDto(documentUpload);
    }

    private void    validateUploadRequest(ProjectDocumentUploadRequestDto requestDto) {

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



}
