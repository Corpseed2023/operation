package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Payload sent to Account Service inside AccountVendorSyncRequestDto.
 *
 * Account Service is responsible for calculating:
 *
 * price
 * + GST
 * - TDS
 * = vendor net payable
 *
 * and creating the PURCHASE_INVOICE voucher.
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

    /**
     * Basic / taxable purchase value.
     *
     * Account Service expects this field as `price`.
     */
    private BigDecimal price;

    private String gstRegistrationType;

    /**
     * INTRA_STATE or INTER_STATE.
     */
    private String gstSupplyType;

    private String gstStateCode;

    private BigDecimal gstPercentage;

    private Boolean tdsActive;

    private BigDecimal tdsPercentage;

    private Long approvedByOperationUserId;

    private LocalDate approvedDate;

    private String approvalComment;

    private BigDecimal tdsAmount;
    private Long tdsPayableLedgerId;

    private LocalDate paymentDate;
    private BigDecimal bankPaymentAmount;
    private String paymentMode;
    private Long bankLedgerId;
    private Long ledgerId;
    private String ledgerType;
    private String transactionReference;
    private String paymentProof;
    private List<String> proofAttachmentUrls;

    private Long paymentReleasedByOperationUserId;
    private LocalDate paymentReleasedDate;
    private String releaseComment;
}