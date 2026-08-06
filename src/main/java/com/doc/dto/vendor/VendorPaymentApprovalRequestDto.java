package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Immutable procurement calculation snapshot sent to Account Service.
 * Account Service must validate this snapshot before posting vouchers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPaymentApprovalRequestDto {

    private Long procurementPaymentRequestId;
    private Long procurementOrderId;
    private String purchaseOrderNumber;

    private String invoiceNumber;
    private LocalDate invoiceDate;

    /** Taxable/basic purchase amount before GST. */
    private BigDecimal price;
    private BigDecimal taxableAmount;

    private String gstRegistrationType;
    private Boolean gstActive;
    private String gstSupplyType;
    private String gstStateCode;
    private BigDecimal gstPercentage;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal totalGstAmount;

    /** GST-inclusive invoice liability. */
    private BigDecimal invoiceGrossAmount;

    private Boolean tdsActive;
    private BigDecimal tdsBaseAmount;
    private BigDecimal tdsPercentage;
    private BigDecimal tdsAmount;
    private Long tdsPayableLedgerId;

    /** Invoice gross minus TDS. */
    private BigDecimal vendorNetPayableAmount;

    /** Bank payment plus TDS. Equals invoice gross for this workflow. */
    private BigDecimal settlementAmount;

    private LocalDate paymentDate;
    private BigDecimal bankPaymentAmount;
    private String paymentMode;
    private Long bankLedgerId;

    /** Optional legacy metadata. Account Service resolves vendor ledger by operationVendorId. */
    private Long ledgerId;
    private String ledgerType;

    private String transactionReference;
    private String paymentProof;
    private List<String> proofAttachmentUrls;

    private Long approvedByOperationUserId;
    private LocalDate approvedDate;
    private String approvalComment;

    private Long paymentReleasedByOperationUserId;
    private LocalDate paymentReleasedDate;
    private String releaseComment;

    private String calculationVersion;
}
