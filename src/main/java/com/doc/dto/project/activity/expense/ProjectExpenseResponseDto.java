package com.doc.dto.project.activity.expense;

import com.doc.em.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProjectExpenseResponseDto {

    // =========================================================
    // BASIC IDENTIFIERS
    // =========================================================

    private Long expenseId;

    private Long activityId;

    // =========================================================
    // PROJECT DETAILS
    // =========================================================

    private Long projectId;

    private String projectNo;

    private String projectName;

    private String unbilledNumber;

    private String productName;

    // =========================================================
    // ORIGINATING DEPARTMENT
    // =========================================================

    /**
     * Department that raised the expense.
     */
    private Long raisedDepartmentId;

    private String raisedDepartmentName;

    // =========================================================
    // EXPENSE DETAILS
    // =========================================================

    private ExpenseCategory expenseCategory;

    /**
     * Amount requested by the originating department.
     */
    private BigDecimal requestedAmount;

    /**
     * Final amount approved by Accounts.
     */
    private BigDecimal approvedAmount;

    /**
     * Total amount paid against this expense.
     */
    private BigDecimal paidAmount;

    /**
     * approvedAmount - paidAmount.
     */
    private BigDecimal outstandingAmount;

    /**
     * ISO currency code such as INR, USD or EUR.
     */
    private String currencyCode;

    private String remark;

    private LocalDateTime expenseDate;

    /**
     * Optional supporting document.
     */
    private String attachmentUrl;

    /**
     * Optional application, challan or portal reference.
     */
    private String externalReference;

    // =========================================================
    // OVERALL APPROVAL WORKFLOW
    // =========================================================

    /**
     * Overall expense approval status.
     */
    private ApprovalStatus approvalStatus;

    /**
     * Current workflow stage:
     * CRT_REVIEW, ACCOUNTS_REVIEW or COMPLETED.
     */
    private ExpenseApprovalStage approvalStage;

    // =========================================================
    // CRT APPROVAL DETAILS
    // =========================================================

    private ApprovalStatus crtApprovalStatus;

    private Long crtActionByUserId;

    private String crtActionByUserName;

    private LocalDateTime crtActionDate;

    private String crtDecisionRemark;

    // =========================================================
    // ACCOUNTS APPROVAL DETAILS
    // =========================================================

    private ApprovalStatus accountsApprovalStatus;

    private Long accountsActionByUserId;

    private String accountsActionByUserName;

    private LocalDateTime accountsActionDate;

    private String accountsDecisionRemark;

    // =========================================================
    // PAYMENT SUMMARY
    // =========================================================

    private ExpensePaymentStatus paymentStatus;

    private LocalDateTime paymentCompletedDate;

    // =========================================================
    // CREATOR AND AUDIT DETAILS
    // =========================================================

    private Long createdByUserId;

    private String createdByUserName;

    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;

    private ExpensePaidBy expensePaidBy;

    private AccountPostingStatus accountPostingStatus;

    private Long accountVoucherId;

    private String accountVoucherNumber;

    private LocalDateTime accountPostedAt;

    private String accountPostingError;


}