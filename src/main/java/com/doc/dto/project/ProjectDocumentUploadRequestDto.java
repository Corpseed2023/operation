// src/main/java/com/doc/dto/project/ProjectDocumentUploadRequestDto.java

package com.doc.dto.project;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectDocumentUploadRequestDto {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "Milestone assignment ID is required")
    private Long milestoneAssignmentId;

    @NotNull(message = "Required document ID is required")
    private Long requiredDocumentId;

    @NotBlank(message = "File name cannot be empty")
    @Size(max = 255, message = "File name cannot exceed 255 characters")
    private String fileName;

    @NotNull(message = "Uploaded by user ID is required")
    private Long uploadedById;

    @NotNull(message = "Created by user ID is required")
    private Long createdById;

    // Optional: for reuse from company documents
    private Long companyDocSourceId;

    private Boolean isFromCompanyDoc = false;

    // Optional: override expiry (for EXPIRING docs)
    private String expiryDate; // ISO format: "2027-12-31"

    private Boolean isPermanent = false;

    @Min(value = 1, message = "File size must be greater than 0")
    private Integer fileSizeKb;

    @NotBlank(message = "File format is required")
    @Pattern(regexp = "pdf|jpg|jpeg|png", message = "Only pdf, jpg, jpeg, png allowed")
    private String fileFormat;

    // Optional: initial remarks
    private String remarks;
}