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
import com.doc.repository.*;
import com.doc.repository.documentRepo.ProjectDocumentUploadRepository;
import com.doc.repository.projectRepo.ProjectPortalDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MilestoneValidator {
    @Autowired private ProjectDocumentUploadRepository projectDocumentUploadRepository;
    @Autowired private ProductDocumentMappingRepository productDocumentMappingRepository;
    @Autowired
    private ProjectPortalDetailRepository portalDetailRepository;



    public void validateDocumentMilestone(ProjectMilestoneAssignment assignment) {
        validateDocuments(assignment, false); // verification NOT required
    }

    public void validateLegalMilestone(ProjectMilestoneAssignment assignment) {
        validateDocuments(assignment, true); // verification required
    }

    public void validateFillingMilestone(ProjectMilestoneAssignment assignment) {

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

        // Fetch all active (not deleted) portal details
        List<ProjectPortalDetail> portalDetails =
                portalDetailRepository
                        .findByProjectIdAndIsDeletedFalse(project.getId());

        if (portalDetails == null || portalDetails.isEmpty()) {
            throw new ValidationException(
                    "Cannot complete Filing milestone. No portal details have been added for this project.",
                    "PORTAL_DETAILS_MISSING"
            );
        }

        // Optional but recommended: require at least one APPROVED entry
        boolean hasApprovedPortal = portalDetails.stream()
                .anyMatch(p -> "APPROVED".equalsIgnoreCase(p.getStatus()));

        if (!hasApprovedPortal) {
            throw new ValidationException(
                    "Cannot complete Filing milestone. At least one portal detail must be APPROVED.",
                    "PORTAL_NOT_APPROVED"
            );
        }

        // ✅ Filing milestone validation passed
    }



    private void validateDocuments(ProjectMilestoneAssignment assignment, boolean requireVerification) {

        if (assignment == null || assignment.getProject() == null) {
            throw new ValidationException(
                    "Invalid milestone assignment.",
                    "INVALID_ASSIGNMENT"
            );
        }

        Project project = assignment.getProject();

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
                        .findByProductAndApplicantType(product, applicantType);

        if (requiredMappings == null || requiredMappings.isEmpty()) {
            throw new ValidationException(
                    "No document configuration found for this product and applicant type.",
                    "DOC_MAPPING_MISSING"
            );
        }

        List<ProjectDocumentUpload> uploadedDocuments =
                projectDocumentUploadRepository
                        .findByProjectIdAndIsDeletedFalse(project.getId());

        if (uploadedDocuments == null) {
            uploadedDocuments = List.of();
        }

        Map<Long, List<ProjectDocumentUpload>> uploadedMap =
                uploadedDocuments.stream()
                        .filter(u -> u.getRequiredDocument() != null)
                        .collect(Collectors.groupingBy(
                                u -> u.getRequiredDocument().getId()
                        ));

        for (ProductDocumentMapping mapping : requiredMappings) {

            if (!mapping.isMandatory() || !mapping.isActive()) {
                continue;
            }

            ProductRequiredDocuments requiredDoc = mapping.getRequiredDocument();

            if (requiredDoc == null || requiredDoc.isDeleted() || !requiredDoc.isActive()) {
                continue;
            }

            List<ProjectDocumentUpload> uploads =
                    uploadedMap.get(requiredDoc.getId());

            if (uploads == null || uploads.isEmpty()) {
                throw new ValidationException(
                        "Mandatory document missing: " + requiredDoc.getName(),
                        "DOC_MISSING"
                );
            }

            boolean validFound = false;

            for (ProjectDocumentUpload upload : uploads) {

                if (upload == null || upload.isDeleted()) continue;

                // 🔥 Verification check only when required
                if (requireVerification) {
                    if (upload.getStatus() == null ||
                            !"VERIFIED".equalsIgnoreCase(upload.getStatus().getName())) {
                        continue;
                    }
                }

                if (requiredDoc.getExpiryType() != null &&
                        requiredDoc.getExpiryType() != com.doc.em.DocumentExpiryType.UNKNOWN) {

                    if (!upload.isPermanent()) {

                        if (upload.getExpiryDate() == null) {
                            throw new ValidationException(
                                    "Expiry date missing for document: " + requiredDoc.getName(),
                                    "DOC_EXPIRY_MISSING"
                            );
                        }

                        Date today = new Date();

                        if (upload.getExpiryDate().before(today) || upload.isExpired()) {
                            throw new ValidationException(
                                    "Document expired: " + requiredDoc.getName(),
                                    "DOC_EXPIRED"
                            );
                        }
                    }
                }

                validFound = true;
                break;
            }

            if (!validFound) {
                throw new ValidationException(
                        "No valid document found for: " + requiredDoc.getName(),
                        "DOC_INVALID"
                );
            }
        }
    }


}
