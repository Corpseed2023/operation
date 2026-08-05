package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPaymentSummaryResponseDto {

    /*
     * Total amount already released to vendors.
     */
    private BigDecimal paymentGivenAmount;

    /*
     * Total amount still pending for payment.
     *
     * Includes payment requests having status:
     * PENDING
     * UNDER_REVIEW
     * APPROVED
     * PAYMENT_PROCESSING
     * ON_HOLD
     */
    private BigDecimal pendingPaymentAmount;

    /*
     * Number of payment requests having
     * PAYMENT_RELEASED status.
     */
    private Long paymentReleasedCount;

    /*
     * Number of payment requests that are
     * pending, under review, approved,
     * processing, or on hold.
     */
    private Long pendingPaymentCount;

    /*
     * paymentGivenAmount + pendingPaymentAmount
     */
    private BigDecimal totalPaymentAmount;

    /*
     * paymentReleasedCount + pendingPaymentCount
     */
    private Long totalPaymentCount;
}