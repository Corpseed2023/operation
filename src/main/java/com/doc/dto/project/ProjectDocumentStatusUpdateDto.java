package com.doc.dto.project;

import com.doc.entity.project.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectDocumentStatusUpdateDto {

    @NotNull(message = "New status cannot be null")
    private DocumentStatus newStatus;

    private String remarks;

    @NotNull(message = "Changed by user ID cannot be null")
    private Long changedById;
}