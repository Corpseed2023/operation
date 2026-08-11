package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementPaymentActionRequestDto {

    /*
     * Common approval/rejection fields
     */
    private String comment;
    private String remarks;
    private String reason;

    /*
     * Payment release
     */
    private String paymentMode;

    private Long bankLedgerId;

    /*
     * Kept only for backward compatibility.
     *
     * IMPORTANT:
     * Operation backend must NOT trust this amount.
     * Backend calculates the actual bank payment.
     */
    private BigDecimal bankPaymentAmount;

    private LocalDate paymentDate;

    private String transactionReference;

    private String paymentProof;

    private List<String> proofAttachmentUrls;

    /*
     * =========================================================
     * RELEASE-TIME TDS CONFIGURATION
     * =========================================================
     *
     * Boolean instead of primitive boolean is intentional.
     *
     * null  -> keep Payment Request's existing TDS configuration
     * true  -> apply/recalculate TDS using tdsPercentage
     * false -> disable TDS
     */
    private Boolean tdsActive;

    private BigDecimal tdsPercentage;

    /*
     * Optional.
     *
     * Account Service may resolve its own TDS Payable ledger.
     * If Accounts explicitly selects one, pass it through.
     */
    private Long tdsPayableLedgerId;

    /*
     * Existing compatibility fields, if your frontend still sends them.
     * Operation should not use these to determine vendor liability.
     */
    private Long ledgerId;
}