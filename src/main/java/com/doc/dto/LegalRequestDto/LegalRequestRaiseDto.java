package com.doc.dto.LegalRequestDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LegalRequestRaiseDto {

    private Long projectMilestoneAssignmentId;

    private String legalRequestTitle;

    private Long assignedToLegal;

    private Long createdById;

    private String notes;
}
