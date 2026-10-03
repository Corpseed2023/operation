package com.doc.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCancellationEligibilityDto {

    private Long projectId;
    private String projectNo;
    private String projectStatus;

    private int milestoneCompletionPercentage;
    private long totalMilestones;
    private long completedMilestones;
    private String unbilledNumber;

    private boolean certificationMilestonePresent;
    private boolean certificationCompleted;

    /** false when the project is 100% complete or Certification is completed */
    private boolean cancellationAllowed;

    /** Human-readable reason, null when cancellation is allowed */
    private String blockReason;
}