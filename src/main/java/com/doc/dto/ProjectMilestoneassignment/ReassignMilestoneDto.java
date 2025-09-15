package com.doc.dto.ProjectMilestoneassignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReassignMilestoneDto {
    @NotNull(message = "Assignment ID cannot be null")
    private Long assignmentId;

    @NotNull(message = "New user ID cannot be null")
    private Long newUserId;

    @NotBlank(message = "Reassignment reason cannot be blank")
    private String reassignmentReason;

    @NotNull(message = "Changed by user ID cannot be null")
    private Long changedById;
}