package com.doc.dto.project.projectHistory;


import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class MilestoneHistoryDto {

    private Long milestoneId;
    private String milestoneName;
    private int order;
    private Date assignmentCreatedDate;
    private List<AssignmentEventDto> assignmentEvents;
    private List<StatusChangeEventDto> statusChangeEvents;

}