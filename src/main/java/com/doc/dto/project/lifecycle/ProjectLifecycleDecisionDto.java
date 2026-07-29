package com.doc.dto.project.lifecycle;

import com.doc.entity.project.ProjectLifecycleDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectLifecycleDecisionDto {

    @NotNull(message = "Decision is required")
    private ProjectLifecycleDecision decision;

    @NotNull(message = "Reviewed by user ID is required")
    private Long reviewedById;

    @Size(
            max = 2000,
            message = "Review remark cannot exceed 2000 characters"
    )
    private String reviewRemark;
}