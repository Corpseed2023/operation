package com.doc.controller.vendor;

import com.doc.dto.vendor.ProductVendorDashboardCountDto;
import com.doc.repository.projection.VendorAssignmentCountProjection;
import com.doc.service.vendor.ProductVendorDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/product-vendor-dashboard")
@RequiredArgsConstructor
public class ProductVendorDashboardController {

    private final ProductVendorDashboardService productVendorDashboardService;

    @GetMapping("/{productId}/summary")
    @Operation(summary = "Get product based vendor dashboard counts")
    public ResponseEntity<ProductVendorDashboardCountDto> getProductVendorDashboardSummary(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                productVendorDashboardService.getProductVendorDashboardCounts(productId)
        );
    }

    @GetMapping("/vendor-wise-assignment-count")
    public ResponseEntity<List<VendorAssignmentCountProjection>>
    getVendorWiseAssignmentCounts() {

        return ResponseEntity.ok(
                productVendorDashboardService.getVendorWiseAssignmentCounts()
        );
    }

}