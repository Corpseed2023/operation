package com.doc.dto.vendor;

import com.doc.entity.vendor.ProcurementOrderStatus;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ProcurementOrderResponseDto {

    private Long id;

    private Long procurementAssignmentId;
    private Long projectId;
    private String projectName;

    private Long vendorId;
    private String vendorName;

    private Long vendorContactId;
    private String vendorContactName;

    private String poNumber;
    private String poReferenceNumber;

    private BigDecimal finalAmount;
    private BigDecimal gstRate;

    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;

    private BigDecimal totalTaxAmount;
    private BigDecimal grandTotal;

    private String scopeOfWork;
    private String termsAndConditions;
    private String remarks;

    private List<String> attachmentUrls;

    private ProcurementOrderStatus status;

    private Date poCreatedDate;
    private Date poSubmittedForApprovalDate;
    private Date poApprovedDate;
    private Date poReleasedDate;

    private Long paymentTypeId;
    private String paymentTypeName;

    private Long createdBy;
    private Long updatedBy;
    private Long approvedBy;

    private Date createdDate;
    private Date updatedDate;
}