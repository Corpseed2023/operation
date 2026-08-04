package com.doc.dto;

import com.doc.em.ExpensePaidBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentFeePostingRequestDto {

    /**
     * ProjectExpense ID from Operation Service.
     * Used for idempotency in Account Service.
     */
    private Long operationExpenseId;

    // =========================================================
    // PROJECT DETAILS
    // =========================================================

    private Long projectId;

    private String projectNo;

    private String projectName;

    // =========================================================
    // CLIENT DETAILS
    // =========================================================

    /**
     * Required for creating/finding the client-specific
     * Government Fee Advance ledger.
     */
    private Long clientCompanyId;

    private String clientCompanyName;

    private Long clientUnitId;

    private String clientUnitName;

    // =========================================================
    // EXPENSE DETAILS
    // =========================================================

    /**
     * GOVERNMENT_FEE.
     */
    private String expenseCategory;

    private BigDecimal approvedAmount;

    /**
     * Usually INR.
     */
    private String currencyCode;

    /**
     * Accounts approval/accounting date.
     *
     * This field generates the Lombok builder method:
     * .expenseDate(...)
     */
    private LocalDate expenseDate;

    /**
     * COMPANY, CLIENT_TO_COMPANY, CLIENT_DIRECT or legacy CLIENT.
     */
    private ExpensePaidBy paidBy;

    // =========================================================
    // CLIENT FUNDING DETAILS
    // =========================================================

    /**
     * CASH, CASH_DEPOSIT, CHEQUE, DEMAND_DRAFT,
     * NEFT, RTGS, IMPS, UPI, CARD,
     * BANK_TRANSFER or OTHER.
     */
    private String clientPaymentMode;

    /**
     * Account Service ledger ID where client money was received.
     * Example: HDFC Bank ledger ID.
     */
    private Long clientPaymentBankLedgerId;

    private String clientPaymentBankName;

    /**
     * Date when client money was received.
     */
    private LocalDate clientPaymentDate;

    /**
     * UTR, cheque number, deposit reference, etc.
     */
    private String clientPaymentReference;

    private String clientPaymentProofUrl;

    // =========================================================
    // ACCOUNTS APPROVER DETAILS
    // =========================================================

    private Long approvedByUserId;

    private String approvedByUserName;

    private String narration;
}