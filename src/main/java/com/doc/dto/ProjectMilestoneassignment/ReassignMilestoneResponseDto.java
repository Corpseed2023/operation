package com.doc.dto.ProjectMilestoneassignment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReassignMilestoneResponseDto {
    private Long assignmentId;
    private Long newUserId;
    private String newUserName;
    private String newUserEmail;
    private String milestoneName;
    private Long projectId;
    private String reassignmentReason;
}