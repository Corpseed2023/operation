package com.doc.dto.vendor;

import com.doc.entity.vendor.ProcurementOrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PurchaseOrderResponseDto {

    private Long id;
    private String poNumber;
    private String poReferenceNumber;

    private Long procurementAssignmentId;
    private Long projectId;
    private String projectName;
    private String projectNo;

    private Long vendorId;
    private String vendorName;

    private BigDecimal estimatedAmount;
    private BigDecimal finalAmount;

    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal grandTotal;
    private BigDecimal gstRate;

    private String scopeOfWork;
    private String paymentTerms;
    private String termsAndConditions;
    private String remarks;
    private ProcurementOrderStatus status;

    private String paymentTypeName;

    private List<String> attachmentUrls;

    private Date poCreatedDate;
    private Date poApprovedDate;
    private Date poReleasedDate;

    private Long createdBy;
    private Long approvedBy;
    private Date createdDate;
    private Date updatedDate;
}