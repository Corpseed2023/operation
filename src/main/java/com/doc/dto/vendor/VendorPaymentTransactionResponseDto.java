package com.doc.dto.vendor;

import com.doc.entity.vendor.PaymentRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
public class VendorPaymentTransactionResponseDto {

    private Long paymentRequestId;

    private Long vendorId;
    private String vendorName;

    private Long procurementOrderId;
    private String purchaseOrderNumber;

    private String invoiceNumber;
    private LocalDate invoiceDate;

    private BigDecimal taxableAmount;
    private BigDecimal gstAmount;
    private BigDecimal invoiceAmount;

    private BigDecimal tdsAmount;

    /**
     * Actual amount transferred to vendor Bank/Cash.
     */
    private BigDecimal amountPaidToVendor;

    /**
     * amountPaidToVendor + tdsAmount.
     */
    private BigDecimal settlementAmount;

    private LocalDate paymentDate;
    private String paymentMode;
    private String transactionReference;
    private String paymentProof;

    private PaymentRequestStatus status;

    private Long paymentReleasedBy;
    private Date paymentReleasedDate;
}