package com.doc.dto.vendor;

import com.doc.entity.vendor.ProcurementOrderStatus;
import com.doc.entity.vendor.VendorGSTRegistrationType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
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
    private VendorGSTRegistrationType vendorGSTRegistrationType;
    private BigDecimal finalAmount;

    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal tdsPercentage;
    private BigDecimal tdsAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal grandTotal;
    private BigDecimal gstRate;

    private String scopeOfWork;
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