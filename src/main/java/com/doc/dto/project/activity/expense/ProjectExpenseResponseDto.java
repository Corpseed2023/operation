package com.doc.dto.project.activity.expense;

import com.doc.em.AccountPostingStatus;
import com.doc.em.ApprovalStatus;
import com.doc.em.ExpenseApprovalStage;
import com.doc.em.ExpenseCategory;
import com.doc.em.ExpensePaidBy;
import com.doc.em.ExpensePaymentStatus;
import com.doc.em.GovernmentPaymentVerificationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProjectExpenseResponseDto {

    // Basic identifiers
    private Long expenseId;
    private Long activityId;

    // Project details
    private Long projectId;
    private String projectNo;
    private String projectName;
    private String unbilledNumber;
    private String productName;

    // Originating department
    private Long raisedDepartmentId;
    private String raisedDepartmentName;

    // Expense details
    private ExpenseCategory expenseCategory;
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String currencyCode;
    private String remark;
    private LocalDateTime expenseDate;
    private String attachmentUrl;
    private String externalReference;

    // Overall approval workflow
    private ApprovalStatus approvalStatus;
    private ExpenseApprovalStage approvalStage;

    // CRT decision
    private ApprovalStatus crtApprovalStatus;
    private Long crtActionByUserId;
    private String crtActionByUserName;
    private LocalDateTime crtActionDate;
    private String crtDecisionRemark;

    // Client funding declaration from Step 2
    private String clientPaymentMode;
    private Long clientPaymentBankLedgerId;
    private String clientPaymentBankName;
    private LocalDate clientPaymentDate;
    private String clientPaymentReference;
    private String clientPaymentProofUrl;

    // Accounts approval
    private ApprovalStatus accountsApprovalStatus;
    private Long accountsActionByUserId;
    private String accountsActionByUserName;
    private LocalDateTime accountsActionDate;
    private String accountsDecisionRemark;

    // Payment summary
    private ExpensePaymentStatus paymentStatus;
    private LocalDateTime paymentCompletedDate;

    // Creator and audit
    private Long createdByUserId;
    private String createdByUserName;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // General Step 3 account posting
    private ExpensePaidBy expensePaidBy;
    private AccountPostingStatus accountPostingStatus;
    private Long accountVoucherId;
    private String accountVoucherNumber;
    private LocalDateTime accountPostedAt;
    private String accountPostingError;

    // Step 3 Entry A - client receipt
    private Long receiptVoucherId;
    private String receiptVoucherNumber;

    // Step 3 Entry B - government-fee accrual journal
    private Long initialJournalVoucherId;
    private String initialJournalVoucherNumber;

    // Step 4 fund-transfer posting status
    private AccountPostingStatus fundTransferPostingStatus;

    // Step 4 transfer request/audit details
    private Long fundTransferFromBankLedgerId;
    private String fundTransferFromBankName;
    private Long fundTransferToBankLedgerId;
    private String fundTransferToBankName;
    private BigDecimal fundTransferAmount;
    private LocalDate fundTransferDate;
    private String fundTransferReference;
    private String fundTransferProofUrl;

    // Step 4 Entry C - CONTRA voucher
    private Long fundTransferVoucherId;
    private String fundTransferVoucherNumber;
    private String fundTransferPostingError;

    // Destination bank retained for Step 5 payment
    private Long paymentBankLedgerId;
    private String paymentBankName;

    // =========================================================
    // STEP 5 - FINAL GOVERNMENT PAYMENT
    // =========================================================

    private AccountPostingStatus governmentPaymentPostingStatus;
    private GovernmentPaymentVerificationStatus governmentPaymentVerificationStatus;
    private String governmentPaymentMode;
    private BigDecimal governmentPaymentAmount;
    private LocalDate governmentPaymentDate;
    private String governmentPaymentReference;
    private String governmentPaymentReceiptUrl;
    private String governmentPaymentRemark;
    private String governmentPaymentVerificationRemark;
    private Long governmentPaymentVoucherId;
    private String governmentPaymentVoucherNumber;
    private String governmentPaymentPostingError;
    private Long governmentPaymentMarkedByUserId;
    private String governmentPaymentMarkedByUserName;
    private LocalDateTime governmentPaymentMarkedAt;
    private Long governmentPaymentSubmittedByUserId;
    private String governmentPaymentSubmittedByUserName;
    private LocalDateTime governmentPaymentSubmittedAt;
}
