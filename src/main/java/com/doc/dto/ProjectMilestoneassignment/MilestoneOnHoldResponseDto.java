package com.doc.dto.ProjectMilestoneassignment;

import com.doc.entity.milestone.MilestoneOnHoldApprovalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MilestoneOnHoldResponseDto {
    private Long requestId;
    private Long assignmentId;
    private Long projectId;
    private String projectNumber;
    private String projectName;
    private String milestoneName;
    private String currentMilestoneStatus;
    private Long requestedById;
    private String requestedByName;
    private Long managerId;
    private String managerName;
    private MilestoneOnHoldApprovalStatus approvalStatus;
    private String requestReason;
    private String decisionReason;
    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;
}
