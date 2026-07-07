package com.doc.dto.vendor;

import com.doc.entity.vendor.PaymentRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class ProcurementPaymentRequestResponseDto {

    private Long id;

    private Long procurementOrderId;
    private String poNumber;

    private Long projectId;
    private String projectName;
    private String projectNo;

    private Long vendorId;
    private String vendorName;

    private BigDecimal invoiceAmount;
    private BigDecimal payableAmount;

    private String invoiceNumber;
    private Date invoiceDate;
    private Date submissionDate;

    private String completionRemarks;
    private List<String> proofAttachmentUrls;

    private PaymentRequestStatus status;

    private Date approvedDate;
    private Date paymentReleasedDate;

    private Long createdBy;
    private Long approvedBy;
    private Long paymentReleasedBy;

    private Date createdDate;
    private Date updatedDate;

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