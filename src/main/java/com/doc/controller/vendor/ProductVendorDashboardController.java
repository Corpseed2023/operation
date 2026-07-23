package com.doc.controller.vendor;

import com.doc.dto.vendor.*;
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

    @GetMapping("/{productId}/vendor-wise-assignment-count")
    public ResponseEntity<List<VendorAssignmentCountProjection>>
    getVendorWiseAssignmentCounts(@PathVariable Long productId) {

        return ResponseEntity.ok(
                productVendorDashboardService.getVendorWiseAssignmentCounts(productId)
        );
    }
    @GetMapping("/product/{productId}")
    public ResponseEntity<ProductVendorDashboardResponse>
    getDashboardByProductId(
            @PathVariable Long productId
    ) {
        ProductVendorDashboardResponse response =
                productVendorDashboardService
                        .getDashboardByProductId(productId);

        return ResponseEntity.ok(response);
    }

    @GetMapping(
            "/product/vendor/payment-summary"
    )
    public ResponseEntity<VendorPaymentSummaryResponse>
    getVendorPaymentSummary(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long vendorId
    ) {
        return ResponseEntity.ok(
                productVendorDashboardService
                        .getVendorPaymentSummary(
                                productId,
                                vendorId
                        ));
    }



    @GetMapping(value = "/rfqs")
    public ResponseEntity<List<ProductRfqDashboardResponse>> getRfqDashboard(
            @RequestParam(required = false) Long productId){
        List<ProductRfqDashboardResponse> response =
                productVendorDashboardService
                        .getRfqDashboard(productId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/verification/by-product/{productId}")
    public ResponseEntity<ProductVendorVerificationResponse>
    getVendorVerificationByProductId(
            @PathVariable Long productId
    ) {

        return ResponseEntity.ok(
                productVendorDashboardService
                        .getVendorVerificationByProductId(productId)
        );
    }
}