package com.doc.dto.project.projectHistory;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class MilestoneHistoryResponseDto {

    private Long milestoneAssignmentId;
    private String milestoneName;
    private Integer order;

    private Date createdDate;
    private Long createdBy;
    private String createdByName;

    private String currentStatus;
    private String currentStatusReason;
    private Date visibleDate;
    private Date startedDate;
    private Date completedDate;
    private String visibilityReason;
    private boolean isVisible;
    private int reworkAttempts;

    private Long currentAssignedUserId;
    private String currentAssignedUserName;

    private List<AssignmentEventDto> assignmentEvents;
    private List<StatusChangeEventDto> statusChangeEvents;
}