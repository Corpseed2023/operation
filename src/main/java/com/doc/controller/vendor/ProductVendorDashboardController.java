package com.doc.controller.vendor;

import com.doc.dto.vendor.*;
import com.doc.entity.user.User;
import com.doc.repository.UserRepository;
import com.doc.repository.projection.VendorAssignmentCountProjection;
import com.doc.service.vendor.ProductVendorDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    getVendorWiseAssignmentCounts(@RequestParam("productId") Long productId,
                                  @RequestParam(name = "userId", required = true) Long userId) {

        return ResponseEntity.ok(
                productVendorDashboardService.getVendorWiseAssignmentCounts(productId,userId)
        );
    }
    @GetMapping("/product/{productId}")
    public ResponseEntity<ProductVendorDashboardResponse>
    getDashboardByProductId(
            @RequestParam("productId") Long productId,
            @RequestParam(name = "userId", required = true) Long userId
    ) {
        ProductVendorDashboardResponse response =
                productVendorDashboardService
                        .getDashboardByProductId(productId,userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping(
            "/product/vendor/payment-summary"
    )
    public ResponseEntity<VendorPaymentSummaryResponse>
    getVendorPaymentSummary(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(name = "userId", required = true) Long userId
    ) {
        return ResponseEntity.ok(
                productVendorDashboardService
                        .getVendorPaymentSummary(
                                productId,
                                vendorId,userId
                        ));
    }



    @GetMapping(value = "/rfqs")
    public ResponseEntity<List<ProductRfqDashboardResponse>> getRfqDashboard(
            @RequestParam(required = false) Long productId ,
            @RequestParam(name = "userId", required = true) Long userId){
        List<ProductRfqDashboardResponse> response =
                productVendorDashboardService
                        .getRfqDashboard(productId,userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/verification/by-product/{productId}")
    public ResponseEntity<ProductVendorVerificationResponse>
    getVendorVerificationByProductId(
            @RequestParam(required = false) Long productId ,
            @RequestParam(name = "userId", required = true) Long userId
    ) {

        return ResponseEntity.ok(
                productVendorDashboardService
                        .getVendorVerificationByProductId(productId,userId)
        );
    }

    @GetMapping("/quotation-response-rate")
    public ResponseEntity<ProductQuotationResponseRate>
    getQuotationResponseRate(
            @RequestParam("productId") Long productId,
            @RequestParam(name = "userId", required = true) Long userId
    ) {

        return ResponseEntity.ok(
                productVendorDashboardService
                        .getQuotationResponseRate(productId,userId)
        );
    }
}