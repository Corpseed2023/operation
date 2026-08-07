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

    private String comment;
    private String remarks;
    private String reason;

    private String invoiceNumber;
    private LocalDate invoiceDate;

    private LocalDate paymentDate;

    /**
     * Optional confirmation supplied by the UI. The backend always derives
     * the bank amount as invoice gross minus TDS. A different value is rejected.
     */
    private BigDecimal bankPaymentAmount;

    private String paymentMode;
    private Long bankLedgerId;

    /** Optional legacy metadata. Account Service resolves the vendor ledger. */
    private Long ledgerId;
    private String ledgerType;

    private String transactionReference;
    private String paymentProof;
    private List<String> proofAttachmentUrls;

    /** Optional confirmations only; stored tax configuration cannot change at release. */
    private Boolean tdsActive;
    private BigDecimal tdsPercentage;
    private BigDecimal tdsAmount;
    private Long tdsPayableLedgerId;
}
