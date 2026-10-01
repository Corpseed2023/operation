package com.doc.dto.lead;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledCertificationRenewalLeadRequestDto {

    private Long projectId;

    private String projectNo;

    private Long originalLeadId;

    private Long solutionId;

    private Long companyId;

    private String companyName;

    private Long contactId;

    private String contactName;

    private String contactEmail;

    private String contactMobile;

    private Long milestoneAssignmentId;

    private String sourceReferenceType;

    private Long sourceReferenceId;

    private LocalDate certificateIssueDate;

    private LocalDate certificateExpiryDate;

    private LocalDate renewalDueDate;

    private String source;

    private String targetWorkFunction;
}