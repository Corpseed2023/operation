package com.doc.dto.project.activity.expense;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for recording final payment to government
 *
 * Scenario: After funds are in the payment bank (e.g., Axis),
 * Technical pays the government and uploads the receipt.
 *
 * Creates PAYMENT voucher: Dr Government Fee Payable / Cr Bank
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentToGovernmentRequestDto {

    /**
     * Bank ledger from which the payment is made
     * Example: Axis Bank (must have sufficient balance)
     */
    @NotNull(message = "Payment bank ledger ID is required")
    @Positive(message = "Payment bank ledger ID must be greater than zero")
    private Long paymentBankLedgerId;

    /**
     * Date when payment was made to the government
     */
    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    /**
     * Government portal reference/receipt number
     * Example: "FSSAI-REF-2026-0123" or "CHALLAN-2026-00089"
     */
    @NotNull(message = "Government reference number is required")
    private String governmentReference;

    /**
     * URL to government receipt/confirmation PDF
     */
    private String proofUrl;

    /**
     * Optional remarks
     */
    private String remarks;
}