package com.doc.dto.project.activity.expense;

import com.doc.em.ApprovalStatus;
import com.doc.em.ExpensePaidBy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CrtExpenseDecisionRequestDto {

    @NotNull(message = "CRT decision status is required")
    private ApprovalStatus status;

    /**
     * Required when status = APPROVED.
     */
    private ExpensePaidBy expensePaidBy;

    @Size(
            max = 2000,
            message = "Remark cannot exceed 2000 characters"
    )
    private String remark;

    /**
     * Required only when expensePaidBy = CLIENT_TO_COMPANY.
     *
     * Supported values:
     * CASH, CASH_DEPOSIT, CHEQUE, DEMAND_DRAFT,
     * NEFT, RTGS, IMPS, UPI, CARD, BANK_TRANSFER, OTHER
     */
    @Size(
            max = 30,
            message = "Client payment mode cannot exceed 30 characters"
    )
    private String clientPaymentMode;

    /**
     * ================================================================
     * OUR bank ledger (Account Service LedgerMaster, ledgerType = BANK)
     * that actually received the client's money.
     * Example: HDFC (ledgerId=2), Axis, Kotak.
     * Required for any clientPaymentMode other than CASH.
     * ================================================================
     */
    @Positive(
            message = "Client payment bank ledger ID must be greater than zero"
    )
    private Long clientPaymentBankLedgerId;

    /**
     * Display snapshot of the bank above.
     * Account Service will validate the ledger ID during Step 3.
     */
    @Size(
            max = 150,
            message = "Client payment bank name cannot exceed 150 characters"
    )
    private String clientPaymentBankName;


    /**
     * Display snapshot of the client ledger above (e.g. "Microsoft").
     */
    @Size(
            max = 150,
            message = "Client ledger name cannot exceed 150 characters"
    )
    private String clientLedgerName;

    @PastOrPresent(
            message = "Client payment date cannot be in the future"
    )
    private LocalDate clientPaymentDate;

    /**
     * UTR, cheque number, deposit slip number, UPI reference, etc.
     */
    @Size(
            max = 100,
            message = "Client payment reference cannot exceed 100 characters"
    )
    private String clientPaymentReference;

    @Size(
            max = 1000,
            message = "Client payment proof URL cannot exceed 1000 characters"
    )
    private String clientPaymentProofUrl;
}