package com.doc.dto.lead;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificationRenewalLeadResponseDto {

    /*
     * Newly created Lead ID or existing Lead ID
     * when Lead Service detects an idempotent retry.
     */
    private Long leadId;

    /*
     * RD user assigned by Lead Service.
     */
    private Long assignedUserId;

    private String assignedUserName;

    /*
     * Should be RD.
     */
    private String assignmentWorkFunction;

    /*
     * CREATED / EXISTING / FAILED etc.
     */
    private String status;

    /*
     * true if this request was already processed
     * by Lead Service earlier.
     */
    private boolean existingLead;
}