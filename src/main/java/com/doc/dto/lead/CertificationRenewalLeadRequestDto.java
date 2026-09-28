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
public class CertificationRenewalLeadRequestDto {

    /*
     * Operation Project.
     */
    private Long projectId;

    private String projectNo;

    /*
     * Lead from which this Project was originally created.
     */
    private Long originalLeadId;

    /*
     * Operation Product ID corresponds to
     * Lead Service Solution ID in your existing flow.
     */
    private Long solutionId;

    /*
     * Account/Company reference.
     */
    private Long companyId;

    private String companyName;

    /*
     * Project contact.
     */
    private Long contactId;

    private String contactName;

    private String contactEmail;

    private String contactMobile;

    /*
     * Certification milestone assignment.
     *
     * This becomes the unique renewal reference.
     */
    private Long milestoneAssignmentId;

    /*
     * Explicit idempotency reference.
     */
    private String sourceReferenceType;

    private Long sourceReferenceId;

    /*
     * Certificate data.
     */
    private LocalDate certificateIssueDate;

    private LocalDate certificateExpiryDate;

    private LocalDate renewalDueDate;

    /*
     * Always:
     *
     * CERTIFICATE_RENEWAL
     */
    private String source;

    /*
     * Lead Service must assign only
     * to this work function.
     *
     * RD
     */
    private String targetWorkFunction;
}