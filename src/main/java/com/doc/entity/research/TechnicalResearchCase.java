package com.doc.entity.research;

import com.doc.entity.product.Product;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "technical_research_cases",
        indexes = {
                @Index(
                        name = "uk_research_case_number",
                        columnList = "case_number",
                        unique = true
                ),
                @Index(
                        name = "idx_research_case_product",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_research_case_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_research_case_raised_by",
                        columnList = "raised_by_user_id"
                ),
                @Index(
                        name = "idx_research_case_assignee",
                        columnList = "current_assignee_user_id"
                ),
                @Index(
                        name = "idx_research_case_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TechnicalResearchCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * System-generated business identifier.
     *
     * Example: TRC-20260831-0001
     */
    @Column(
            name = "case_number",
            nullable = false,
            unique = true,
            updatable = false,
            length = 50
    )
    @Comment("Unique technical research case number")
    private String caseNumber;

    /**
     * Reference to the originating lead.
     *
     * This is not a JPA relationship because Lead belongs
     * to another microservice.
     */
    @Column(name = "originating_lead_id")
    @Comment("Lead Service identifier from which the case originated")
    private Long originatingLeadId;

//    /**
//     * Reference to the originating solution.
//     *
//     * This is not a JPA relationship because Solution belongs
//     * to Lead Service.
//     */
//    @Column(name = "originating_solution_id")
//    @Comment("Lead Service solution identifier")
//    private Long originatingSolutionId;

//    /**
//     * Snapshot retained for display and historical reporting.
//     */
//    @Column(name = "solution_name_snapshot", length = 255)
//    @Comment("Solution name captured when the case was created")
//    private String solutionNameSnapshot;

    /**
     * Product belongs to Operation Service and can therefore
     * be mapped through JPA.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @Comment("Operation product associated with the research case")
    private Product product;

    /**
     * Clear, short subject of the research.
     *
     * Example: FSSAI Central Licence Eligibility Assessment
     */
    @Column(name = "subject", nullable = false, length = 500)
    @Comment("Subject of the technical research")
    private String subject;

    /**
     * Business background provided by the salesperson.
     */
    @Lob
    @Column(name = "business_context", columnDefinition = "TEXT")
    @Comment("Customer and business context for the research")
    private String businessContext;

    /**
     * Exact areas that the technical team must research.
     */
    @Lob
    @Column(name = "research_scope", columnDefinition = "TEXT")
    @Comment("Scope and questions to be covered during research")
    private String researchScope;


    /**
     * Salesperson who raised the research case.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raised_by_user_id", nullable = false)
    @Comment("Salesperson who raised the technical research case")
    private User raisedBy;

    /**
     * Current technical person responsible for the case.
     *
     * Null until a manager assigns the case.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_assignee_user_id")
    @Comment("Technical user currently responsible for the case")
    private User currentAssignee;

    /**
     * Manager who performed the latest assignment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_assigned_by_user_id")
    @Comment("Manager who performed the latest assignment")
    private User lastAssignedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Comment("Current lifecycle status of the research case")
    private TechnicalResearchCaseStatus status =
            TechnicalResearchCaseStatus.PENDING_ASSIGNMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Comment("Business priority of the research case")
    private ResearchPriority priority = ResearchPriority.MEDIUM;

    /**
     * Business due date selected by sales or the manager.
     */
    @Column(name = "due_date")
    @Comment("Expected research completion date")
    private LocalDate dueDate;

    /**
     * Time when the case was assigned for the first time.
     *
     * This value must never be changed by reassignment.
     */
    @Column(name = "first_assigned_at")
    @Comment("UTC time of the first assignment")
    private Instant firstAssignedAt;

    /**
     * Time of the most recent assignment or reassignment.
     */
    @Column(name = "last_assigned_at")
    @Comment("UTC time of the most recent assignment")
    private Instant lastAssignedAt;

    /**
     * Total assignment count.
     *
     * First assignment: 1
     * First reassignment: 2
     */
    @Column(name = "assignment_count", nullable = false)
    @Comment("Total assignments, including reassignments")
    private Integer assignmentCount = 0;

    /**
     * Time when technical work actually started.
     */
    @Column(name = "work_started_at")
    @Comment("UTC time when technical research work started")
    private Instant workStartedAt;

    /**
     * Findings prepared by the technical person.
     */
    @Lob
    @Column(name = "findings", columnDefinition = "TEXT")
    @Comment("Technical research findings")
    private String findings;

    /**
     * Final recommendation based on the research.
     */
    @Lob
    @Column(name = "recommendation", columnDefinition = "TEXT")
    @Comment("Final technical recommendation")
    private String recommendation;

    /**
     * Time when the technical person submitted the work.
     */
    @Column(name = "submitted_at")
    @Comment("UTC time when research was submitted for review")
    private Instant submittedAt;

    /**
     * Time when the case reached its final state.
     */
    @Column(name = "closed_at")
    @Comment("UTC time when the case was completed, rejected or cancelled")
    private Instant closedAt;

    /**
     * User who completed, rejected or cancelled the case.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_user_id")
    @Comment("User who closed the technical research case")
    private User closedBy;

    /**
     * Required when the case is rejected or cancelled.
     */
    @Column(name = "closure_reason", length = 2000)
    @Comment("Reason for rejecting or cancelling the case")
    private String closureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("UTC time when the record was created")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Comment("UTC time when the record was last updated")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    @Comment("User who created the database record")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    @Comment("User who last updated the database record")
    private User updatedBy;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft-delete indicator")
    private boolean deleted = false;

    /**
     * Prevents conflicting assignments or status changes.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status =
                    TechnicalResearchCaseStatus.PENDING_ASSIGNMENT;
        }

        if (this.priority == null) {
            this.priority = ResearchPriority.MEDIUM;
        }

        if (this.assignmentCount == null) {
            this.assignmentCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}