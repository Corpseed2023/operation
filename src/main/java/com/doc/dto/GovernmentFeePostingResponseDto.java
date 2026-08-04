package com.doc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentFeePostingResponseDto {

    /**
     * POSTED, ALREADY_POSTED or SKIPPED_CLIENT_DIRECT.
     */
    private String postingStatus;

    private String message;

    /**
     * ProjectExpense ID from Operation Service.
     */
    private Long operationExpenseId;

    // =========================================================
    // STEP 3 — ENTRY A: CLIENT RECEIPT
    // =========================================================

    /**
     * RECEIPT voucher:
     *
     * Dr Company Bank
     *    Cr Client Government Fee Advance
     *
     * Null when the expense is funded by COMPANY.
     */
    private Long receiptVoucherId;

    private String receiptVoucherNumber;

    // =========================================================
    // STEP 3 — ENTRY B: GOVERNMENT-FEE ACCRUAL
    // =========================================================

    /**
     * Client-funded:
     *
     * Dr Client Government Fee Advance
     *    Cr Government Fee Payable
     *
     * Company-funded:
     *
     * Dr Government Fee Expense
     *    Cr Government Fee Payable
     */
    private Long journalVoucherId;

    private String journalVoucherNumber;

    // =========================================================
    // LEDGER REFERENCES
    // =========================================================

    /**
     * HDFC/Kotak/Axis/Cash ledger where client money was received.
     */
    private Long receivingBankLedgerId;

    /**
     * Client and unit-specific Government Fee Advance ledger.
     */
    private Long clientAdvanceLedgerId;

    /**
     * Populated only for company-funded government fees.
     */
    private Long governmentFeeExpenseLedgerId;

    private Long governmentFeePayableLedgerId;

    // =========================================================
    // BACKWARD COMPATIBILITY
    // =========================================================

    /**
     * Legacy primary voucher field.
     * It should contain journalVoucherId.
     */
    @Deprecated
    private Long voucherId;

    /**
     * Legacy primary voucher number.
     * It should contain journalVoucherNumber.
     */
    @Deprecated
    private String voucherNumber;

    private LocalDateTime postedAt;
}