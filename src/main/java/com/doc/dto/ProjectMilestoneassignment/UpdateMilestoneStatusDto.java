package com.doc.dto.ProjectMilestoneassignment;

import com.doc.entity.project.MilestoneStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
public class UpdateMilestoneStatusDto {
    @NotNull(message = "Assignment ID cannot be null")
    private Long assignmentId;

    @NotNull(message = "New status cannot be null")
    private MilestoneStatus newStatus;

    @NotBlank(message = "Status reason cannot be blank when required")
    private String statusReason;

    @NotNull(message = "Changed by user ID cannot be null")
    private Long changedById;
}