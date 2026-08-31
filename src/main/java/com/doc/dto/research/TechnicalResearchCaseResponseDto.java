package com.doc.dto.research;

import com.doc.entity.research.ResearchPriority;
import com.doc.entity.research.TechnicalResearchCaseStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TechnicalResearchCaseResponseDto {

    private Long id;
    private String caseNumber;

    /*
     * Origin references
     */
    private Long originatingLeadId;

    /*
     * Product information
     */
    private Long productId;
    private String productName;

    /*
     * Research requirement
     */
    private String subject;
    private String businessContext;
    private String researchScope;

    /*
     * Salesperson who raised the case
     */
    private Long raisedByUserId;
    private String raisedByName;

    /*
     * Current technical assignee
     */
    private Long currentAssigneeUserId;
    private String currentAssigneeName;

    /*
     * Manager who performed the latest assignment
     */
    private Long lastAssignedByUserId;
    private String lastAssignedByName;

    /*
     * Workflow information
     */
    private TechnicalResearchCaseStatus status;
    private ResearchPriority priority;
    private LocalDate dueDate;

    /*
     * Assignment tracking
     */
    private Instant firstAssignedAt;
    private Instant lastAssignedAt;
    private Integer assignmentCount;

    /*
     * Research execution
     */
    private Instant workStartedAt;
    private String findings;
    private String recommendation;
    private Instant submittedAt;

    /*
     * Closure information
     */
    private Instant closedAt;
    private Long closedByUserId;
    private String closedByName;
    private String closureReason;

    /*
     * Audit timestamps
     */
    private Instant createdAt;
    private Instant updatedAt;
}