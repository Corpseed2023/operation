package com.doc.dto.vendor;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementPaymentActionRequestDto {

    private String comment;

    private String reason;

    private String invoiceNumber;

    private Date invoiceDate;

    /*
     * Existing Account Service bank ledger ID.
     *
     * Required when releasing payment.
     */
    private Long bankLedgerId;

    /*
     * Existing Account Service TDS Payable ledger ID.
     *
     * Required only when tdsAmount > 0.
     */
    private Long tdsPayableLedgerId;

    /*
     * Actual amount transferred through the bank.
     */
    private BigDecimal bankPaymentAmount;

    /*
     * Exact TDS amount deducted.
     *
     * Do not recalculate it from percentage at this stage.
     */
    @Builder.Default
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    private String transactionReference;
}