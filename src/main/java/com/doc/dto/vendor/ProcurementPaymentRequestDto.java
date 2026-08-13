package com.doc.dto.vendor;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request used to create one procurement vendor invoice payment request.
 *
 * The client supplies invoice gross and tax configuration. All calculated
 * monetary fields are ignored and recalculated by Operation Service.
 */
@Data
public class ProcurementPaymentRequestDto {

    /** GST-inclusive invoice gross. Required. */
    private BigDecimal invoiceAmount;

    private String invoiceNumber;
    private LocalDate invoiceDate;

    private String completionRemarks;
    private List<String> proofAttachmentUrls;
    private Long createdBy;

    private Boolean tdsActive;
    private BigDecimal tdsPercentage;

    private Boolean gstActive;
    private String gstType;
    private String gstStateCode;
    private BigDecimal gstPercentage;


    /*
     * Legacy client-calculated fields retained for JSON compatibility only.
     * ProcurementPaymentRequestServiceImpl does not trust or persist these
     * values; it replaces them with the backend calculation snapshot.
     */
    @Deprecated
    private BigDecimal payableAmount;
    @Deprecated
    private BigDecimal amount;
    @Deprecated
    private BigDecimal cgstAmount;
    @Deprecated
    private BigDecimal sgstAmount;
    @Deprecated
    private BigDecimal igstAmount;
    @Deprecated
    private BigDecimal totalGstAmount;
    @Deprecated
    private BigDecimal tdsAmount;

    /* Legacy release fields. Use ProcurementPaymentActionRequestDto instead. */
    @Deprecated
    private String paymentMode;
    @Deprecated
    private Long bankLedgerId;
    @Deprecated
    private Long ledgerId;
    @Deprecated
    private String ledgerType;
    @Deprecated
    private String transactionReference;
    @Deprecated
    private String paymentProof;
}
