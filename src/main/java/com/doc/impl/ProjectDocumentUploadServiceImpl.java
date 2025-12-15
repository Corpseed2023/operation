package com.doc.impl;

import com.doc.dto.project.DocumentResponseDto;
import com.doc.dto.project.ProjectDocumentStatusUpdateDto;
import com.doc.dto.project.ProjectDocumentUploadRequestDto;
import com.doc.entity.document.DocumentStatus;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.project.Project;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.documentRepo.DocumentStatusRepository;
import com.doc.repository.documentRepo.ProductRequiredDocumentsRepository;
import com.doc.repository.documentRepo.ProjectDocumentUploadRepository;
import com.doc.service.ProjectDocumentUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@Transactional
public class ProjectDocumentUploadServiceImpl implements ProjectDocumentUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectDocumentUploadServiceImpl.class);

    private final ProjectDocumentUploadRepository projectDocumentUploadRepository;
    private final ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;
    private final ProjectRepository projectRepository;
    private final ProductRequiredDocumentsRepository productRequiredDocumentsRepository;
    private final UserRepository userRepository;
    private final DocumentStatusRepository documentStatusRepository;

    @Value("${aws.s3.bucket.url}")
    private String bucketUrl;

    @Autowired
    public ProjectDocumentUploadServiceImpl(
            ProjectDocumentUploadRepository projectDocumentUploadRepository,
            ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository,
            ProjectRepository projectRepository,
            ProductRequiredDocumentsRepository productRequiredDocumentsRepository,
            UserRepository userRepository,
            DocumentStatusRepository documentStatusRepository) {
        this.projectDocumentUploadRepository = projectDocumentUploadRepository;
        this.projectMilestoneAssignmentRepository = projectMilestoneAssignmentRepository;
        this.projectRepository = projectRepository;
        this.productRequiredDocumentsRepository = productRequiredDocumentsRepository;
        this.userRepository = userRepository;
        this.documentStatusRepository = documentStatusRepository;
    }

    @Override
    public DocumentResponseDto uploadDocument(ProjectDocumentUploadRequestDto requestDto) {
        logger.info("Initiating document upload for project ID: {}, required document ID: {}",
                requestDto.getProjectId(), requestDto.getRequiredDocumentId());

        validateUploadRequest(requestDto);

        String fileName = sanitizeFileName(requestDto.getFileName());
        String fileUrl = bucketUrl + "/" + fileName;

        // Fetch required entities
        Project project = projectRepository.findActiveUserById(requestDto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + requestDto.getProjectId(), "PROJECT_NOT_FOUND"));

        ProductRequiredDocuments requiredDoc = productRequiredDocumentsRepository.findById(requestDto.getRequiredDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Required document not found: " + requestDto.getRequiredDocumentId(), "DOCUMENT_NOT_FOUND"));

        User uploadedBy = userRepository.findActiveUserById(requestDto.getUploadedById())
                .orElseThrow(() -> new ResourceNotFoundException("Uploader not found", "USER_NOT_FOUND"));

        User createdBy = userRepository.findActiveUserById(requestDto.getCreatedById())
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found", "USER_NOT_FOUND"));

        DocumentStatus pendingStatus = documentStatusRepository.findByName("PENDING")
                .orElseThrow(() -> new ResourceNotFoundException("Status PENDING not found", "STATUS_NOT_FOUND"));
        DocumentStatus uploadedStatus = documentStatusRepository.findByName("UPLOADED")
                .orElseThrow(() -> new ResourceNotFoundException("Status UPLOADED not found", "STATUS_NOT_FOUND"));

        // Check for existing project-level document (same project + same required document type)
        var existingOpt = projectDocumentUploadRepository
                .findActiveProjectLevelDocument(
                        requestDto.getProjectId(), requestDto.getRequiredDocumentId());

        ProjectDocumentUpload doc;
        boolean isReplacement = existingOpt.isPresent();

        if (isReplacement) {
            doc = existingOpt.get();

            if ("VERIFIED".equals(doc.getStatus().getName())) {
                throw new ValidationException("Cannot replace a VERIFIED document", "VERIFIED_DOCUMENT_REPLACEMENT");
            }

            // Keep old file for history
            doc.setOldFileUrl(doc.getFileUrl());
            doc.setOldFileName(doc.getFileName());
            doc.setReplacementCount(doc.getReplacementCount() + 1);
            logger.info("Replacing existing document ID: {}", doc.getId());
        } else {
            doc = new ProjectDocumentUpload();
            doc.setProject(project);
            doc.setRequiredDocument(requiredDoc);
            doc.setCreatedBy(requestDto.getCreatedById());
            doc.setCreatedDate(new Date());
            doc.setReplacementCount(0);
            doc.setStatus(pendingStatus);
            logger.info("Creating new project-level document");
        }

        // Common fields (new upload or replacement)
        doc.setFileUrl(fileUrl);
        doc.setFileName(fileName);
        doc.setFileFormat(requestDto.getFileFormat().toLowerCase());
        doc.setFileSizeKb(requestDto.getFileSizeKb());
        doc.setExpiryDate(requestDto.getExpiryDate() != null ? java.sql.Date.valueOf(requestDto.getExpiryDate()) : null);
        doc.setPermanent(Boolean.TRUE.equals(requestDto.getIsPermanent()));
        doc.setFromCompanyDoc(Boolean.TRUE.equals(requestDto.getIsFromCompanyDoc()));
        doc.setCompanyDocSourceId(requestDto.getCompanyDocSourceId());
        doc.setRemarks(requestDto.getRemarks());

        // Final state
        doc.setStatus(uploadedStatus);
        doc.setUploadedBy(uploadedBy);
        doc.setUploadTime(new Date());
        doc.setUpdatedBy(requestDto.getCreatedById());
        doc.setUpdatedDate(new Date());
        doc.setDeleted(false);

        doc = projectDocumentUploadRepository.save(doc);
        logger.info("Document {} successfully - ID: {}", isReplacement ? "replaced" : "uploaded", doc.getId());

        return mapToDocumentResponseDto(doc);
    }



    @Override
    public DocumentResponseDto updateDocumentStatus(Long documentId, ProjectDocumentStatusUpdateDto updateDto) {
        logger.info("Updating document status for ID: {} to {}", documentId, updateDto.getNewStatus());

        // Fetch document
        ProjectDocumentUpload documentUpload = projectDocumentUploadRepository.findActiveUserById(documentId)
                .orElseThrow(() -> {
                    logger.error("Document upload with ID {} not found or is deleted", documentId);
                    return new ResourceNotFoundException("Document upload with ID " + documentId + " not found or is deleted", "DOCUMENT_UPLOAD_NOT_FOUND");
                });

        User changedBy = userRepository.findActiveUserById(updateDto.getChangedById())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", updateDto.getChangedById());
                    return new ResourceNotFoundException("User with ID " + updateDto.getChangedById() + " not found or is deleted", "USER_NOT_FOUND");
                });

        DocumentStatus newStatusEntity = documentStatusRepository.findByName(updateDto.getNewStatus())
                .orElseThrow(() -> {
                    logger.error("Document status {} not found", updateDto.getNewStatus());
                    return new ResourceNotFoundException("Document status " + updateDto.getNewStatus() + " not found", "STATUS_NOT_FOUND");
                });


        // Validate status transition
        validateDocumentStatusTransition(documentUpload.getStatus(), newStatusEntity);

        // Validate remarks for REJECTED status
        if ("REJECTED".equals(newStatusEntity.getName()) && (updateDto.getRemarks() == null || updateDto.getRemarks().trim().isEmpty())) {
            logger.warn("Remarks are required for REJECTED status for document ID: {}", documentId);
            throw new ValidationException("Remarks are required for REJECTED status", "INVALID_REJECTED_STATUS");
        }

        // Sanitize remarks
        String sanitizedRemarks = updateDto.getRemarks() != null ? sanitizeRemarks(updateDto.getRemarks()) : null;

        // Update document
        documentUpload.setStatus(newStatusEntity);
        documentUpload.setRemarks(sanitizedRemarks);
        documentUpload.setUpdatedBy(updateDto.getChangedById());
        documentUpload.setUpdatedDate(new Date());

        documentUpload = projectDocumentUploadRepository.save(documentUpload);
        logger.info("Document status updated successfully for ID: {}", documentId);

        return mapToDocumentResponseDto(documentUpload);
    }

    private void validateUploadRequest(ProjectDocumentUploadRequestDto requestDto) {
        if (requestDto.getFileName() == null || requestDto.getFileName().trim().isEmpty()) {
            logger.warn("File name is empty or null");
            throw new ValidationException("File name cannot be empty", "INVALID_FILE_NAME");
        }
        if (requestDto.getUploadedById() == null) {
            logger.warn("Uploaded by user ID is null");
            throw new ValidationException("Uploaded by user ID cannot be null", "INVALID_UPLOADED_BY");
        }
        if (requestDto.getCreatedById() == null) {
            logger.warn("Created by user ID is null");
            throw new ValidationException("Created by user ID cannot be null", "INVALID_CREATED_BY");
        }
        if (requestDto.getRequiredDocumentId() == null) {
            logger.warn("Required document ID is null");
            throw new ValidationException("Required document ID cannot be null", "INVALID_REQUIRED_DOCUMENT_ID");
        }
    }

    private void validateDocumentStatusTransition(DocumentStatus currentStatus, DocumentStatus newStatus) {
        if (currentStatus.getName().equals(newStatus.getName())) {
            logger.warn("Attempted to set document to same status: {}", newStatus.getName());
            throw new ValidationException("Document is already in status: " + newStatus.getName(), "INVALID_STATUS_TRANSITION_SAME");
        }
        switch (currentStatus.getName()) {
            case "PENDING" -> {
                if (!"UPLOADED".equals(newStatus.getName())) {
                    logger.warn("Invalid status transition from PENDING to {}", newStatus.getName());
                    throw new ValidationException("Invalid transition from PENDING to " + newStatus.getName(), "INVALID_STATUS_TRANSITION_PENDING");
                }
            }
            case "UPLOADED" -> {
                if (!"VERIFIED".equals(newStatus.getName()) && !"REJECTED".equals(newStatus.getName())) {
                    logger.warn("Invalid status transition from UPLOADED to {}", newStatus.getName());
                    throw new ValidationException("Invalid transition from UPLOADED to " + newStatus.getName(), "INVALID_STATUS_TRANSITION_UPLOADED");
                }
            }
            case "VERIFIED", "REJECTED" -> {
                logger.warn("Attempted to change status from final state: {}", currentStatus.getName());
                throw new ValidationException("Cannot change status from " + currentStatus.getName(), "INVALID_STATUS_TRANSITION_FINAL");
            }
            default -> {
                logger.error("Invalid current status: {}", currentStatus.getName());
                throw new ValidationException("Invalid current status: " + currentStatus.getName(), "INVALID_CURRENT_STATUS");
            }
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String sanitized = fileName.trim().replaceAll("[^a-zA-Z0-9\\.\\-_]", "");
        if (sanitized.length() > 255) {
            logger.warn("File name exceeds maximum length of 255 characters");
            throw new ValidationException("File name cannot exceed 255 characters", "INVALID_FILE_NAME_LENGTH");
        }
        if (sanitized.isEmpty()) {
            logger.warn("Invalid file name after sanitization");
            throw new ValidationException("Invalid file name", "INVALID_FILE_NAME_FORMAT");
        }
        return sanitized;
    }

    private String sanitizeRemarks(String remarks) {
        if (remarks == null) {
            return null;
        }
        String sanitized = remarks.trim();
        if (sanitized.length() > 1000) {
            logger.warn("Remarks exceed maximum length of 1000 characters");
            throw new ValidationException("Remarks cannot exceed 1000 characters", "INVALID_REMARKS_LENGTH");
        }
        return sanitized;
    }

    private DocumentResponseDto mapToDocumentResponseDto(ProjectDocumentUpload documentUpload) {
        DocumentResponseDto dto = new DocumentResponseDto();
        dto.setId(documentUpload.getId());
        dto.setFileUrl(documentUpload.getFileUrl());
        dto.setFileName(documentUpload.getFileName());
        dto.setOldFileUrl(documentUpload.getOldFileUrl());
        dto.setOldFileName(documentUpload.getOldFileName());
        dto.setStatus(documentUpload.getStatus());
        dto.setRemarks(documentUpload.getRemarks());
        dto.setUploadTime(documentUpload.getUploadTime());
        dto.setExpiryDate(documentUpload.getExpiryDate());
        dto.setPermanent(documentUpload.isPermanent());
        dto.setExpired(documentUpload.isExpired());
        dto.setFileSizeKb(documentUpload.getFileSizeKb());
        dto.setFileFormat(documentUpload.getFileFormat());
        dto.setValidationPassed(documentUpload.isValidationPassed());
        dto.setValidationIssues(documentUpload.getValidationIssues());
        dto.setRequiredDocumentId(documentUpload.getRequiredDocument().getId());
        dto.setProjectId(documentUpload.getProject().getId());
        dto.setUploadedById(documentUpload.getUploadedBy().getId());
        dto.setCreatedBy(documentUpload.getCreatedBy());
        dto.setUpdatedBy(documentUpload.getUpdatedBy());
        dto.setCreatedDate(documentUpload.getCreatedDate());
        dto.setUpdatedDate(documentUpload.getUpdatedDate());
        dto.setReplacementCount(documentUpload.getReplacementCount());
        dto.setFromCompanyDoc(documentUpload.isFromCompanyDoc());
        dto.setCompanyDocSourceId(documentUpload.getCompanyDocSourceId());
        return dto;
    }
}