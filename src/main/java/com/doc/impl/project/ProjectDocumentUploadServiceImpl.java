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
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.*;
import com.doc.repository.documentRepo.*;
import com.doc.service.project.ProjectDocumentUploadService;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger logger =
            LoggerFactory.getLogger(ProjectDocumentUploadServiceImpl.class);

    private final ProjectDocumentUploadRepository projectDocumentUploadRepository;
    private final ProjectRepository projectRepository;
    private final ProductRequiredDocumentsRepository productRequiredDocumentsRepository;
    private final UserRepository userRepository;
    private final DocumentStatusRepository documentStatusRepository;
    private final CompanyDocumentRepository companyDocumentRepository;
    private final ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;

    @Value("${aws_path}")
    private String awsPath;

    public ProjectDocumentUploadServiceImpl(
            ProjectDocumentUploadRepository projectDocumentUploadRepository,
            ProjectRepository projectRepository,
            ProductRequiredDocumentsRepository productRequiredDocumentsRepository,
            UserRepository userRepository,
            DocumentStatusRepository documentStatusRepository,
            CompanyDocumentRepository companyDocumentRepository,
            ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository) {

        this.projectDocumentUploadRepository = projectDocumentUploadRepository;
        this.projectRepository = projectRepository;
        this.productRequiredDocumentsRepository = productRequiredDocumentsRepository;
        this.userRepository = userRepository;
        this.documentStatusRepository = documentStatusRepository;
        this.companyDocumentRepository = companyDocumentRepository;
        this.projectMilestoneAssignmentRepository = projectMilestoneAssignmentRepository;
    }


    @Override
    public DocumentResponseDto uploadDocument(ProjectDocumentUploadRequestDto requestDto) {

        logger.info(
                "[DOC-UPLOAD-START] projectId={}, requiredDocumentId={}, uploadedById={}, fileUrl={}, fileSizeKb={}, fileFormat={}",
                requestDto != null ? requestDto.getProjectId() : null,
                requestDto != null ? requestDto.getRequiredDocumentId() : null,
                requestDto != null ? requestDto.getUploadedById() : null,
                requestDto != null ? requestDto.getFileUrl() : null,
                requestDto != null ? requestDto.getFileSizeKb() : null,
                requestDto != null ? requestDto.getFileFormat() : null
        );

        validateUploadRequest(requestDto);

        validateFileSizeAgainstRequirement(
                requestDto.getRequiredDocumentId(),
                requestDto.getFileSizeKb()
        );

        String fileUrl = requestDto.getFileUrl();

        if (!StringUtils.hasText(fileUrl)) {
            throw new ValidationException("File URL cannot be empty", "INVALID_FILE_URL");
        }

        String extractedFileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        String fileName = sanitizeFileName(extractedFileName);

        if (!StringUtils.hasText(fileUrl)) {
            throw new ValidationException("File URL cannot be empty", "INVALID_FILE_URL");
        }

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

            String existingStatus = doc.getStatus() != null
                    ? doc.getStatus().getName()
                    : null;

            boolean admin = isAdmin(uploadedBy);

            logger.info(
                    "[DOC-UPLOAD-EXISTING] documentId={}, projectId={}, requiredDocumentId={}, currentStatus={}, uploadedById={}, roles={}, isAdmin={}",
                    doc.getId(),
                    requestDto.getProjectId(),
                    requestDto.getRequiredDocumentId(),
                    existingStatus,
                    uploadedBy.getId(),
                    getRoleNames(uploadedBy),
                    admin
            );

            validateReplacementAuthorization(doc, uploadedBy, "UPLOAD_EXISTING");

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

        // Save exactly what frontend sends
        doc.setFileName(fileName);
        doc.setFileUrl(fileUrl);

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

        doc.setDeleted(false);

        ProjectDocumentUpload savedDoc = projectDocumentUploadRepository.save(doc);

        logger.info(
                "[DOC-UPLOAD-SUCCESS] ID: {}, ProjectId: {}, RequiredDoc: {}, FileName: {}, FileUrl: {}, Size: {} KB, Format: {}, Status: {}, ReplacementCount: {}",
                savedDoc.getId(),
                requestDto.getProjectId(),
                requiredDoc.getName(),
                savedDoc.getFileName(),
                savedDoc.getFileUrl(),
                requestDto.getFileSizeKb(),
                fileFormat,
                savedDoc.getStatus() != null ? savedDoc.getStatus().getName() : null,
                savedDoc.getReplacementCount()
        );

        return mapToDocumentResponseDto(savedDoc);
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

        logger.info(
                "[DOC-STATUS-START] documentId={}, requestedStatus={}, changedById={}, remarks={}",
                documentId,
                updateDto != null ? updateDto.getNewStatus() : null,
                updateDto != null ? updateDto.getChangedById() : null,
                updateDto != null ? updateDto.getRemarks() : null
        );

        ProjectDocumentUpload documentUpload = projectDocumentUploadRepository
                .findActiveUserById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found",
                        "DOCUMENT_UPLOAD_NOT_FOUND"
                ));

        DocumentStatus newStatus = documentStatusRepository.findByName(updateDto.getNewStatus())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status not found",
                        "STATUS_NOT_FOUND"
                ));

        User statusChangedBy = userRepository.findActiveUserById(updateDto.getChangedById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status changing user not found",
                        "USER_NOT_FOUND"
                ));

        String currentStatusName = documentUpload.getStatus() != null
                ? documentUpload.getStatus().getName()
                : null;

        String requestedStatusName = newStatus.getName();
        boolean admin = isAdmin(statusChangedBy);

        logger.info(
                "[DOC-STATUS-AUTH] documentId={}, currentStatus={}, requestedStatus={}, changedById={}, roles={}, isAdmin={}",
                documentId,
                currentStatusName,
                requestedStatusName,
                statusChangedBy.getId(),
                getRoleNames(statusChangedBy),
                admin
        );

        /*
         * SECURITY VALIDATION ONLY:
         * Once a document is VERIFIED, a non-admin must not downgrade it to another status.
         * Otherwise VERIFIED -> REJECTED could bypass the ADMIN-only replacement rule.
         */
        if ("VERIFIED".equalsIgnoreCase(currentStatusName)
                && !"VERIFIED".equalsIgnoreCase(requestedStatusName)
                && !admin) {

            logger.warn(
                    "[DOC-STATUS-DENIED] Non-admin attempted to change VERIFIED document status. documentId={}, requestedStatus={}, changedById={}, roles={}",
                    documentId,
                    requestedStatusName,
                    statusChangedBy.getId(),
                    getRoleNames(statusChangedBy)
            );

            throw new ValidationException(
                    "Only ADMIN can change the status of a VERIFIED document",
                    "VERIFIED_DOCUMENT_STATUS_CHANGE_ADMIN_ONLY"
            );
        }

        validateDocumentStatusTransition(documentUpload.getStatus(), newStatus);

        documentUpload.setStatus(newStatus);
        documentUpload.setRemarks(updateDto.getRemarks());
        documentUpload.setUpdatedBy(updateDto.getChangedById());
        documentUpload.setUpdatedDate(new Date());

        documentUpload = projectDocumentUploadRepository.save(documentUpload);

        logger.info(
                "[DOC-STATUS-SAVED] documentId={}, oldStatus={}, newStatus={}, changedById={}",
                documentUpload.getId(),
                currentStatusName,
                newStatus.getName(),
                updateDto.getChangedById()
        );

        if ("VERIFIED".equalsIgnoreCase(newStatus.getName())) {

            logger.info(
                    "[DOC-VERIFY-COMPANY-SYNC-START] documentId={}, projectId={}, changedById={}",
                    documentUpload.getId(),
                    documentUpload.getProject() != null ? documentUpload.getProject().getId() : null,
                    updateDto.getChangedById()
            );

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
            if (requiredDoc == null) {
                throw new RuntimeException("Required document missing for uploaded document");
            }

            Optional<CompanyDocument> existingOpt =
                    companyDocumentRepository
                            .findByCompanyIdAndRequiredDocumentIdAndIsDeletedFalse(
                                    company.getId(),
                                    requiredDoc.getId()
                            );

            CompanyDocument companyDoc;

            if (existingOpt.isPresent()) {
                companyDoc = existingOpt.get();

                companyDoc.setOldFileUrl(companyDoc.getFileUrl());
                companyDoc.setOldFileName(companyDoc.getFileName());

                companyDoc.setReplacementCount(companyDoc.getReplacementCount() + 1);

            } else {
                companyDoc = new CompanyDocument();

                companyDoc.setCompany(company);
                companyDoc.setCompanyUnit(unit);
                companyDoc.setRequiredDocument(requiredDoc);

                companyDoc.setReplacementCount(0);
                companyDoc.setCreatedBy(
                        documentUpload.getCreatedBy() != null
                                ? documentUpload.getCreatedBy()
                                : updateDto.getChangedById()
                );
                companyDoc.setCreatedDate(new Date());
            }

            companyDoc.setFileUrl(documentUpload.getFileUrl());
            companyDoc.setFileName(documentUpload.getFileName());

            companyDoc.setStatus(newStatus);
            companyDoc.setRemarks(documentUpload.getRemarks());

            companyDoc.setUploadedBy(documentUpload.getUploadedBy());

            companyDoc.setUpdatedBy(updateDto.getChangedById());
            companyDoc.setUpdatedDate(new Date());

            companyDoc.setUploadTime(
                    documentUpload.getUploadTime() != null
                            ? documentUpload.getUploadTime()
                            : new Date()
            );

            companyDoc.setFileSizeKb(documentUpload.getFileSizeKb());
            companyDoc.setFileFormat(documentUpload.getFileFormat());

            companyDoc.setPermanent(documentUpload.isPermanent());
            companyDoc.setExpiryDate(documentUpload.getExpiryDate());

            companyDoc.setValidationPassed(documentUpload.isValidationPassed());
            companyDoc.setValidationIssues(documentUpload.getValidationIssues());

            User verifiedBy = userRepository.findActiveUserById(updateDto.getChangedById())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Verifier not found",
                            "USER_NOT_FOUND"
                    ));

            companyDoc.setVerifiedBy(verifiedBy);
            companyDoc.setVerifiedDate(new Date());

            CompanyDocument savedCompanyDoc = companyDocumentRepository.save(companyDoc);

            logger.info(
                    "[DOC-VERIFY-COMPANY-SYNC-SUCCESS] projectDocumentId={}, companyDocumentId={}, companyId={}, unitId={}, requiredDocumentId={}",
                    documentUpload.getId(),
                    savedCompanyDoc.getId(),
                    company.getId(),
                    unit.getId(),
                    requiredDoc.getId()
            );
        }

        logger.info(
                "[DOC-STATUS-SUCCESS] documentId={}, finalStatus={}, changedById={}",
                documentUpload.getId(),
                documentUpload.getStatus() != null ? documentUpload.getStatus().getName() : null,
                updateDto.getChangedById()
        );

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

        String sanitized = fileName.trim().replaceAll("[^a-zA-Z0-9\\\\.\\\\-_() ]", "");

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

        logger.info(
                "[DOC-REPLACE-START] documentId={}, projectId={}, requiredDocumentId={}, uploadedById={}, fileName={}, fileUrl={}, fileSizeKb={}, fileFormat={}",
                documentId,
                requestDto != null ? requestDto.getProjectId() : null,
                requestDto != null ? requestDto.getRequiredDocumentId() : null,
                requestDto != null ? requestDto.getUploadedById() : null,
                requestDto != null ? requestDto.getFileName() : null,
                requestDto != null ? requestDto.getFileUrl() : null,
                requestDto != null ? requestDto.getFileSizeKb() : null,
                requestDto != null ? requestDto.getFileFormat() : null
        );

        validateUploadRequest(requestDto);

        validateFileSizeAgainstRequirement(
                requestDto.getRequiredDocumentId(),
                requestDto.getFileSizeKb()
        );

        if (documentId == null) {
            throw new ValidationException(
                    "Document ID is required",
                    "INVALID_DOCUMENT_ID"
            );
        }

        ProjectDocumentUpload doc = projectDocumentUploadRepository.findActiveUserById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found",
                        "DOCUMENT_UPLOAD_NOT_FOUND"
                ));

        if (doc.getProject() == null
                || doc.getProject().getId() == null
                || !doc.getProject().getId().equals(requestDto.getProjectId())) {

            logger.warn(
                    "[DOC-REPLACE-DENIED-PROJECT] documentId={}, documentProjectId={}, requestProjectId={}, uploadedById={}",
                    documentId,
                    doc.getProject() != null ? doc.getProject().getId() : null,
                    requestDto.getProjectId(),
                    requestDto.getUploadedById()
            );

            throw new ValidationException(
                    "Document does not belong to this project",
                    "DOCUMENT_PROJECT_MISMATCH"
            );
        }

        if (doc.getRequiredDocument() == null
                || doc.getRequiredDocument().getId() == null
                || !doc.getRequiredDocument().getId().equals(requestDto.getRequiredDocumentId())) {

            logger.warn(
                    "[DOC-REPLACE-DENIED-REQUIRED-DOC] documentId={}, existingRequiredDocumentId={}, requestedRequiredDocumentId={}",
                    documentId,
                    doc.getRequiredDocument() != null ? doc.getRequiredDocument().getId() : null,
                    requestDto.getRequiredDocumentId()
            );

            throw new ValidationException(
                    "Required document ID cannot be changed during replacement",
                    "REQUIRED_DOCUMENT_MISMATCH"
            );
        }

        User replacementRequestedBy = userRepository
                .findActiveUserById(requestDto.getUploadedById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Uploader not found",
                        "USER_NOT_FOUND"
                ));

        String currentDocumentStatus = doc.getStatus() != null
                ? doc.getStatus().getName()
                : null;

        validateReplacementAuthorization(doc, replacementRequestedBy, "REPLACE_API");

        DocumentStatus uploadedStatus = documentStatusRepository.findByName("UPLOADED")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status UPLOADED not found",
                        "STATUS_NOT_FOUND"
                ));

        String fileUrl = requestDto.getFileUrl();

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
        doc.setUploadedBy(replacementRequestedBy);
        doc.setUploadTime(new Date());

        doc.setUpdatedBy(requestDto.getUploadedById());
        doc.setUpdatedDate(new Date());

        doc.setValidationPassed(false);
        doc.setValidationIssues(null);

        doc.setReplacementCount(doc.getReplacementCount() + 1);

        ProjectDocumentUpload saved = projectDocumentUploadRepository.save(doc);

        logger.info(
                "[DOC-REPLACE-SUCCESS] documentId={}, projectId={}, previousStatus={}, newStatus={}, replacedById={}, replacementCount={}, oldFileUrl={}, newFileUrl={}",
                saved.getId(),
                saved.getProject() != null ? saved.getProject().getId() : null,
                currentDocumentStatus,
                saved.getStatus() != null ? saved.getStatus().getName() : null,
                replacementRequestedBy.getId(),
                saved.getReplacementCount(),
                saved.getOldFileUrl(),
                saved.getFileUrl()
        );

        return mapToDocumentResponseDto(saved);
    }

    /**
     * Replacement authorization shared by the explicit replacement endpoint
     * and the upload endpoint when an active project-level document already exists.
     *
     * Rules:
     * 1. ADMIN may replace any document.
     * 2. VERIFIED may only be replaced directly by ADMIN.
     * 3. REJECTED may be replaced by the user assigned to an active REWORK
     *    milestone for the same project.
     * 4. PENDING/UPLOADED may be replaced by the original uploader or by the
     *    user assigned to an active REWORK milestone.
     * 5. Any other state is denied for non-admin users.
     */
    private void validateReplacementAuthorization(
            ProjectDocumentUpload document,
            User requestedBy,
            String source
    ) {
        Long projectId = document.getProject() != null
                ? document.getProject().getId()
                : null;

        String statusName = document.getStatus() != null
                ? document.getStatus().getName()
                : null;

        boolean admin = isAdmin(requestedBy);
        boolean originalUploader = document.getUploadedBy() != null
                && document.getUploadedBy().getId() != null
                && document.getUploadedBy().getId().equals(requestedBy.getId());

        List<ProjectMilestoneAssignment> reworkAssignments = projectId == null
                ? List.of()
                : projectMilestoneAssignmentRepository
                .findByProjectIdAndIsDeletedFalse(projectId);

        boolean assignedToReworkMilestone = reworkAssignments.stream()
                .anyMatch(assignment ->
                        assignment.getStatus() != null
                                && assignment.getStatus().getName() != null
                                && "REWORK".equalsIgnoreCase(
                                assignment.getStatus().getName()
                        )
                                && assignment.getAssignedUser() != null
                                && assignment.getAssignedUser().getId() != null
                                && assignment.getAssignedUser().getId()
                                .equals(requestedBy.getId())
                );

        List<Long> reworkAssignmentIds = reworkAssignments.stream()
                .filter(assignment ->
                        assignment.getStatus() != null
                                && assignment.getStatus().getName() != null
                                && "REWORK".equalsIgnoreCase(
                                assignment.getStatus().getName()
                        )
                )
                .map(ProjectMilestoneAssignment::getId)
                .toList();

        logger.info(
                "[DOC-REPLACE-AUTH-CHECK] source={}, documentId={}, projectId={}, status={}, " +
                        "requestedById={}, roles={}, isAdmin={}, originalUploader={}, " +
                        "assignedToReworkMilestone={}, reworkAssignmentIds={}",
                source,
                document.getId(),
                projectId,
                statusName,
                requestedBy.getId(),
                getRoleNames(requestedBy),
                admin,
                originalUploader,
                assignedToReworkMilestone,
                reworkAssignmentIds
        );

        if (admin) {
            logger.info(
                    "[DOC-REPLACE-AUTH-ALLOWED] source={}, documentId={}, reason=ADMIN, userId={}",
                    source,
                    document.getId(),
                    requestedBy.getId()
            );
            return;
        }

        if ("VERIFIED".equalsIgnoreCase(statusName)) {
            denyReplacement(
                    source,
                    document,
                    requestedBy,
                    "Only ADMIN can directly replace a VERIFIED document",
                    "VERIFIED_DOCUMENT_REPLACEMENT_ADMIN_ONLY"
            );
        }

        if ("REJECTED".equalsIgnoreCase(statusName)) {
            if (!assignedToReworkMilestone) {
                denyReplacement(
                        source,
                        document,
                        requestedBy,
                        "Only the user assigned to the REWORK milestone can replace this rejected document",
                        "REWORK_DOCUMENT_REPLACEMENT_NOT_ALLOWED"
                );
            }

            logger.info(
                    "[DOC-REPLACE-AUTH-ALLOWED] source={}, documentId={}, reason=ASSIGNED_REWORK_USER, userId={}",
                    source,
                    document.getId(),
                    requestedBy.getId()
            );
            return;
        }

        if ("PENDING".equalsIgnoreCase(statusName)
                || "UPLOADED".equalsIgnoreCase(statusName)) {

            if (!originalUploader && !assignedToReworkMilestone) {
                denyReplacement(
                        source,
                        document,
                        requestedBy,
                        "Only the original uploader or assigned REWORK user can replace this document",
                        "DOCUMENT_REPLACEMENT_NOT_ALLOWED"
                );
            }

            logger.info(
                    "[DOC-REPLACE-AUTH-ALLOWED] source={}, documentId={}, reason={}, userId={}",
                    source,
                    document.getId(),
                    assignedToReworkMilestone ? "ASSIGNED_REWORK_USER" : "ORIGINAL_UPLOADER",
                    requestedBy.getId()
            );
            return;
        }

        denyReplacement(
                source,
                document,
                requestedBy,
                "Document cannot be replaced in its current status: " + statusName,
                "INVALID_DOCUMENT_STATUS_FOR_REPLACEMENT"
        );
    }

    private void denyReplacement(
            String source,
            ProjectDocumentUpload document,
            User requestedBy,
            String message,
            String errorCode
    ) {
        logger.warn(
                "[DOC-REPLACE-AUTH-DENIED] source={}, documentId={}, projectId={}, " +
                        "status={}, requestedById={}, roles={}, errorCode={}, message={}",
                source,
                document.getId(),
                document.getProject() != null ? document.getProject().getId() : null,
                document.getStatus() != null ? document.getStatus().getName() : null,
                requestedBy.getId(),
                getRoleNames(requestedBy),
                errorCode,
                message
        );

        throw new ValidationException(message, errorCode);
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

    /**
     * Role validation helper used only for VERIFIED document protection.
     */
    private boolean isAdmin(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }

        return user.getRoles().stream()
                .anyMatch(role ->
                        role != null
                                && role.getName() != null
                                && "ADMIN".equalsIgnoreCase(role.getName())
                );
    }

    /**
     * Used only for readable authorization logs.
     */
    private List<String> getRoleNames(User user) {
        if (user == null || user.getRoles() == null) {
            return List.of();
        }

        return user.getRoles().stream()
                .filter(role -> role != null && role.getName() != null)
                .map(role -> role.getName())
                .toList();
    }

}
