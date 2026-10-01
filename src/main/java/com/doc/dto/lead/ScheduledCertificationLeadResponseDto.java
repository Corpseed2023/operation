package com.doc.dto.lead;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledCertificationLeadResponseDto {

    private Long leadId;

    private String leadName;

    private Long solutionId;

    private String solutionName;

    private Long companyId;

    private String companyName;

    private Long assignedUserId;

    private String assignedUserName;

    private String assignmentWorkFunction;

    private String leadType;

    private boolean existingLead;
}