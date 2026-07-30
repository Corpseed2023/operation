package com.doc.dto.vendor;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ProcurementPaymentRequestDto {

    private BigDecimal invoiceAmount;
    private BigDecimal payableAmount;

    private String completionRemarks;

    private List<String> proofAttachmentUrls;

    private Long createdBy;

    private Boolean tdsActive;
    private BigDecimal tdsPercentage;

    private Boolean gstActive;
    private String gstStateCode;
    private BigDecimal gstPercentage;

    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal totalGstAmount;

    private BigDecimal amount;
    private String paymentMode;

    private Long bankLedgerId;
    private Long ledgerId;
    private String ledgerType;

    private String transactionReference;
    private String paymentProof;

    private String gstType;
    private BigDecimal tdsAmount;

}