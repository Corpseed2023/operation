
package com.doc.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectDocumentStatusUpdateDto {

    @NotBlank(message = "New status name is required")
    private String newStatus; // e.g., "VERIFIED", "REJECTED"

    private String remarks; // REQUIRED when status = REJECTED

    @NotNull(message = "Changed by user ID is required")
    private Long changedById;
}