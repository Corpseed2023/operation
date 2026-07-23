package com.doc.service.vendor;

import com.doc.dto.vendor.*;
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
    List<ProductRfqDashboardResponse> getRfqDashboard(
            Long productId
    );

    ProductVendorVerificationResponse getVendorVerificationByProductId(
            Long productId
    );
}