package com.doc.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO for uploading a project document.
 */
@Getter
@Setter
public class ProjectDocumentUploadRequestDto {

    @NotNull(message = "Project ID cannot be null")
    private Long projectId;

    @NotNull(message = "Milestone assignment ID cannot be null")
    private Long milestoneAssignmentId;

    @NotNull(message = "Required document UUID cannot be null")
    private UUID requiredDocumentId;

    @NotBlank(message = "File URL cannot be empty")
    private String fileUrl;

    @NotNull(message = "Uploaded by user ID cannot be null")
    private Long uploadedById;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdById;
}