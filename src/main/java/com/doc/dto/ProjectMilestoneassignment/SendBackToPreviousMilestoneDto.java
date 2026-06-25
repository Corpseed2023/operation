package com.doc.dto.ProjectMilestoneassignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SendBackToPreviousMilestoneDto {

    private Long currentAssignmentId;

    @NotNull(message = "Changed by user ID is required")
    private Long changedById;

    @NotBlank(message = "Reason is required")
    private String reason;

    /**
     * Documents found wrong by current milestone user.
     * Example: Aadhaar document ID, PAN document ID.
     */
    private List<Long> rejectedDocumentIds;
}