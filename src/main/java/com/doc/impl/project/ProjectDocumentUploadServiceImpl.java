package com.doc.impl.project;

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
import org.springframework.util.StringUtils;

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
    private final CompanyDocumentRepository companyDocumentRepository;

    public ProjectDocumentUploadServiceImpl(
            ProjectDocumentUploadRepository projectDocumentUploadRepository,
            ProjectRepository projectRepository,
            ProductRequiredDocumentsRepository productRequiredDocumentsRepository,
            UserRepository userRepository,
            DocumentStatusRepository documentStatusRepository,
            CompanyDocumentRepository companyDocumentRepository) {

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

        validateFileSizeAgainstRequirement(
                requestDto.getRequiredDocumentId(),
                requestDto.getFileSizeKb()
        );

        /*
         * IMPORTANT:
         * Currently your request DTO does not have fileUrl.
         * So this assumes requestDto.getFileName() contains the uploaded S3 URL.
         * If frontend sends only actual file name here, then add fileUrl field in DTO.
         */
        String fileUrl = requestDto.getFileName();

        if (!StringUtils.hasText(fileUrl)) {
            throw new ValidationException("File URL cannot be empty", "INVALID_FILE_URL");
        }

        String extractedFileName;
        try {
            extractedFileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        } catch (Exception e) {
            throw new ValidationException("Invalid file URL format", "INVALID_FILE_URL_FORMAT");
        }

        String fileName = sanitizeFileName(extractedFileName);

        String fileFormat = requestDto.getFileFormat() != null
                ? requestDto.getFileFormat().trim().toLowerCase()
                : null;

        validateFileFormat(fileFormat);

        Project project = projectRepository.findActiveUserById(requestDto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "PROJECT_NOT_FOUND"
                ));

        ProductRequiredDocuments requiredDoc = productRequiredDocumentsRepository
                .findById(requestDto.getRequiredDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Required document not found",
                        "DOCUMENT_NOT_FOUND"
                ));

        User uploadedBy = userRepository.findActiveUserById(requestDto.getUploadedById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Uploader not found",
                        "USER_NOT_FOUND"
                ));

        DocumentStatus uploadedStatus = documentStatusRepository.findByName("UPLOADED")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status UPLOADED not found",
                        "STATUS_NOT_FOUND"
                ));

        Optional<ProjectDocumentUpload> existingOpt =
                projectDocumentUploadRepository.findActiveProjectLevelDocument(
                        requestDto.getProjectId(),
                        requestDto.getRequiredDocumentId()
                );

        ProjectDocumentUpload doc;

        if (existingOpt.isPresent()) {

            doc = existingOpt.get();

            if (doc.getStatus() != null
                    && "VERIFIED".equalsIgnoreCase(doc.getStatus().getName())) {
                throw new ValidationException(
                        "Cannot replace VERIFIED document",
                        "VERIFIED_DOCUMENT_REPLACEMENT"
                );
            }

            doc.setOldFileUrl(doc.getFileUrl());
            doc.setOldFileName(doc.getFileName());
            doc.setReplacementCount(doc.getReplacementCount() + 1);

        } else {

            doc = new ProjectDocumentUpload();

            doc.setProject(project);
            doc.setRequiredDocument(requiredDoc);

            // from request
            doc.setCreatedBy(requestDto.getCreatedById());
            doc.setCreatedDate(new Date());

            doc.setReplacementCount(0);
        }

        // Values coming from request / derived from request
        doc.setFileUrl(fileUrl);
        doc.setFileName(fileName);
        doc.setFileFormat(fileFormat);
        doc.setFileSizeKb(requestDto.getFileSizeKb());

        doc.setExpiryDate(requestDto.getExpiryDate());

        doc.setPermanent(Boolean.TRUE.equals(requestDto.getIsPermanent()));
        doc.setFromCompanyDoc(Boolean.TRUE.equals(requestDto.getIsFromCompanyDoc()));
        doc.setCompanyDocSourceId(requestDto.getCompanyDocSourceId());
        doc.setRemarks(requestDto.getRemarks());

        // System/action fields
        doc.setStatus(uploadedStatus);
        doc.setUploadedBy(uploadedBy);
        doc.setUploadTime(new Date());

        /*
         * Better than createdById here.
         * The person uploading/replacing the document is the latest updater.
         */
        doc.setUpdatedBy(requestDto.getUploadedById());
        doc.setUpdatedDate(new Date());

        doc.setDeleted(false);

        doc = projectDocumentUploadRepository.save(doc);

        logger.info(
                "Document uploaded successfully. ID: {}, ProjectId: {}, RequiredDoc: {}, Size: {} KB, Format: {}",
                doc.getId(),
                requestDto.getProjectId(),
                requiredDoc.getName(),
                requestDto.getFileSizeKb(),
                fileFormat
        );

        return mapToDocumentResponseDto(doc);
    }

    private void validateFileFormat(String fileFormat) {

        if (!StringUtils.hasText(fileFormat)) {
            throw new ValidationException(
                    "File format is required",
                    "INVALID_FILE_FORMAT"
            );
        }

        if (!fileFormat.matches("pdf|jpg|jpeg|png")) {
            throw new ValidationException(
                    "Only pdf, jpg, jpeg, png allowed",
                    "INVALID_FILE_FORMAT"
            );
        }
    }
    /**
     * Validates that the uploaded file size does not exceed the maximum allowed size
     * defined in ProductRequiredDocuments.
     */
    private void validateFileSizeAgainstRequirement(Long requiredDocumentId, Integer uploadedFileSizeKb) {
        if (uploadedFileSizeKb == null || uploadedFileSizeKb <= 0) {
            throw new ValidationException("File size is required and must be positive", "INVALID_FILE_SIZE");
        }

        ProductRequiredDocuments requiredDoc = productRequiredDocumentsRepository
                .findById(requiredDocumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Required document not found", "DOCUMENT_NOT_FOUND"));

        Integer maxAllowedKb = requiredDoc.getMaxFileSizeKb();

        if (maxAllowedKb != null && uploadedFileSizeKb > maxAllowedKb) {
            throw new ValidationException(
                    String.format("File size exceeds maximum limit for this document. " +
                                    "Maximum allowed: %d KB, Uploaded: %d KB",
                            maxAllowedKb, uploadedFileSizeKb),
                    "ERR_MAX_FILE_SIZE_EXCEEDED"
            );
        }

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

            // Check existing company document
            Optional<CompanyDocument> existingOpt =
                    companyDocumentRepository
                            .findByCompanyIdAndRequiredDocumentIdAndIsDeletedFalse(
                                    company.getId(),
                                    requiredDoc.getId()
                            );

            CompanyDocument companyDoc;

            if (existingOpt.isPresent()) {
                // Update (Replacement)
                companyDoc = existingOpt.get();

                companyDoc.setOldFileUrl(companyDoc.getFileUrl());
                companyDoc.setOldFileName(companyDoc.getFileName());

                companyDoc.setFileUrl(documentUpload.getFileUrl());
                companyDoc.setFileName(documentUpload.getFileName());

                companyDoc.setReplacementCount(companyDoc.getReplacementCount() + 1);
                // Common fields
                companyDoc.setStatus(newStatus);
                companyDoc.setRemarks(documentUpload.getRemarks());

                companyDoc.setUploadedBy(documentUpload.getUploadedBy());
                companyDoc.setCreatedBy(documentUpload.getCreatedBy());
                companyDoc.setUpdatedBy(documentUpload.getUpdatedBy());

            } else {
                // Create new
                companyDoc = new CompanyDocument();

                companyDoc.setCompany(company);
                companyDoc.setCompanyUnit(unit);
                companyDoc.setRequiredDocument(requiredDoc);

                companyDoc.setFileUrl(documentUpload.getFileUrl());
                companyDoc.setFileName(documentUpload.getFileName());

                companyDoc.setReplacementCount(0);
            }

            // Common fields
            companyDoc.setStatus(newStatus);
            companyDoc.setRemarks(documentUpload.getRemarks());

            companyDoc.setUploadedBy(documentUpload.getUploadedBy());

            companyDoc.setCreatedBy(
                    documentUpload.getCreatedBy() != null
                            ? documentUpload.getCreatedBy()
                            : updateDto.getChangedById()
            );

            companyDoc.setUpdatedBy(updateDto.getChangedById());
            companyDoc.setUpdatedDate(new Date());

            companyDoc.setUploadTime(
                    documentUpload.getUploadTime() != null
                            ? documentUpload.getUploadTime()
                            : new Date()
            );

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

    private void validateUploadRequest(ProjectDocumentUploadRequestDto requestDto) {

        if (requestDto.getProjectId() == null)
            throw new ValidationException("Project ID required", "INVALID_PROJECT_ID");

        if (requestDto.getRequiredDocumentId() == null)
            throw new ValidationException("Required document ID required", "INVALID_REQUIRED_DOCUMENT_ID");

        if (requestDto.getUploadedById() == null)
            throw new ValidationException("UploadedBy ID required", "INVALID_UPLOADED_BY");

        if (requestDto.getFileSizeKb() == null || requestDto.getFileSizeKb() <= 0)
            throw new ValidationException("Valid file size is required", "INVALID_FILE_SIZE");
    }

    private void validateDocumentStatusTransition(DocumentStatus currentStatus, DocumentStatus newStatus) {

        if (currentStatus.getName().equals(newStatus.getName())) {
            throw new ValidationException("Already in same status", "INVALID_STATUS_TRANSITION");
        }
    }

    private String sanitizeFileName(String fileName) {

        if (!StringUtils.hasText(fileName)) {
            throw new ValidationException("File name cannot be empty", "INVALID_FILE_NAME");
        }

        String sanitized = fileName.trim().replaceAll("[^a-zA-Z0-9\\.\\-_() ]", "");

        if (sanitized.length() > 255) {
            throw new ValidationException("File name too long (max 255 characters)", "INVALID_FILE_NAME_LENGTH");
        }

        if (sanitized.isEmpty()) {
            throw new ValidationException("Invalid file name format", "INVALID_FILE_NAME_FORMAT");
        }

        return sanitized;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getCompanyDocuments(Long companyId, Long companyUnitId) {

        if (companyId == null) {
            throw new ValidationException("Company ID is required", "INVALID_COMPANY_ID");
        }

        if (companyUnitId == null) {
            throw new ValidationException("Company Unit ID is required", "INVALID_COMPANY_UNIT_ID");
        }

        List<CompanyDocument> documents =
                companyDocumentRepository.findByCompanyIdAndCompanyUnitIdAndIsDeletedFalse(
                        companyId,
                        companyUnitId
                );

        return documents.stream()
                .map(this::mapCompanyDocumentToDto)
                .toList();
    }

    private DocumentResponseDto mapCompanyDocumentToDto(CompanyDocument doc) {

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

        dto.setRequiredDocumentId(doc.getRequiredDocument() != null
                ? doc.getRequiredDocument().getId()
                : null);

        dto.setRequiredDocumentName(doc.getRequiredDocument() != null
                ? doc.getRequiredDocument().getName()
                : null);

        dto.setUploadedById(doc.getUploadedBy() != null
                ? doc.getUploadedBy().getId()
                : null);

        dto.setCreatedBy(doc.getCreatedBy());
        dto.setUpdatedBy(doc.getUpdatedBy());
        dto.setCreatedDate(doc.getCreatedDate());
        dto.setUpdatedDate(doc.getUpdatedDate());

        dto.setReplacementCount(doc.getReplacementCount());
        dto.setFromCompanyDoc(true);
        dto.setCompanyDocSourceId(doc.getId());

        return dto;
    }



    @Override
    @Transactional
    public DocumentResponseDto replaceDocument(Long documentId, ProjectDocumentUploadRequestDto requestDto) {

        validateUploadRequest(requestDto);

        validateFileSizeAgainstRequirement(
                requestDto.getRequiredDocumentId(),
                requestDto.getFileSizeKb()
        );

        ProjectDocumentUpload doc = projectDocumentUploadRepository.findActiveUserById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found",
                        "DOCUMENT_UPLOAD_NOT_FOUND"
                ));

        if (!doc.getProject().getId().equals(requestDto.getProjectId())) {
            throw new ValidationException(
                    "Document does not belong to this project",
                    "DOCUMENT_PROJECT_MISMATCH"
            );
        }

        if (doc.getStatus() != null &&
                "VERIFIED".equalsIgnoreCase(doc.getStatus().getName())) {
            throw new ValidationException(
                    "Cannot replace VERIFIED document",
                    "VERIFIED_DOCUMENT_REPLACEMENT"
            );
        }

        User uploadedBy = userRepository.findActiveUserById(requestDto.getUploadedById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Uploader not found",
                        "USER_NOT_FOUND"
                ));

        DocumentStatus uploadedStatus = documentStatusRepository.findByName("UPLOADED")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status UPLOADED not found",
                        "STATUS_NOT_FOUND"
                ));

        String fileUrl = requestDto.getFileName();

        if (!StringUtils.hasText(fileUrl)) {
            throw new ValidationException("File URL cannot be empty", "INVALID_FILE_URL");
        }

        String extractedFileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        String fileName = sanitizeFileName(extractedFileName);

        String fileFormat = requestDto.getFileFormat() != null
                ? requestDto.getFileFormat().trim().toLowerCase()
                : null;

        validateFileFormat(fileFormat);

        doc.setOldFileUrl(doc.getFileUrl());
        doc.setOldFileName(doc.getFileName());

        doc.setFileUrl(fileUrl);
        doc.setFileName(fileName);
        doc.setFileFormat(fileFormat);
        doc.setFileSizeKb(requestDto.getFileSizeKb());

        doc.setExpiryDate(requestDto.getExpiryDate());
        doc.setPermanent(Boolean.TRUE.equals(requestDto.getIsPermanent()));
        doc.setFromCompanyDoc(Boolean.TRUE.equals(requestDto.getIsFromCompanyDoc()));
        doc.setCompanyDocSourceId(requestDto.getCompanyDocSourceId());
        doc.setRemarks(requestDto.getRemarks());

        doc.setStatus(uploadedStatus);
        doc.setUploadedBy(uploadedBy);
        doc.setUploadTime(new Date());

        doc.setUpdatedBy(requestDto.getUploadedById());
        doc.setUpdatedDate(new Date());

        doc.setValidationPassed(false);
        doc.setValidationIssues(null);

        doc.setReplacementCount(doc.getReplacementCount() + 1);

        ProjectDocumentUpload saved = projectDocumentUploadRepository.save(doc);

        return mapToDocumentResponseDto(saved);
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
        dto.setRequiredDocumentId(doc.getRequiredDocument() != null
                ? doc.getRequiredDocument().getId()
                : null);

        dto.setRequiredDocumentName(doc.getRequiredDocument() != null
                ? doc.getRequiredDocument().getName()
                : null);

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