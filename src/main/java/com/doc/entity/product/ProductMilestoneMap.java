package com.doc.entity.product;

import com.doc.entity.milestone.Milestone;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.Date;

/**
 * Master entity mapping products to milestones with order, TAT and workflow rules.
 *
 * Example:
 * Product = CDSO
 * Milestone = Documentation
 * Step Order = 1
 *
 * Important:
 * This is only master/template configuration.
 * Actual project/user-wise TAT must be stored in project milestone assignment table.
 */
@Entity
@Table(
        name = "product_milestone_map",
        indexes = {
                @Index(name = "idx_product_id", columnList = "product_id"),
                @Index(name = "idx_milestone_id", columnList = "milestone_id"),
                @Index(name = "idx_product_step_order", columnList = "product_id, step_order")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_milestone",
                        columnNames = {"product_id", "milestone_id"}
                ),
                @UniqueConstraint(
                        name = "uk_product_step_order",
                        columnNames = {"product_id", "step_order"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProductMilestoneMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Product milestone mapping ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @Comment("Associated product")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    @Comment("Associated milestone")
    private Milestone milestone;

    @Column(name = "step_order", nullable = false)
    @Comment("Order of the milestone in the product workflow")
    private int order;

    /**
     * Old/default field.
     * Keep for backward compatibility.
     *
     * Example:
     * 2 means 2 days.
     */
    @Column(name = "tat_in_days", nullable = false)
    @Comment("Default turnaround time in days for this milestone")
    private double tatInDays;

    // =====================================================================
    // EXECUTION TAT - USER WISE
    // =====================================================================

    /**
     * Whether user-wise execution TAT is applicable.
     *
     * Example:
     * true  -> Vishal/Divya must complete under TAT.
     * false -> no user-wise TAT calculation.
     */
    @Column(name = "execution_tat_applicable", nullable = false)
    @Comment("Whether user-wise execution TAT is applicable")
    private boolean executionTatApplicable = true;

    /**
     * Existing day-based execution TAT.
     * Keep for backward compatibility.
     *
     * Example:
     * 2 means 2 days.
     */
    @Column(name = "execution_tat_in_days", nullable = false)
    @Comment("Default execution TAT in days given to every assignee")
    private double executionTatInDays;

    /**
     * New recommended hour-based execution TAT.
     *
     * Example:
     * 48 means 48 hours.
     */
    @Column(name = "execution_tat_hours")
    @Comment("Default execution TAT in hours given to every assignee")
    private Double executionTatHours;

    // =====================================================================
    // DEPARTMENT TAT
    // =====================================================================

    /**
     * Whether department-level TAT is applicable.
     *
     * Example:
     * true  -> Documentation department has SLA.
     * false -> department SLA is not applicable.
     */
    @Column(name = "department_tat_applicable", nullable = false)
    @Comment("Whether department-level TAT is applicable")
    private boolean departmentTatApplicable = false;

    @Column(name = "department_tat_hours")
    @Comment("Department-level TAT in hours")
    private Double departmentTatHours;

    // =====================================================================
    // PERFORMANCE TAT
    // =====================================================================

    /**
     * Whether this milestone should affect employee performance.
     *
     * Example:
     * true  -> Vishal/Divya score will be calculated.
     * false -> no performance calculation for this milestone.
     */
    @Column(name = "performance_tat_applicable", nullable = false)
    @Comment("Whether performance TAT is applicable for employee scoring")
    private boolean performanceTatApplicable = true;

    @Column(name = "performance_tat_hours")
    @Comment("Performance calculation TAT threshold in hours")
    private Double performanceTatHours;

    // =====================================================================
    // CUSTOMER / PROJECT SLA TAT
    // =====================================================================

    /**
     * Whether customer/project SLA is applicable.
     *
     * Example:
     * true  -> client-facing milestone SLA will be calculated.
     * false -> no customer SLA calculation.
     */
    @Column(name = "customer_tat_applicable", nullable = false)
    @Comment("Whether customer/project SLA TAT is applicable")
    private boolean customerTatApplicable = false;

    @Column(name = "customer_tat_hours")
    @Comment("Overall customer/project SLA TAT in hours")
    private Double customerTatHours;

    // =====================================================================
    // ROLLBACK TAT
    // =====================================================================

    @Column(name = "rollback_tat_applicable", nullable = false)
    @Comment("Whether rollback TAT is applicable")
    private boolean rollbackTatApplicable = false;

    @Column(name = "rollback_tat_in_days")
    @Comment("Rollback turnaround time in days, if rollback is applicable")
    private Double rollbackTatInDays;

    @Column(name = "rollback_tat_hours")
    @Comment("Rollback turnaround time in hours, if rollback is applicable")
    private Double rollbackTatHours;

    // =====================================================================
    // WORKFLOW RULES
    // =====================================================================

    @Column(name = "strict_approval", nullable = false)
    @Comment("Whether approval is mandatory before moving to the next milestone")
    private boolean strictApproval = false;

    @Column(name = "allow_rollback", nullable = false)
    @Comment("Whether this milestone can be rolled back")
    private boolean allowRollback = false;

    @Column(name = "max_attempts", nullable = false)
    @Comment("Maximum retry attempts after rejection/disapproval")
    private int maxAttempts = 1;

    @Column(name = "is_mandatory", nullable = false)
    @Comment("Whether this milestone is mandatory for the product workflow")
    private boolean isMandatory = true;

    @Column(name = "payment_percentage", nullable = false)
    @Comment("Percentage of total payment required/unlocked at this milestone")
    private double paymentPercentage;

    @Column(name = "is_auto_generated", nullable = false)
    @Comment("Whether this milestone is auto-generated by system")
    private boolean isAutoGenerated = false;

    @Column(name = "requires_portal_details", nullable = false)
    @Comment("Whether this milestone requires client portal login credentials")
    private boolean requiresPortalDetails = false;

    /**
     * If true, new assignee gets fresh execution TAT on reassignment.
     *
     * Example:
     * Vishal failed.
     * Divya is assigned now.
     * Divya gets fresh 48 hours.
     */
    @Column(name = "allow_tat_reset_on_reassign", nullable = false)
    @Comment("Whether TAT should reset for new assignee after reassignment")
    private boolean allowTatResetOnReassign = true;

    /**
     * If true, TAT calculation can skip Sundays/holidays/business off-hours.
     * Actual implementation should be done in a calendar/TAT calculation service.
     */
    @Column(name = "business_days_enabled", nullable = false)
    @Comment("Whether TAT should be calculated using business days/business calendar")
    private boolean businessDaysEnabled = false;

    // =====================================================================
    // REMINDER / ESCALATION
    // =====================================================================

    @Column(name = "reminder_before_due_hours")
    @Comment("Reminder hours before due date")
    private Integer reminderBeforeDueHours;

    @Column(name = "manager_escalation_after_due_hours")
    @Comment("Escalation hours after due date breach")
    private Integer managerEscalationAfterDueHours;

    @Column(name = "hod_escalation_after_due_hours")
    @Comment("Escalation hours after due date breach for HOD level")
    private Integer hodEscalationAfterDueHours;

    // =====================================================================
    // COMMON FIELDS
    // =====================================================================

    @Column(name = "is_active", nullable = false)
    @Comment("Whether this product milestone mapping is active")
    private boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    private Date updatedDate;

    private LocalDate date;

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

    @PreUpdate
    public void preUpdate() {
        this.updatedDate = new Date();

        normalizeFields();
        validateFields();
    }

    private void normalizeFields() {

        if (this.maxAttempts <= 0) {
            this.maxAttempts = 1;
        }

        /*
         * EXECUTION TAT NORMALIZATION
         */
        if (!this.executionTatApplicable) {
            this.executionTatHours = null;
            this.executionTatInDays = 0;
            this.tatInDays = 0;
        } else {
            if ((this.executionTatHours == null || this.executionTatHours <= 0)
                    && this.executionTatInDays > 0) {
                this.executionTatHours = this.executionTatInDays * 24;
            }

            if ((this.executionTatHours == null || this.executionTatHours <= 0)
                    && this.tatInDays > 0) {
                this.executionTatHours = this.tatInDays * 24;
            }

            if (this.executionTatHours != null && this.executionTatHours > 0) {
                this.executionTatInDays = this.executionTatHours / 24;
                this.tatInDays = this.executionTatHours / 24;
            }
        }

        /*
         * DEPARTMENT TAT NORMALIZATION
         */
        if (!this.departmentTatApplicable) {
            this.departmentTatHours = null;
        }

        /*
         * PERFORMANCE TAT NORMALIZATION
         */
        if (!this.performanceTatApplicable) {
            this.performanceTatHours = null;
        } else {
            if ((this.performanceTatHours == null || this.performanceTatHours <= 0)
                    && this.executionTatApplicable
                    && this.executionTatHours != null
                    && this.executionTatHours > 0) {
                this.performanceTatHours = this.executionTatHours;
            }
        }

        /*
         * CUSTOMER TAT NORMALIZATION
         */
        if (!this.customerTatApplicable) {
            this.customerTatHours = null;
        }

        /*
         * ROLLBACK TAT NORMALIZATION
         */
        if (!this.rollbackTatApplicable) {
            this.rollbackTatHours = null;
            this.rollbackTatInDays = null;
        } else {
            if ((this.rollbackTatHours == null || this.rollbackTatHours <= 0)
                    && this.rollbackTatInDays != null
                    && this.rollbackTatInDays > 0) {
                this.rollbackTatHours = this.rollbackTatInDays * 24;
            }

            if (this.rollbackTatHours != null && this.rollbackTatHours > 0) {
                this.rollbackTatInDays = this.rollbackTatHours / 24;
            }
        }

        /*
         * ESCALATION DEFAULTS
         */
        if (this.reminderBeforeDueHours != null && this.reminderBeforeDueHours < 0) {
            this.reminderBeforeDueHours = null;
        }

        if (this.managerEscalationAfterDueHours != null && this.managerEscalationAfterDueHours < 0) {
            this.managerEscalationAfterDueHours = null;
        }

        if (this.hodEscalationAfterDueHours != null && this.hodEscalationAfterDueHours < 0) {
            this.hodEscalationAfterDueHours = null;
        }
    }

    private void validateFields() {

        if (this.executionTatApplicable
                && (this.executionTatHours == null || this.executionTatHours <= 0)) {
            throw new IllegalStateException("Execution TAT hours is required when execution TAT is applicable");
        }

        if (this.departmentTatApplicable
                && (this.departmentTatHours == null || this.departmentTatHours <= 0)) {
            throw new IllegalStateException("Department TAT hours is required when department TAT is applicable");
        }

        if (this.performanceTatApplicable
                && (this.performanceTatHours == null || this.performanceTatHours <= 0)) {
            throw new IllegalStateException("Performance TAT hours is required when performance TAT is applicable");
        }

        if (this.customerTatApplicable
                && (this.customerTatHours == null || this.customerTatHours <= 0)) {
            throw new IllegalStateException("Customer TAT hours is required when customer TAT is applicable");
        }

        if (this.rollbackTatApplicable
                && (this.rollbackTatHours == null || this.rollbackTatHours <= 0)) {
            throw new IllegalStateException("Rollback TAT hours is required when rollback TAT is applicable");
        }

        if (this.paymentPercentage < 0 || this.paymentPercentage > 100) {
            throw new IllegalStateException("Payment percentage must be between 0 and 100");
        }

        if (this.reminderBeforeDueHours != null && this.reminderBeforeDueHours < 0) {
            throw new IllegalStateException("Reminder before due hours cannot be negative");
        }

        if (this.managerEscalationAfterDueHours != null && this.managerEscalationAfterDueHours < 0) {
            throw new IllegalStateException("Manager escalation hours cannot be negative");
        }

        if (this.hodEscalationAfterDueHours != null && this.hodEscalationAfterDueHours < 0) {
            throw new IllegalStateException("HOD escalation hours cannot be negative");
        }
    }
}