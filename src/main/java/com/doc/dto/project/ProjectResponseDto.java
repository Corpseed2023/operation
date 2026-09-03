package com.doc.dto.project;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ProjectResponseDto {

    private Long id;
    private String name;
    private String projectNo;
    private String unbilledNumber;
    private String estimateNumber;
    private String priority;

    private Long salesPersonId;
    private String salesPersonName;

    private Long productId;

    private Long companyId;
    private String companyName;

    private Long contactId;
    private String contactName;

    private Long leadId;
    private LocalDate date;

    private Double totalAmount;
    private Double dueAmount;
    private String paymentStatus;
    private Long paymentTypeId;
    private Long approvedById;

    private Date createdDate;
    private Date updatedDate;

    private boolean isDeleted;
    private boolean isActive;

    private Long statusId;
    private String statusName;

    private Long unitId;
    private String unitName;

    private Integer milestoneCompletionPercentage;

    private Boolean poBillingEligible;

    private boolean forceClosed;
    private boolean reopened;
    private boolean lifecycleActionAllowed;

    private Long lastCompletedMilestoneAssignmentId;
    private Long lastCompletedMilestoneId;
    private String lastCompletedMilestoneName;
    private Integer lastCompletedMilestoneOrder;
    private Date lastCompletedMilestoneCompletedDate;

    private Long lastCompletedMilestoneUserId;
    private String lastCompletedMilestoneUserName;
    private String lastCompletedMilestoneUserEmail;
    private String lastCompletedMilestoneUserMobile;

    private Long currentMilestoneAssignmentId;
    private Long currentMilestoneId;
    private String currentMilestoneName;
    private Integer currentMilestoneOrder;
    private String currentMilestoneStatusName;

    private Long currentAssignedUserId;
    private String currentAssignedUserName;
    private String currentAssignedUserEmail;
    private String currentAssignedUserMobile;

    // NEW
    private List<ProjectMilestoneListDto> milestones = new ArrayList<>();
}