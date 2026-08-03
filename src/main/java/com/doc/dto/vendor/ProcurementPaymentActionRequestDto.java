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

    /* ------------------------------------------------------------------
     * APPROVE / REJECT
     * ------------------------------------------------------------------ */

    /*
     * Approval or release comment.
     */
    private String comment;

    /*
     * Alias used by the release screen. When both are present
     * "remarks" wins.
     */
    private String remarks;

    /*
     * Required when rejecting the payment request.
     */
    private String reason;

    /* ------------------------------------------------------------------
     * INVOICE (captured at release)
     * ------------------------------------------------------------------ */

    private String invoiceNumber;

    private LocalDate invoiceDate;

    /* ------------------------------------------------------------------
     * RELEASE - BANK PAYMENT
     * ------------------------------------------------------------------ */

    /*
     * Date on which money actually left the bank / cash box.
     * Defaults to today when not supplied.
     */
    private LocalDate paymentDate;

    /*
     * Net amount actually paid to the vendor
     * (invoice + GST - TDS). Drives the PAYMENT voucher.
     */
    private BigDecimal bankPaymentAmount;

    /*
     * CASH / NEFT / RTGS / UPI / CHEQUE ...
     */
    private String paymentMode;

    /*
     * Credit side of the PAYMENT voucher (Bank / Cash ledger).
     */
    private Long bankLedgerId;

    /*
     * Debit side of the PAYMENT voucher (Vendor ledger).
     */
    private Long ledgerId;

    private String ledgerType;

    /*
     * Payment transaction reference supplied during release.
     */
    private String transactionReference;

    /*
     * Payment proof URL or document reference.
     */
    private String paymentProof;

    /*
     * Additional supporting documents captured at release.
     */
    private List<String> proofAttachmentUrls;

    /* ------------------------------------------------------------------
     * RELEASE - TDS
     * ------------------------------------------------------------------ */

    /*
     * "YES" / "NO" (also tolerates true / false / Y / N / 1 / 0).
     */
    private Boolean tdsActive;

    private BigDecimal tdsPercentage;

    private BigDecimal tdsAmount;

    /*
     * Ledger to which the deducted TDS is credited.
     */
    private Long tdsPayableLedgerId;
}