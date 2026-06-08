package com.doc.dto.LegalRequestDto;

import lombok.Getter;
import lombok.Setter;
import org.aspectj.apache.bcel.generic.LineNumberGen;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
public class LegalRequestDto {

    private Long projectId;
    private Long projectMilestoneAssignmentId;
    private String status;
    private Long milestoneAssigneeId;
    private String statusReason;
    private String legalRequestTitle;
    private Long assignedToLegal;
    private Long createdById;

    private String notes;

    private List<String> documents;

}
