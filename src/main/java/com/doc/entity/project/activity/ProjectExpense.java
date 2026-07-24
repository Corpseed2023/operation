package com.doc.entity.project.activity;

import com.doc.em.ApprovalStatus;
import com.doc.em.ExpenseApprovalStage;
import com.doc.em.ExpenseCategory;
import com.doc.em.ExpensePaymentStatus;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectActivity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "project_expense",
        indexes = {
                @Index(
                        name = "idx_expense_project",
                        columnList = "project_id"
                ),
                @Index(
                        name = "idx_expense_approval_stage",
                        columnList = "approval_stage"
                ),
                @Index(
                        name = "idx_expense_approval_status",
                        columnList = "approval_status"
                ),
                @Index(
                        name = "idx_expense_payment_status",
                        columnList = "payment_status"
                ),
                @Index(
                        name = "idx_expense_created_by",
                        columnList = "created_by_user_id"
                ),
                @Index(
                        name = "idx_expense_department",
                        columnList = "raised_department_id"
                ),
                @Index(
                        name = "idx_expense_project_stage_status",
                        columnList = "project_id, approval_stage, approval_status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProjectExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Prevents two users from approving or updating the same expense
     * simultaneously.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "activity_id",
            nullable = false,
            unique = true
    )
    private ProjectActivity activity;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "project_id",
            nullable = false
    )
    private Project project;

    // ================= REQUEST ORIGIN =================

    /**
     * Department that raised the expense.
     * Example: Technical Department.
     */
    @Column(name = "raised_department_id", nullable = false)
    private Long raisedDepartmentId;

    /**
     * Snapshot of department name.
     * Retained even if the department master name changes later.
     */
    @Column(
            name = "raised_department_name",
            nullable = false,
            length = 150
    )
    private String raisedDepartmentName;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    /**
     * Snapshot of creator name.
     */
    @Column(
            name = "created_by_user_name",
            nullable = false,
            length = 150
    )
    private String createdByUserName;

    // ================= EXPENSE INFORMATION =================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "expense_category",
            nullable = false,
            length = 50
    )
    private ExpenseCategory expenseCategory;

    /**
     * Amount requested by the Technical or originating department.
     */
    @Column(
            name = "requested_amount",
            precision = 15,
            scale = 2,
            nullable = false
    )
    private BigDecimal requestedAmount;

    /**
     * Final amount approved by Accounts.
     * It may differ from the originally requested amount.
     */
    @Column(
            name = "approved_amount",
            precision = 15,
            scale = 2
    )
    private BigDecimal approvedAmount;

    /**
     * Total amount paid across payment transactions.
     */
    @Column(
            name = "paid_amount",
            precision = 15,
            scale = 2,
            nullable = false
    )
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /**
     * ISO-4217 currency code.
     */
    @Column(
            name = "currency_code",
            nullable = false,
            length = 3
    )
    private String currencyCode = "INR";

    @Column(
            name = "remark",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String remark;

    @Column(name = "expense_date", nullable = false)
    private LocalDateTime expenseDate;

    /**
     * Optional supporting document, challan, portal screenshot,
     * quotation or government fee document.
     */
    @Column(name = "attachment_url", length = 1000)
    private String attachmentUrl;

    /**
     * Optional external reference.
     *
     * Examples:
     * FSSAI application number,
     * government challan number,
     * portal application ID,
     * consultant reference.
     */
    @Column(name = "external_reference", length = 150)
    private String externalReference;

    // ================= OVERALL APPROVAL =================

    /**
     * Overall approval outcome.
     *
     * It remains PENDING after CRT approval because Accounts
     * approval is still pending.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "approval_status",
            nullable = false,
            length = 20
    )
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    /**
     * Identifies which department must act next.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "approval_stage",
            nullable = false,
            length = 30
    )
    private ExpenseApprovalStage approvalStage =
            ExpenseApprovalStage.CRT_REVIEW;

    // ================= CRT DECISION =================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "crt_approval_status",
            nullable = false,
            length = 20
    )
    private ApprovalStatus crtApprovalStatus =
            ApprovalStatus.PENDING;

    /**
     * The field is actionBy rather than approvedBy because
     * CRT can approve, reject or place the request on hold.
     */
    @Column(name = "crt_action_by_user_id")
    private Long crtActionByUserId;

    @Column(name = "crt_action_by_user_name", length = 150)
    private String crtActionByUserName;

    @Column(name = "crt_action_date")
    private LocalDateTime crtActionDate;

    @Column(name = "crt_decision_remark", columnDefinition = "TEXT")
    private String crtDecisionRemark;

    // ================= ACCOUNTS DECISION =================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "accounts_approval_status",
            nullable = false,
            length = 20
    )
    private ApprovalStatus accountsApprovalStatus =
            ApprovalStatus.PENDING;

    @Column(name = "accounts_action_by_user_id")
    private Long accountsActionByUserId;

    @Column(name = "accounts_action_by_user_name", length = 150)
    private String accountsActionByUserName;

    @Column(name = "accounts_action_date")
    private LocalDateTime accountsActionDate;

    @Column(
            name = "accounts_decision_remark",
            columnDefinition = "TEXT"
    )
    private String accountsDecisionRemark;

    // ================= PAYMENT SUMMARY =================

    /**
     * This is the aggregate payment status.
     * Individual payment transactions should be kept in a separate table.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_status",
            nullable = false,
            length = 30
    )
    private ExpensePaymentStatus paymentStatus =
            ExpensePaymentStatus.NOT_INITIATED;

    @Column(name = "payment_completed_date")
    private LocalDateTime paymentCompletedDate;

    // ================= AUDIT =================

    @Column(
            name = "created_date",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdDate;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdDate == null) {
            createdDate = now;
        }

        updatedDate = now;

        if (expenseDate == null) {
            expenseDate = now;
        }

        if (currencyCode == null || currencyCode.isBlank()) {
            currencyCode = "INR";
        } else {
            currencyCode = currencyCode.trim().toUpperCase();
        }

        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }

        if (approvalStatus == null) {
            approvalStatus = ApprovalStatus.PENDING;
        }

        if (approvalStage == null) {
            approvalStage = ExpenseApprovalStage.CRT_REVIEW;
        }

        if (crtApprovalStatus == null) {
            crtApprovalStatus = ApprovalStatus.PENDING;
        }

        if (accountsApprovalStatus == null) {
            accountsApprovalStatus = ApprovalStatus.PENDING;
        }

        if (paymentStatus == null) {
            paymentStatus = ExpensePaymentStatus.NOT_INITIATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }

    /**
     * Derived value. It should not be persisted because persisted
     * outstanding amounts can become inconsistent.
     */
    @Transient
    public BigDecimal getOutstandingAmount() {

        BigDecimal payableAmount =
                approvedAmount != null
                        ? approvedAmount
                        : requestedAmount;

        if (payableAmount == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalPaid =
                paidAmount != null
                        ? paidAmount
                        : BigDecimal.ZERO;

        BigDecimal outstanding = payableAmount.subtract(totalPaid);

        return outstanding.compareTo(BigDecimal.ZERO) > 0
                ? outstanding
                : BigDecimal.ZERO;
    }
}