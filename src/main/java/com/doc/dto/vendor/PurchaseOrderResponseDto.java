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

    private String paymentTerms;

    private Long vendorId;

    private String vendorName;

    private String vendorEmail;

    private String vendorMobile;

    private String vendorAddress;

    private String vendorCity;

    private String vendorState;

    private String vendorCountry;

    private String vendorGSTNumber;

    private VendorGSTRegistrationType vendorGSTRegistrationType;

    /**
     * Automatically derived from first two digits of Vendor GSTIN.
     */
    private String vendorStateCode;

    /**
     * Buyer/place-of-supply state.
     */
    private String placeOfSupplyStateCode;

    private String vendorPANNumber;

    private BigDecimal finalAmount;

    private BigDecimal gstRate;

    private BigDecimal cgstRate;

    private BigDecimal sgstRate;

    private BigDecimal igstRate;

    private BigDecimal cgstAmount;

    private BigDecimal sgstAmount;

    private BigDecimal igstAmount;

    private BigDecimal totalTaxAmount;

    private BigDecimal tdsPercentage;

    private BigDecimal tdsAmount;

    private BigDecimal grandTotal;

    private String scopeOfWork;

    private String termsAndConditions;

    private String remarks;

    private ProcurementOrderStatus status;

    private String paymentTypeName;

    private List<String> attachmentUrls;

    private Date poCreatedDate;

    private Date poSubmittedForApprovalDate;

    private Date poApprovedDate;

    private Date poReleasedDate;

    private Long createdBy;

    private Long updatedBy;

    private Long approvedBy;

    private Date createdDate;

    private Date updatedDate;
}