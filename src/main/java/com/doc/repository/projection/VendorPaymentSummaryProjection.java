package com.doc.repository.projection;

import java.math.BigDecimal;

public interface VendorPaymentSummaryProjection {
    BigDecimal getPaymentGivenAmount();

    BigDecimal getPendingPaymentAmount();

    Long getPaymentReleasedCount();

    Long getPendingPaymentCount();
}
