package com.doc.entity.product;

import com.doc.entity.milestone.Milestone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.Date;

/**
 * Master configuration that maps a product with its milestones.
 *
 * All TAT, reminder and escalation values are stored in minutes.
 *
 * This table stores template configuration. Actual project-specific due dates
 * should be stored in ProjectMilestoneAssignment.
 */
@Entity
@Table(
        name = "product_milestone_map",
        indexes = {
                @Index(
                        name = "idx_product_id",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_milestone_id",
                        columnList = "milestone_id"
                ),
                @Index(
                        name = "idx_product_step_order",
                        columnList = "product_id, step_order"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_milestone",
                        columnNames = {
                                "product_id",
                                "milestone_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_product_step_order",
                        columnNames = {
                                "product_id",
                                "step_order"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProductMilestoneMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key of product milestone mapping")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    @Comment("Associated product")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "milestone_id",
            nullable = false
    )
    @Comment("Associated milestone")
    private Milestone milestone;

    @Column(name = "step_order", nullable = false)
    @Comment("Execution order of milestone in product workflow")
    private int order;

    // =====================================================================
    // EXECUTION TAT
    // =====================================================================

    @Column(
            name = "execution_tat_applicable",
            nullable = false
    )
    @Comment("Whether execution TAT applies to the assigned employee")
    private boolean executionTatApplicable = true;

    @Column(name = "execution_tat_minutes")
    @Comment("Execution TAT provided to assignee in minutes")
    private Integer executionTatMinutes;

    // =====================================================================
    // DEPARTMENT TAT
    // =====================================================================

    @Column(
            name = "department_tat_applicable",
            nullable = false
    )
    @Comment("Whether department-level TAT is applicable")
    private boolean departmentTatApplicable = false;

    @Column(name = "department_tat_minutes")
    @Comment("Department-level TAT in minutes")
    private Integer departmentTatMinutes;

    // =====================================================================
    // PERFORMANCE TAT
    // =====================================================================

    @Column(
            name = "performance_tat_applicable",
            nullable = false
    )
    @Comment("Whether this milestone affects employee performance")
    private boolean performanceTatApplicable = true;

    @Column(name = "performance_tat_minutes")
    @Comment("Employee performance TAT threshold in minutes")
    private Integer performanceTatMinutes;

    // =====================================================================
    // CUSTOMER / PROJECT SLA TAT
    // =====================================================================

    @Column(
            name = "customer_tat_applicable",
            nullable = false
    )
    @Comment("Whether customer/project SLA TAT is applicable")
    private boolean customerTatApplicable = false;

    @Column(name = "customer_tat_minutes")
    @Comment("Customer/project SLA TAT in minutes")
    private Integer customerTatMinutes;

    // =====================================================================
    // ROLLBACK / REWORK TAT
    // =====================================================================

    @Column(
            name = "rollback_tat_applicable",
            nullable = false
    )
    @Comment("Whether rollback/rework TAT is applicable")
    private boolean rollbackTatApplicable = false;

    @Column(name = "rollback_tat_minutes")
    @Comment("Rollback/rework TAT in minutes")
    private Integer rollbackTatMinutes;

    // =====================================================================
    // WORKFLOW RULES
    // =====================================================================

    @Column(name = "strict_approval", nullable = false)
    @Comment("Whether approval is required before next milestone")
    private boolean strictApproval = false;

    @Column(name = "allow_rollback", nullable = false)
    @Comment("Whether milestone can be sent back for rework")
    private boolean allowRollback = false;

    @Column(name = "max_attempts", nullable = false)
    @Comment("Maximum execution or rework attempts")
    private int maxAttempts = 1;

    @Column(name = "is_mandatory", nullable = false)
    @Comment("Whether milestone is mandatory for project completion")
    private boolean isMandatory = true;

    @Column(name = "payment_percentage", nullable = false)
    @Comment("Payment percentage associated with this milestone")
    private double paymentPercentage;

    @Column(name = "is_auto_generated", nullable = false)
    @Comment("Whether milestone is automatically generated")
    private boolean isAutoGenerated = false;

    @Column(name = "requires_portal_details", nullable = false)
    @Comment("Whether milestone requires client portal details")
    private boolean requiresPortalDetails = false;


    @Column(name = "business_days_enabled", nullable = false)
    @Comment("Whether TAT is calculated using business working time")
    private boolean businessDaysEnabled = false;


    // =====================================================================
    // COMMON FIELDS
    // =====================================================================

    @Column(name = "is_active", nullable = false)
    @Comment("Whether product milestone mapping is active")
    private boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft-delete indicator")
    private boolean isDeleted = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(
            name = "created_date",
            nullable = false,
            updatable = false
    )
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date", nullable = false)
    private Date updatedDate;

    @Column(name = "date")
    private LocalDate date;

    // =====================================================================
    // ENTITY CALLBACKS
    // =====================================================================

    @PrePersist
    public void prePersist() {
        Date now = new Date();

        this.createdDate = now;
        this.updatedDate = now;

        if (this.date == null) {
            this.date = LocalDate.now();
        }

        normalizeFields();
        validateFields();
    }



    private void normalizeFields() {

        if (this.maxAttempts < 1) {
            this.maxAttempts = 1;
        }

        if (!this.executionTatApplicable) {
            this.executionTatMinutes = null;
        }

        if (!this.departmentTatApplicable) {
            this.departmentTatMinutes = null;
        }

        if (!this.performanceTatApplicable) {
            this.performanceTatMinutes = null;
        }

        if (!this.customerTatApplicable) {
            this.customerTatMinutes = null;
        }

        if (!this.allowRollback) {
            this.rollbackTatApplicable = false;
            this.rollbackTatMinutes = null;
        } else if (!this.rollbackTatApplicable) {
            this.rollbackTatMinutes = null;
        }
    }

    // =====================================================================
    // ENTITY VALIDATION
    // =====================================================================

    private void validateFields() {

        if (this.product == null) {
            throw new IllegalStateException("Product cannot be null");
        }

        if (this.milestone == null) {
            throw new IllegalStateException("Milestone cannot be null");
        }

        if (this.order < 1) {
            throw new IllegalStateException(
                    "Step order must be at least 1"
            );
        }

        if (this.executionTatApplicable
                && !isPositive(this.executionTatMinutes)) {
            throw new IllegalStateException(
                    "Execution TAT minutes is required when execution TAT is applicable"
            );
        }

        if (this.departmentTatApplicable
                && !isPositive(this.departmentTatMinutes)) {
            throw new IllegalStateException(
                    "Department TAT minutes is required when department TAT is applicable"
            );
        }

        if (this.performanceTatApplicable
                && !isPositive(this.performanceTatMinutes)) {
            throw new IllegalStateException(
                    "Performance TAT minutes is required when performance TAT is applicable"
            );
        }

        if (this.customerTatApplicable
                && !isPositive(this.customerTatMinutes)) {
            throw new IllegalStateException(
                    "Customer TAT minutes is required when customer TAT is applicable"
            );
        }

        if (this.rollbackTatApplicable
                && !this.allowRollback) {
            throw new IllegalStateException(
                    "Rollback TAT cannot be enabled when rollback is not allowed"
            );
        }

        if (this.rollbackTatApplicable
                && !isPositive(this.rollbackTatMinutes)) {
            throw new IllegalStateException(
                    "Rollback TAT minutes is required when rollback TAT is applicable"
            );
        }

        if (this.paymentPercentage < 0
                || this.paymentPercentage > 100) {
            throw new IllegalStateException(
                    "Payment percentage must be between 0 and 100"
            );
        }

    }

    private boolean isPositive(Integer value) {
        return value != null && value > 0;
    }

    private void validateNonNegative(
            Integer value,
            String message
    ) {
        if (value != null && value < 0) {
            throw new IllegalStateException(message);
        }
    }
}