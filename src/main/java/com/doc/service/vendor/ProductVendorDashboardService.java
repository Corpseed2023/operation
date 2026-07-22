package com.doc.service.vendor;

import com.doc.dto.vendor.ProductVendorDashboardCountDto;
import com.doc.dto.vendor.ProductVendorDashboardResponse;
import com.doc.dto.vendor.VendorPaymentSummaryResponse;
import com.doc.repository.projection.VendorAssignmentCountProjection;

import java.util.List;

public interface ProductVendorDashboardService {

    ProductVendorDashboardCountDto getProductVendorDashboardCounts(Long productId);
    List<VendorAssignmentCountProjection> getVendorWiseAssignmentCounts(Long productId);
    ProductVendorDashboardResponse getDashboardByProductId(
            Long productId
    );
    VendorPaymentSummaryResponse getVendorPaymentSummary(
            Long productId,
            Long vendorId
    );
}