package com.doc.repository.projection;

public interface ProductVendorDashboardProjection {
    Long getRegisteredVendorCount();

    Long getActiveRfqCount();

    Long getQuotationReceivedCount();

    Long getPriceComparisonCount();

    Long getVendorSelectedCount();
}
