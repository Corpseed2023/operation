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


    private String tdsActive;
    private BigDecimal tdsPercentage;

    private String gstActive;
    private String gstStateCode;
    private BigDecimal gstPercentage;

    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal totalGstAmount;

}