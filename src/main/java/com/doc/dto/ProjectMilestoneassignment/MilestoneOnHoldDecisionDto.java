package com.doc.dto.ProjectMilestoneassignment;

import com.doc.entity.milestone.MilestoneOnHoldDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MilestoneOnHoldDecisionDto {

    @NotNull(message = "Manager user ID is required")
    private Long managerId;

    @NotNull(message = "Decision is required")
    private MilestoneOnHoldDecision decision;

    @Size(max = 1000, message = "Decision reason cannot exceed 1000 characters")
    private String decisionReason;
}
