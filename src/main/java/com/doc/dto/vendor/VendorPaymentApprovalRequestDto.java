package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Internal Operation Service -> Account Service DTO.
 *
 * Used ONLY for procurement vendor PAYMENT RELEASE.
 *
 * This is not an invoice creation request.
 *
 * Operation calculates the payment/tax snapshot.
 * Account validates it and posts the PAYMENT voucher.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPaymentApprovalRequestDto {

    /*
     * ================================================================
     * SOURCE REFERENCES
     * ================================================================
     */

    private Long procurementPaymentRequestId;

    private Long procurementOrderId;

    private String purchaseOrderNumber;


    /*
     * ================================================================
     * BASIC / TAXABLE AMOUNT
     * ================================================================
     */

    /**
     * Taxable/basic procurement amount before GST.
     */
    private BigDecimal price;

    /**
     * Same basic amount represented explicitly
     * for snapshot validation.
     *
     * taxableAmount == price
     */
    private BigDecimal taxableAmount;


    /*
     * ================================================================
     * GST CONFIGURATION
     * ================================================================
     */

    private String gstRegistrationType;

    private Boolean gstActive;

    /**
     * INTRA_STATE / INTER_STATE
     */
    private String gstSupplyType;

    private String gstStateCode;

    private BigDecimal gstPercentage;


    /*
     * ================================================================
     * GST CALCULATION SNAPSHOT
     * ================================================================
     */

    private BigDecimal cgstAmount;

    private BigDecimal sgstAmount;

    private BigDecimal igstAmount;

    private BigDecimal totalGstAmount;


    /*
     * ================================================================
     * GROSS LIABILITY
     * ================================================================
     */

    /**
     * taxableAmount + totalGstAmount
     */
    private BigDecimal invoiceGrossAmount;


    /*
     * ================================================================
     * TDS
     * ================================================================
     */

    private Boolean tdsActive;

    /**
     * Current flow:
     *
     * tdsBaseAmount == taxableAmount
     */
    private BigDecimal tdsBaseAmount;

    private BigDecimal tdsPercentage;

    private BigDecimal tdsAmount;

    /**
     * Account resolves TDS Payable ledger.
     * Normally null from Operation.
     */
    private Long tdsPayableLedgerId;


    /*
     * ================================================================
     * VENDOR PAYABLE
     * ================================================================
     */

    /**
     * invoiceGrossAmount - tdsAmount
     */
    private BigDecimal vendorNetPayableAmount;


    /*
     * ================================================================
     * SETTLEMENT
     * ================================================================
     */

    /**
     * bankPaymentAmount + tdsAmount
     */
    private BigDecimal settlementAmount;


    /*
     * ================================================================
     * PAYMENT RELEASE
     * ================================================================
     */

    private LocalDate paymentDate;

    /**
     * Actual Bank/Cash amount paid to vendor.
     *
     * Current full-settlement flow:
     *
     * bankPaymentAmount == vendorNetPayableAmount
     */
    private BigDecimal bankPaymentAmount;

    private String paymentMode;

    /**
     * BANK / CASH / PAYMENT_GATEWAY ledger
     * in Account Service.
     */
    private Long bankLedgerId;


    /*
     * ================================================================
     * OPTIONAL LEGACY LEDGER INFORMATION
     * ================================================================
     */

    private Long ledgerId;

    private String ledgerType;


    /*
     * ================================================================
     * TRANSACTION / PROOF
     * ================================================================
     */

    private String transactionReference;

    private String paymentProof;

    private List<String> proofAttachmentUrls;


    /*
     * ================================================================
     * APPROVAL / RELEASE AUDIT
     * ================================================================
     */

    private Long approvedByOperationUserId;

    private LocalDate approvedDate;

    private String approvalComment;

    private Long paymentReleasedByOperationUserId;

    private LocalDate paymentReleasedDate;

    private String releaseComment;


    /*
     * ================================================================
     * CONTRACT VERSION
     * ================================================================
     */

    private String calculationVersion;
}