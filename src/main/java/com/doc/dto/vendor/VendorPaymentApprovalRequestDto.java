package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPaymentApprovalRequestDto {

    /*
     * Operation Service payment-request ID.
     *
     * Account Service uses this as the idempotent
     * voucher source ID.
     */
    private Long procurementPaymentRequestId;

    /*
     * Procurement order details.
     */
    private Long procurementOrderId;

    private String purchaseOrderNumber;

    /*
     * Vendor invoice details.
     */
    private String invoiceNumber;

    private LocalDate invoiceDate;

    /*
     * Basic price before GST.
     */
    private BigDecimal price;

    /*
     * Vendor GST registration category:
     *
     * REGISTERED
     * UNREGISTERED
     * SEZ
     * INTERNATIONAL
     */
    private String gstRegistrationType;

    /*
     * GST supply category:
     *
     * INTRA_STATE
     * INTER_STATE
     *
     * Not required for SEZ or INTERNATIONAL because
     * those registration types are zero-rated.
     */
    private String gstSupplyType;

    private String gstStateCode;

    private BigDecimal gstPercentage;

    /*
     * TDS calculation inputs.
     */
    private Boolean tdsActive;

    private BigDecimal tdsPercentage;

    /*
     * Approval metadata.
     */
    private Long approvedByOperationUserId;

    private LocalDate approvedDate;

    private String approvalComment;
}