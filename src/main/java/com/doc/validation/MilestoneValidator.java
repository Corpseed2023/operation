package com.doc.validation;

import com.doc.em.DocumentExpiryType;
import com.doc.entity.document.ApplicantType;
import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.product.Product;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.project.ProjectPortalDetail;
import com.doc.exception.ValidationException;
import com.doc.repository.ProductDocumentMappingRepository;
import com.doc.repository.documentRepo.ProjectDocumentUploadRepository;
import com.doc.repository.projectRepo.ProjectPortalDetailRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MilestoneValidator {

    private static final Logger logger =
            LogManager.getLogger(MilestoneValidator.class);

    /*
     * Explicit business timezone ensures consistent document-expiry
     * validation even when the application server uses another timezone.
     */
    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Kolkata");

    @Autowired
    private ProjectDocumentUploadRepository projectDocumentUploadRepository;

    @Autowired
    private ProductDocumentMappingRepository productDocumentMappingRepository;

    @Autowired
    private ProjectPortalDetailRepository portalDetailRepository;

    /**
     * Documentation milestone validation.
     *
     * Document verification is not required, but all mandatory
     * documents must be uploaded and valid.
     */
    public void validateDocumentMilestone(
            ProjectMilestoneAssignment assignment
    ) {

        Long assignmentId =
                assignment != null ? assignment.getId() : null;

        logger.info(
                "[DOCUMENT-MILESTONE-VALIDATION-START] assignmentId={}",
                assignmentId
        );

        validateDocuments(assignment, false);

        logger.info(
                "[DOCUMENT-MILESTONE-VALIDATION-SUCCESS] assignmentId={}",
                assignmentId
        );
    }

    /**
     * Legal Verification milestone validation.
     *
     * All mandatory documents must be uploaded, valid and VERIFIED.
     */
    public void validateLegalMilestone(
            ProjectMilestoneAssignment assignment
    ) {

        Long assignmentId =
                assignment != null ? assignment.getId() : null;

        logger.info(
                "[LEGAL-MILESTONE-VALIDATION-START] assignmentId={}",
                assignmentId
        );

        validateDocuments(assignment, true);

        logger.info(
                "[LEGAL-MILESTONE-VALIDATION-SUCCESS] assignmentId={}",
                assignmentId
        );
    }

    /**
     * Filing milestone validation.
     *
     * At least one approved portal detail must exist.
     */
    public void validateFillingMilestone(
            ProjectMilestoneAssignment assignment
    ) {

        Long assignmentId =
                assignment != null ? assignment.getId() : null;

        logger.info(
                "[FILING-MILESTONE-VALIDATION-START] assignmentId={}",
                assignmentId
        );

        if (assignment == null || assignment.getProject() == null) {

            logger.error(
                    "[FILING-MILESTONE-VALIDATION-FAILED] " +
                            "reason=INVALID_ASSIGNMENT, assignmentId={}",
                    assignmentId
            );

            throw new ValidationException(
                    "Invalid milestone assignment.",
                    "INVALID_ASSIGNMENT"
            );
        }

        Project project = assignment.getProject();

        if (project.getId() == null) {

            logger.error(
                    "[FILING-MILESTONE-VALIDATION-FAILED] " +
                            "reason=PROJECT_MISSING, assignmentId={}",
                    assignmentId
            );

            throw new ValidationException(
                    "Project not found for milestone validation.",
                    "PROJECT_MISSING"
            );
        }

        Long projectId = project.getId();

        logger.debug(
                "[PORTAL-DETAIL-FETCH-START] " +
                        "assignmentId={}, projectId={}",
                assignmentId,
                projectId
        );

        List<ProjectPortalDetail> portalDetails =
                portalDetailRepository
                        .findByProjectIdAndIsDeletedFalse(projectId);

        int portalDetailCount =
                portalDetails != null ? portalDetails.size() : 0;

        logger.debug(
                "[PORTAL-DETAIL-FETCH-COMPLETED] " +
                        "assignmentId={}, projectId={}, count={}",
                assignmentId,
                projectId,
                portalDetailCount
        );

        if (portalDetails == null || portalDetails.isEmpty()) {

            logger.warn(
                    "[FILING-MILESTONE-VALIDATION-FAILED] " +
                            "reason=PORTAL_DETAILS_MISSING, " +
                            "assignmentId={}, projectId={}",
                    assignmentId,
                    projectId
            );

            throw new ValidationException(
                    "Cannot start Filing milestone. " +
                            "No portal details have been added for this project.",
                    "PORTAL_DETAILS_MISSING"
            );
        }

        for (ProjectPortalDetail portalDetail : portalDetails) {

            if (portalDetail == null) {
                continue;
            }

            logger.debug(
                    "[PORTAL-DETAIL] projectId={}, portalDetailId={}, " +
                            "portalName={}, status={}",
                    projectId,
                    portalDetail.getId(),
                    portalDetail.getPortalName(),
                    portalDetail.getStatus()
            );
        }

        boolean hasApprovedPortal =
                portalDetails.stream()
                        .filter(portalDetail -> portalDetail != null)
                        .anyMatch(portalDetail ->
                                portalDetail.getStatus() != null
                                        && "APPROVED".equalsIgnoreCase(
                                        portalDetail.getStatus()
                                )
                        );

        if (!hasApprovedPortal) {

            logger.warn(
                    "[FILING-MILESTONE-VALIDATION-FAILED] " +
                            "reason=PORTAL_NOT_APPROVED, " +
                            "assignmentId={}, projectId={}, portalCount={}",
                    assignmentId,
                    projectId,
                    portalDetailCount
            );

            throw new ValidationException(
                    "Cannot start Filing milestone. " +
                            "At least one portal detail must be APPROVED.",
                    "PORTAL_NOT_APPROVED"
            );
        }

        logger.info(
                "[FILING-MILESTONE-VALIDATION-SUCCESS] " +
                        "assignmentId={}, projectId={}",
                assignmentId,
                projectId
        );
    }

    /**
     * Validates all mandatory documents configured for the project's
     * product and applicant type.
     *
     * @param assignment          milestone assignment
     * @param requireVerification true when VERIFIED status is mandatory
     */
    private void validateDocuments(
            ProjectMilestoneAssignment assignment,
            boolean requireVerification
    ) {

        Long assignmentId =
                assignment != null ? assignment.getId() : null;

        logger.debug(
                "[DOCUMENT-VALIDATION-START] " +
                        "assignmentId={}, requireVerification={}",
                assignmentId,
                requireVerification
        );

        if (assignment == null || assignment.getProject() == null) {

            logger.error(
                    "[DOCUMENT-VALIDATION-FAILED] " +
                            "reason=INVALID_ASSIGNMENT, assignmentId={}",
                    assignmentId
            );

            throw new ValidationException(
                    "Invalid milestone assignment.",
                    "INVALID_ASSIGNMENT"
            );
        }

        Project project = assignment.getProject();

        if (project.getId() == null) {

            logger.error(
                    "[DOCUMENT-VALIDATION-FAILED] " +
                            "reason=PROJECT_MISSING, assignmentId={}",
                    assignmentId
            );

            throw new ValidationException(
                    "Project not found for milestone validation.",
                    "PROJECT_MISSING"
            );
        }

        Long projectId = project.getId();

        if (project.getApplicantType() == null) {

            logger.warn(
                    "[DOCUMENT-VALIDATION-FAILED] " +
                            "reason=APPLICANT_TYPE_MISSING, " +
                            "assignmentId={}, projectId={}",
                    assignmentId,
                    projectId
            );

            throw new ValidationException(
                    "Applicant Type must be selected before completing milestone.",
                    "APPLICANT_TYPE_MISSING"
            );
        }

        Product product = project.getProduct();
        ApplicantType applicantType = project.getApplicantType();

        if (product == null) {

            logger.error(
                    "[DOCUMENT-VALIDATION-FAILED] " +
                            "reason=PRODUCT_MISSING, " +
                            "assignmentId={}, projectId={}",
                    assignmentId,
                    projectId
            );

            throw new ValidationException(
                    "Project is not linked to a valid product.",
                    "PRODUCT_MISSING"
            );
        }

        logger.debug(
                "[DOCUMENT-MAPPING-FETCH-START] " +
                        "projectId={}, productId={}, applicantTypeId={}",
                projectId,
                product.getId(),
                applicantType.getId()
        );

        List<ProductDocumentMapping> requiredMappings =
                productDocumentMappingRepository
                        .findByProductAndApplicantType(
                                product,
                                applicantType
                        );

        int mappingCount =
                requiredMappings != null
                        ? requiredMappings.size()
                        : 0;

        logger.debug(
                "[DOCUMENT-MAPPING-FETCH-COMPLETED] " +
                        "projectId={}, mappingCount={}",
                projectId,
                mappingCount
        );

        if (requiredMappings == null || requiredMappings.isEmpty()) {

            logger.warn(
                    "[DOCUMENT-VALIDATION-FAILED] " +
                            "reason=DOC_MAPPING_MISSING, " +
                            "projectId={}, productId={}, applicantTypeId={}",
                    projectId,
                    product.getId(),
                    applicantType.getId()
            );

            throw new ValidationException(
                    "No document configuration found for this product " +
                            "and applicant type.",
                    "DOC_MAPPING_MISSING"
            );
        }

        List<ProjectDocumentUpload> uploadedDocuments =
                projectDocumentUploadRepository
                        .findByProjectIdAndIsDeletedFalse(projectId);

        if (uploadedDocuments == null) {
            uploadedDocuments = List.of();
        }

        logger.debug(
                "[PROJECT-DOCUMENT-FETCH-COMPLETED] " +
                        "projectId={}, uploadedDocumentCount={}",
                projectId,
                uploadedDocuments.size()
        );

        Map<Long, List<ProjectDocumentUpload>> uploadedMap =
                uploadedDocuments.stream()
                        .filter(upload ->
                                upload != null
                                        && !upload.isDeleted()
                                        && upload.getRequiredDocument() != null
                                        && upload.getRequiredDocument()
                                        .getId() != null
                        )
                        .collect(
                                Collectors.groupingBy(
                                        upload ->
                                                upload.getRequiredDocument()
                                                        .getId()
                                )
                        );

        LocalDate today =
                LocalDate.now(BUSINESS_ZONE);

        logger.debug(
                "[DOCUMENT-EXPIRY-REFERENCE-DATE] " +
                        "projectId={}, currentDate={}, businessZone={}",
                projectId,
                today,
                BUSINESS_ZONE
        );

        for (ProductDocumentMapping mapping : requiredMappings) {

            if (mapping == null) {
                logger.debug(
                        "[DOCUMENT-MAPPING-SKIPPED] " +
                                "projectId={}, reason=NULL_MAPPING",
                        projectId
                );
                continue;
            }

            if (!mapping.isMandatory() || !mapping.isActive()) {

                logger.debug(
                        "[DOCUMENT-MAPPING-SKIPPED] " +
                                "projectId={}, mappingId={}, mandatory={}, active={}",
                        projectId,
                        mapping.getId(),
                        mapping.isMandatory(),
                        mapping.isActive()
                );

                continue;
            }

            ProductRequiredDocuments requiredDocument =
                    mapping.getRequiredDocument();

            if (requiredDocument == null) {

                logger.warn(
                        "[DOCUMENT-MAPPING-SKIPPED] " +
                                "projectId={}, mappingId={}, " +
                                "reason=REQUIRED_DOCUMENT_NULL",
                        projectId,
                        mapping.getId()
                );

                continue;
            }

            if (requiredDocument.getId() == null
                    || requiredDocument.isDeleted()
                    || !requiredDocument.isActive()) {

                logger.debug(
                        "[REQUIRED-DOCUMENT-SKIPPED] " +
                                "projectId={}, requiredDocumentId={}, " +
                                "deleted={}, active={}",
                        projectId,
                        requiredDocument.getId(),
                        requiredDocument.isDeleted(),
                        requiredDocument.isActive()
                );

                continue;
            }

            Long requiredDocumentId =
                    requiredDocument.getId();

            String requiredDocumentName =
                    requiredDocument.getName();

            List<ProjectDocumentUpload> uploads =
                    uploadedMap.get(requiredDocumentId);

            if (uploads == null || uploads.isEmpty()) {

                logger.warn(
                        "[DOCUMENT-VALIDATION-FAILED] " +
                                "reason=DOC_MISSING, projectId={}, " +
                                "requiredDocumentId={}, requiredDocumentName={}",
                        projectId,
                        requiredDocumentId,
                        requiredDocumentName
                );

                throw new ValidationException(
                        "Mandatory document missing: "
                                + requiredDocumentName,
                        "DOC_MISSING"
                );
            }

            logger.debug(
                    "[REQUIRED-DOCUMENT-VALIDATION-START] " +
                            "projectId={}, requiredDocumentId={}, " +
                            "requiredDocumentName={}, uploadCount={}",
                    projectId,
                    requiredDocumentId,
                    requiredDocumentName,
                    uploads.size()
            );

            boolean validDocumentFound = false;

            for (ProjectDocumentUpload upload : uploads) {

                if (upload == null || upload.isDeleted()) {

                    logger.debug(
                            "[DOCUMENT-UPLOAD-SKIPPED] " +
                                    "projectId={}, requiredDocumentId={}, " +
                                    "reason=NULL_OR_DELETED",
                            projectId,
                            requiredDocumentId
                    );

                    continue;
                }

                Long uploadId = upload.getId();

                /*
                 * Legal Verification requires VERIFIED status.
                 */
                if (requireVerification) {

                    String statusName =
                            upload.getStatus() != null
                                    ? upload.getStatus().getName()
                                    : null;

                    if (statusName == null
                            || !"VERIFIED".equalsIgnoreCase(statusName)) {

                        logger.debug(
                                "[DOCUMENT-UPLOAD-SKIPPED] " +
                                        "projectId={}, uploadId={}, " +
                                        "requiredDocumentId={}, " +
                                        "reason=NOT_VERIFIED, status={}",
                                projectId,
                                uploadId,
                                requiredDocumentId,
                                statusName
                        );

                        continue;
                    }
                }

                /*
                 * Validate expiry only when expiry configuration exists.
                 */
                if (requiredDocument.getExpiryType() != null
                        && requiredDocument.getExpiryType()
                        != DocumentExpiryType.UNKNOWN) {

                    /*
                     * Permanent documents do not require an expiry date.
                     */
                    if (!upload.isPermanent()) {

                        if (upload.getExpiryDate() == null) {

                            logger.warn(
                                    "[DOCUMENT-VALIDATION-FAILED] " +
                                            "reason=DOC_EXPIRY_MISSING, " +
                                            "projectId={}, uploadId={}, " +
                                            "requiredDocumentId={}, " +
                                            "requiredDocumentName={}",
                                    projectId,
                                    uploadId,
                                    requiredDocumentId,
                                    requiredDocumentName
                            );

                            throw new ValidationException(
                                    "Expiry date missing for document: "
                                            + requiredDocumentName,
                                    "DOC_EXPIRY_MISSING"
                            );
                        }

                        LocalDate expiryDate =
                                convertToLocalDate(
                                        upload.getExpiryDate()
                                );

                        logger.debug(
                                "[DOCUMENT-EXPIRY-CHECK] " +
                                        "projectId={}, uploadId={}, " +
                                        "requiredDocumentId={}, " +
                                        "expiryDate={}, currentDate={}, " +
                                        "storedExpiredFlag={}",
                                projectId,
                                uploadId,
                                requiredDocumentId,
                                expiryDate,
                                today,
                                upload.isExpired()
                        );

                        /*
                         * A document remains valid throughout its expiry date.
                         *
                         * expiryDate = 2026-07-31
                         * today      = 2026-07-31
                         * Result     = VALID
                         *
                         * It becomes expired from 2026-08-01.
                         *
                         * Do not include upload.isExpired() in this condition
                         * because an existing scheduler may have set that flag
                         * at midnight on the expiry date.
                         */
                        if (expiryDate.isBefore(today)) {

                            logger.warn(
                                    "[DOCUMENT-VALIDATION-FAILED] " +
                                            "reason=DOC_EXPIRED, " +
                                            "projectId={}, uploadId={}, " +
                                            "requiredDocumentId={}, " +
                                            "requiredDocumentName={}, " +
                                            "expiryDate={}, currentDate={}",
                                    projectId,
                                    uploadId,
                                    requiredDocumentId,
                                    requiredDocumentName,
                                    expiryDate,
                                    today
                            );

                            throw new ValidationException(
                                    "Document expired: "
                                            + requiredDocumentName,
                                    "DOC_EXPIRED"
                            );
                        }
                    }
                }

                validDocumentFound = true;

                logger.debug(
                        "[VALID-DOCUMENT-FOUND] " +
                                "projectId={}, uploadId={}, " +
                                "requiredDocumentId={}, " +
                                "requiredDocumentName={}",
                        projectId,
                        upload.getId(),
                        requiredDocumentId,
                        requiredDocumentName
                );

                break;
            }

            if (!validDocumentFound) {

                logger.warn(
                        "[DOCUMENT-VALIDATION-FAILED] " +
                                "reason=DOC_INVALID, projectId={}, " +
                                "requiredDocumentId={}, requiredDocumentName={}, " +
                                "requireVerification={}",
                        projectId,
                        requiredDocumentId,
                        requiredDocumentName,
                        requireVerification
                );

                throw new ValidationException(
                        "No valid document found for: "
                                + requiredDocumentName,
                        "DOC_INVALID"
                );
            }

            logger.debug(
                    "[REQUIRED-DOCUMENT-VALIDATION-SUCCESS] " +
                            "projectId={}, requiredDocumentId={}, " +
                            "requiredDocumentName={}",
                    projectId,
                    requiredDocumentId,
                    requiredDocumentName
            );
        }

        logger.info(
                "[DOCUMENT-VALIDATION-SUCCESS] " +
                        "assignmentId={}, projectId={}, " +
                        "requireVerification={}",
                assignmentId,
                projectId,
                requireVerification
        );
    }

    /**
     * Converts java.sql.Date and java.util.Date into LocalDate.
     */
    private LocalDate convertToLocalDate(Date date) {

        if (date == null) {
            return null;
        }

        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        return date.toInstant()
                .atZone(BUSINESS_ZONE)
                .toLocalDate();
    }
}