package com.doc.dto.project.activity.expense;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for inter-bank fund transfer
 *
 * Scenario: Client deposit is in HDFC, but Technical can only pay from Axis.
 * Accounts initiates this transfer.
 *
 * Creates CONTRA voucher: Dr Axis Bank / Cr HDFC Bank
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundTransferRequestDto {

    /**
     * Source bank ledger (where the money currently is)
     * Example: HDFC Bank
     */
    @NotNull(message = "Source bank ledger ID is required")
    @Positive(message = "Source bank ledger ID must be greater than zero")
    private Long fromBankLedgerId;

    /**
     * Destination bank ledger (where the money will go)
     * Example: Axis Bank
     */
    @NotNull(message = "Destination bank ledger ID is required")
    @Positive(message = "Destination bank ledger ID must be greater than zero")
    private Long toBankLedgerId;

    /**
     * Amount to transfer
     */
    @NotNull(message = "Transfer amount is required")
    @Positive(message = "Transfer amount must be greater than zero")
    private BigDecimal amount;

    /**
     * Date of transfer
     */
    @NotNull(message = "Transfer date is required")
    private LocalDate transferDate;

    /**
     * Bank reference: transfer ID, cheque number, NEFT reference, etc.
     * Example: "NEFT-2026-0012" or "CHQ-000789"
     */
    @NotNull(message = "Transfer reference is required")
    private String transferReference;

    /**
     * Optional remarks/notes
     */
    private String remarks;
}