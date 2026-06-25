package com.doc.dto.project.reopen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectReopenDecisionDto {

    @NotNull(message = "Action by user ID is required")
    private Long actionById;

    @NotBlank(message = "Decision is required")
    private String decision; // APPROVE or REJECT

    private String remarks;
}