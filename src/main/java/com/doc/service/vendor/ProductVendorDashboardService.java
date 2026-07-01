package com.doc.service.vendor;

import com.doc.dto.vendor.ProductVendorDashboardCountDto;

public interface ProductVendorDashboardService {

    ProductVendorDashboardCountDto getProductVendorDashboardCounts(Long productId);
}