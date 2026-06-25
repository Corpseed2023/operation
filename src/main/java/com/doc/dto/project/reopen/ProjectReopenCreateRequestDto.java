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
     */
    @NotNull(message = "Detected at assignment ID is required")
    private Long detectedAtAssignmentId;

    /*
     * Milestone responsible for mistake.
     * Example: Filing / Technical assignment ID.
     */
    @NotNull(message = "Responsible assignment ID is required")
    private Long responsibleAssignmentId;

    @NotNull(message = "Requested by user ID is required")
    private Long requestedById;

    /*
     * First approval manager.
     * For now frontend sends this.
     * Later we can auto-detect by department.
     */
    @NotNull(message = "Requester manager ID is required")
    private Long requesterManagerId;

    /*
     * Second approval manager.
     * Example: Technical Manager.
     */
    @NotNull(message = "Responsible manager ID is required")
    private Long responsibleManagerId;

    @NotBlank(message = "Reopen reason is required")
    private String reason;
}