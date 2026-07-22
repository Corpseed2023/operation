package com.doc.dto.vendor;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVendorDashboardResponse {

    private Long productId;

    /**
     * Vendors mapped with this product.
     */
    private Long registeredVendorCount;

    /**
     * Active RFQs for this product.
     */
    private Long activeRfqCount;

    /**
     * Vendor assignments against which quotation
     * has been received.
     */
    private Long quotationReceivedCount;

    /**
     * RFQs having quotations from at least
     * two different vendors.
     */
    private Long priceComparisonCount;

    /**
     * RFQ vendor assignments having SELECTED status.
     */
    private Long vendorSelectedCount;
}
