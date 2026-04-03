package com.doc.entity.project.activity;

import com.doc.em.ApprovalStatus;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectActivity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_expense")
@Data
@NoArgsConstructor
public class ProjectExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false, unique = true)
    private ProjectActivity activity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "expense_type", length = 50)
    private String expenseType;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "expense_date", nullable = false)
    private LocalDateTime expenseDate;

    @Column(name = "payment_medium", length = 30)
    @Comment("Payment medium: CASH, ONLINE, UPI, CARD, BANK_TRANSFER, etc.")
    private String paymentMedium;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20, nullable = false)
    @Comment("Expense approval status: PENDING, APPROVED, REJECTED, ON_HOLD")
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    private boolean isApproved = false;

    private String approvedByUserName;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    private String createdByUserName;

    @Column(name = "rejection_remark", columnDefinition = "TEXT")
    @Comment("Remark when expense is rejected")
    private String rejectionRemark;

    @Column(name = "approved_date")
    @Comment("Date when expense was approved/rejected/on-hold")
    private LocalDateTime approvedDate;
}