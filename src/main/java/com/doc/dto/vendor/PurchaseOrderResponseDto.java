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

    // =========================================================
    // PO
    // =========================================================

    private Long id;

    private String poNumber;

    private String poReferenceNumber;

    // =========================================================
    // PROCUREMENT
    // =========================================================

    private Long procurementAssignmentId;

    // =========================================================
    // PROJECT
    // =========================================================

    private Long projectId;

    private String projectName;

    private String projectNo;

    // =========================================================
    // VENDOR BASIC DETAILS
    // =========================================================

    private Long vendorId;

    private String vendorName;

    private String vendorEmail;

    private String vendorMobile;

    // =========================================================
    // VENDOR ADDRESS
    // =========================================================

    private String vendorAddress;

    private String vendorCity;

    private String vendorState;

    private String vendorCountry;

    // =========================================================
    // VENDOR GST
    // =========================================================

    private String vendorGSTNumber;

    private VendorGSTRegistrationType vendorGSTRegistrationType;

    /**
     * Automatically derived from first
     * two digits of Vendor GSTIN.
     */
    private String vendorStateCode;

    /**
     * Buyer/place-of-supply state.
     */
    private String placeOfSupplyStateCode;

    // =========================================================
    // VENDOR PAN
    // =========================================================

    private String vendorPANNumber;

    // =========================================================
    // BASE AMOUNT
    // =========================================================

    private BigDecimal finalAmount;

    // =========================================================
    // GST RATES
    // =========================================================

    private BigDecimal gstRate;

    private BigDecimal cgstRate;

    private BigDecimal sgstRate;

    private BigDecimal igstRate;

    // =========================================================
    // GST AMOUNTS
    // =========================================================

    private BigDecimal cgstAmount;

    private BigDecimal sgstAmount;

    private BigDecimal igstAmount;

    private BigDecimal totalTaxAmount;

    // =========================================================
    // TDS
    // =========================================================

    private BigDecimal tdsPercentage;

    private BigDecimal tdsAmount;

    // =========================================================
    // FINAL PAYABLE
    // =========================================================

    private BigDecimal grandTotal;

    // =========================================================
    // COMMERCIAL
    // =========================================================

    private String scopeOfWork;

    private String termsAndConditions;

    private String remarks;

    // =========================================================
    // STATUS
    // =========================================================

    private ProcurementOrderStatus status;

    // =========================================================
    // PAYMENT
    // =========================================================

    private String paymentTypeName;

    // =========================================================
    // ATTACHMENTS
    // =========================================================

    private List<String> attachmentUrls;

    // =========================================================
    // DATES
    // =========================================================

    private Date poCreatedDate;

    private Date poSubmittedForApprovalDate;

    private Date poApprovedDate;

    private Date poReleasedDate;

    // =========================================================
    // AUDIT
    // =========================================================

    private Long createdBy;

    private Long updatedBy;

    private Long approvedBy;

    private Date createdDate;

    private Date updatedDate;
}