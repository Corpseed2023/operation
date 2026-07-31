package com.doc.validation;

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

    /*
     * Use the application's business timezone explicitly.
     * This prevents expiry checks from changing according to the
     * server's default timezone.
     */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private ProjectDocumentUploadRepository projectDocumentUploadRepository;

    @Autowired
    private ProductDocumentMappingRepository productDocumentMappingRepository;

    @Autowired
    private ProjectPortalDetailRepository portalDetailRepository;

    /**
     * Documentation milestone:
     * Documents must be uploaded and valid.
     * Verification is not mandatory at this stage.
     */
    public void validateDocumentMilestone(
            ProjectMilestoneAssignment assignment
    ) {
        validateDocuments(assignment, false);
    }

    /**
     * Legal Verification milestone:
     * Documents must be uploaded, valid and VERIFIED.
     */
    public void validateLegalMilestone(
            ProjectMilestoneAssignment assignment
    ) {
        validateDocuments(assignment, true);
    }

    /**
     * Filing milestone:
     * At least one approved portal detail must exist.
     */
    public void validateFillingMilestone(
            ProjectMilestoneAssignment assignment
    ) {

        if (assignment == null || assignment.getProject() == null) {
            throw new ValidationException(
                    "Invalid milestone assignment.",
                    "INVALID_ASSIGNMENT"
            );
        }

        Project project = assignment.getProject();

        if (project.getId() == null) {
            throw new ValidationException(
                    "Project not found for milestone validation.",
                    "PROJECT_MISSING"
            );
        }

        List<ProjectPortalDetail> portalDetails =
                portalDetailRepository
                        .findByProjectIdAndIsDeletedFalse(
                                project.getId()
                        );

        if (portalDetails == null || portalDetails.isEmpty()) {
            throw new ValidationException(
                    "Cannot start Filing milestone. " +
                            "No portal details have been added for this project.",
                    "PORTAL_DETAILS_MISSING"
            );
        }

        portalDetails.forEach(portalDetail ->
                System.out.println(
                        "Portal detail: portalName="
                                + portalDetail.getPortalName()
                                + ", status="
                                + portalDetail.getStatus()
                )
        );

        boolean hasApprovedPortal = portalDetails.stream()
                .anyMatch(portalDetail ->
                        portalDetail.getStatus() != null
                                && "APPROVED".equalsIgnoreCase(
                                portalDetail.getStatus()
                        )
                );

        if (!hasApprovedPortal) {
            throw new ValidationException(
                    "Cannot start Filing milestone. " +
                            "At least one portal detail must be APPROVED.",
                    "PORTAL_NOT_APPROVED"
            );
        }
    }

    /**
     * Validates all mandatory documents configured for the project's
     * product and applicant type.
     *
     * @param assignment          milestone assignment being validated
     * @param requireVerification true when VERIFIED status is mandatory
     */
    private void validateDocuments(
            ProjectMilestoneAssignment assignment,
            boolean requireVerification
    ) {

        if (assignment == null || assignment.getProject() == null) {
            throw new ValidationException(
                    "Invalid milestone assignment.",
                    "INVALID_ASSIGNMENT"
            );
        }

        Project project = assignment.getProject();

        if (project.getId() == null) {
            throw new ValidationException(
                    "Project not found for milestone validation.",
                    "PROJECT_MISSING"
            );
        }

        if (project.getApplicantType() == null) {
            throw new ValidationException(
                    "Applicant Type must be selected before completing milestone.",
                    "APPLICANT_TYPE_MISSING"
            );
        }

        Product product = project.getProduct();
        ApplicantType applicantType = project.getApplicantType();

        if (product == null) {
            throw new ValidationException(
                    "Project is not linked to a valid product.",
                    "PRODUCT_MISSING"
            );
        }

        List<ProductDocumentMapping> requiredMappings =
                productDocumentMappingRepository
                        .findByProductAndApplicantType(
                                product,
                                applicantType
                        );

        if (requiredMappings == null || requiredMappings.isEmpty()) {
            throw new ValidationException(
                    "No document configuration found for this product " +
                            "and applicant type.",
                    "DOC_MAPPING_MISSING"
            );
        }

        List<ProjectDocumentUpload> uploadedDocuments =
                projectDocumentUploadRepository
                        .findByProjectIdAndIsDeletedFalse(
                                project.getId()
                        );

        if (uploadedDocuments == null) {
            uploadedDocuments = List.of();
        }

        Map<Long, List<ProjectDocumentUpload>> uploadedMap =
                uploadedDocuments.stream()
                        .filter(upload ->
                                upload != null
                                        && !upload.isDeleted()
                                        && upload.getRequiredDocument() != null
                                        && upload.getRequiredDocument().getId() != null
                        )
                        .collect(
                                Collectors.groupingBy(
                                        upload ->
                                                upload.getRequiredDocument()
                                                        .getId()
                                )
                        );

        for (ProductDocumentMapping mapping : requiredMappings) {

            if (mapping == null
                    || !mapping.isMandatory()
                    || !mapping.isActive()) {
                continue;
            }

            ProductRequiredDocuments requiredDocument =
                    mapping.getRequiredDocument();

            if (requiredDocument == null
                    || requiredDocument.getId() == null
                    || requiredDocument.isDeleted()
                    || !requiredDocument.isActive()) {
                continue;
            }

            List<ProjectDocumentUpload> uploads =
                    uploadedMap.get(requiredDocument.getId());

            if (uploads == null || uploads.isEmpty()) {
                throw new ValidationException(
                        "Mandatory document missing: "
                                + requiredDocument.getName(),
                        "DOC_MISSING"
                );
            }

            boolean validDocumentFound = false;

            for (ProjectDocumentUpload upload : uploads) {

                if (upload == null || upload.isDeleted()) {
                    continue;
                }

                /*
                 * Legal Verification requires VERIFIED status.
                 * Documentation milestone does not require verification.
                 */
                if (requireVerification) {

                    if (upload.getStatus() == null
                            || upload.getStatus().getName() == null
                            || !"VERIFIED".equalsIgnoreCase(
                            upload.getStatus().getName()
                    )) {
                        continue;
                    }
                }

                /*
                 * Validate expiry only when the required document has
                 * an expiry configuration.
                 */
                if (requiredDocument.getExpiryType() != null
                        && requiredDocument.getExpiryType()
                        != com.doc.em.DocumentExpiryType.UNKNOWN) {

                    /*
                     * Permanent documents do not require an expiry date.
                     */
                    if (!upload.isPermanent()) {

                        if (upload.getExpiryDate() == null) {
                            throw new ValidationException(
                                    "Expiry date missing for document: "
                                            + requiredDocument.getName(),
                                    "DOC_EXPIRY_MISSING"
                            );
                        }

                        LocalDate expiryDate =
                                convertToLocalDate(
                                        upload.getExpiryDate()
                                );

                        LocalDate today =
                                LocalDate.now(BUSINESS_ZONE);

                        /*
                         * Important:
                         *
                         * Expiry date: 2026-07-31
                         * Current date: 2026-07-31
                         * Result: VALID
                         *
                         * The document becomes expired on 2026-08-01.
                         *
                         * Do not compare Date objects directly because
                         * @Temporal(TemporalType.DATE) may return:
                         *
                         * 2026-07-31 00:00:00
                         *
                         * That would incorrectly make the document appear
                         * expired during the same day.
                         */
                        if (expiryDate.isBefore(today)) {
                            throw new ValidationException(
                                    "Document expired: "
                                            + requiredDocument.getName(),
                                    "DOC_EXPIRED"
                            );
                        }
                    }
                }

                validDocumentFound = true;
                break;
            }

            if (!validDocumentFound) {
                throw new ValidationException(
                        "No valid document found for: "
                                + requiredDocument.getName(),
                        "DOC_INVALID"
                );
            }
        }
    }

    /**
     * Converts both java.sql.Date and java.util.Date safely.
     *
     * Hibernate may return java.sql.Date for fields mapped with
     * @Temporal(TemporalType.DATE).
     */
    private LocalDate convertToLocalDate(Date date) {

        if (date == null) {
            return null;
        }

        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }

        return date.toInstant()
                .atZone(BUSINESS_ZONE)
                .toLocalDate();
    }
}