package com.doc.service.vendor;

import com.doc.dto.vendor.*;
import com.doc.repository.projection.VendorAssignmentCountProjection;

import java.util.List;

public interface ProductVendorDashboardService {

    ProductVendorDashboardCountDto getProductVendorDashboardCounts(Long productId);
    List<VendorAssignmentCountProjection> getVendorWiseAssignmentCounts(Long productId, Long userId);
    ProductVendorDashboardResponse getDashboardByProductId(
            Long productId,Long userId
    );
    VendorPaymentSummaryResponse getVendorPaymentSummary(
            Long productId,
            Long vendorId,Long userId
    );
    List<ProductRfqDashboardResponse> getRfqDashboard(
            Long productId,Long userId
    );

    ProductVendorVerificationResponse getVendorVerificationByProductId(
            Long productId,Long userId
    );

    ProductQuotationResponseRate getQuotationResponseRate(
            Long productId, Long userId
    );
}