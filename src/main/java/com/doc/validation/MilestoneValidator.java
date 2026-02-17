package com.doc.validation;


import com.doc.entity.document.ApplicantType;
import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.product.Product;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.exception.ValidationException;
import com.doc.repository.*;
import com.doc.repository.documentRepo.ProductRequiredDocumentRepository;
import com.doc.repository.documentRepo.ProjectDocumentUploadRepository;
import com.doc.repository.projectRepo.ProjectStatusRepository;
import com.doc.service.ProjectService;
import jakarta.annotation.Nonnull;
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



    public void validateDocumentMilestone(ProjectMilestoneAssignment assignment) {

        if (assignment == null || assignment.getProject() == null) {
            throw new ValidationException(
                    "Invalid milestone assignment.",
                    "INVALID_ASSIGNMENT"
            );
        }

        Project project = assignment.getProject();

        // 1️⃣ Applicant Type must be selected
        if (project.getApplicantType() == null) {
            throw new ValidationException(
                    "Applicant Type must be selected before completing Document milestone.",
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

        // 2️⃣ Fetch Product + ApplicantType document mappings (source of truth)
        List<ProductDocumentMapping> requiredMappings =
                productDocumentMappingRepository
                        .findByProductAndApplicantType(product, applicantType);

        if (requiredMappings == null || requiredMappings.isEmpty()) {
            throw new ValidationException(
                    "No document configuration found for this product and applicant type.",
                    "DOC_MAPPING_MISSING"
            );
        }

        // 3️⃣ Fetch all non-deleted uploads for this project
        List<ProjectDocumentUpload> uploadedDocuments =
                projectDocumentUploadRepository
                        .findByProjectIdAndIsDeletedFalse(project.getId());

        if (uploadedDocuments == null) {
            uploadedDocuments = List.of();
        }

        // Group uploads by requiredDocumentId
        Map<Long, List<ProjectDocumentUpload>> uploadedMap =
                uploadedDocuments.stream()
                        .filter(u -> u.getRequiredDocument() != null)
                        .collect(Collectors.groupingBy(
                                u -> u.getRequiredDocument().getId()
                        ));

        // 4️⃣ Validate each mandatory & active mapping
        for (ProductDocumentMapping mapping : requiredMappings) {

            if (!mapping.isMandatory() || !mapping.isActive()) {
                continue; // Skip optional or inactive documents
            }

            ProductRequiredDocuments requiredDoc = mapping.getRequiredDocument();

            if (requiredDoc == null || requiredDoc.isDeleted() || !requiredDoc.isActive()) {
                continue; // Skip invalid master document definitions
            }

            List<ProjectDocumentUpload> uploads =
                    uploadedMap.get(requiredDoc.getId());

            // ❌ No upload found
            if (uploads == null || uploads.isEmpty()) {
                throw new ValidationException(
                        "Mandatory document missing: " + requiredDoc.getName(),
                        "DOC_MISSING"
                );
            }

            boolean validFound = false;

            for (ProjectDocumentUpload upload : uploads) {

                if (upload == null) continue;
                if (upload.isDeleted()) continue;

                // ------------------------------------------------------------
                // 🚫 VERIFICATION CHECK DISABLED (AS REQUESTED)
                // ------------------------------------------------------------
                // If you want to enforce verification, uncomment below:
                //
                // if (!"VERIFIED".equalsIgnoreCase(upload.getStatus().getName())) {
                //     continue;
                // }
                // ------------------------------------------------------------

                // 5️⃣ Expiry validation (only if document requires expiry)
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

                // If we reach here → document is acceptable
                validFound = true;
                break;
            }

            // ❌ No valid upload found
            if (!validFound) {
                throw new ValidationException(
                        "No valid document found for: " + requiredDoc.getName(),
                        "DOC_INVALID"
                );
            }
        }

        // ✅ All mandatory documents satisfied
    }

    public void validateLegalMilestone() { }

    public void validateFillingMilestone() { }

    public void validateLiasioningMilestone() { }

    public void validateCertificationMilestone() { }

}
