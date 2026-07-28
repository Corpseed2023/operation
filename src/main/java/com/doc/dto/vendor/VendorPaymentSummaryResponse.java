package com.doc.dto.vendor;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPaymentSummaryResponse {

    private Long productId;

    private Long vendorId;

    private BigDecimal paymentGivenAmount;

    private BigDecimal pendingPaymentAmount;

    private Long paymentReleasedCount;

    private Long pendingPaymentCount;
}
