package com.doc.dto.project.reopen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectReopenCreateRequestDto {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    /*
     * Milestone where issue was found.
     * Example: Liaison / Certification assignment ID.
     *
     * Backend will also use this assignment's assignedUser as requestedBy.
     */
    @NotNull(message = "Detected at assignment ID is required")
    private Long detectedAtAssignmentId;

    /*
     * Milestone responsible for mistake.
     * Example: Filing / Technical assignment ID.
     */
    @NotNull(message = "Responsible assignment ID is required")
    private Long responsibleAssignmentId;

    @NotBlank(message = "Reopen reason is required")
    private String reason;
}