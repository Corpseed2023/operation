package com.doc.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDocumentUploadRequestDto {

    @NotNull(message = "Project ID cannot be null")
    private Long projectId;

    @NotNull(message = "Milestone assignment ID cannot be null")
    private Long milestoneAssignmentId;

    @NotNull(message = "Required document id cannot be null")
    private Long requiredDocumentId;

    @NotBlank(message = "File name cannot be empty")
    private String fileName;

    @NotNull(message = "Uploaded by user ID cannot be null")
    private Long uploadedById;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdById;
}