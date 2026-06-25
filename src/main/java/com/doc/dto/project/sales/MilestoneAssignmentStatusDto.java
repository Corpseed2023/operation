package com.doc.dto.project.sales;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneAssignmentStatusDto {

    private Long assignmentId;

    private Long milestoneId;
    private String milestoneName;
    private Integer milestoneOrder;

    private Long milestoneStatusId;
    private String milestoneStatusName;
    private String statusReason;

    private Boolean visible;
    private String visibilityReason;

    private Long assignedUserId;
    private String assignedUserName;
    private String assignedUserEmail;
    private String assignedUserMobile;

    private Date visibleDate;
    private Date startedDate;
    private Date completedDate;

    private Integer reworkAttempts;
}