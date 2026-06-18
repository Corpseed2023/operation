package com.doc.controller.vendor;

import com.doc.dto.vendor.ProductVendorCreateRequestDto;
import com.doc.dto.vendor.ProductVendorResponseDto;
import com.doc.dto.vendor.ProductVendorUpdateRequestDto;
import com.doc.service.vendor.ProductVendorService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operationService/api/products")
public class ProductVendorController {

    private final ProductVendorService productVendorService;

    public ProductVendorController(ProductVendorService productVendorService) {
        this.productVendorService = productVendorService;
    }

    @PostMapping("/{productId}/vendors")
    @Operation(summary = "Create or map vendor against product")
    public ResponseEntity<ProductVendorResponseDto> createVendorAgainstProduct(
            @PathVariable Long productId,
            @RequestParam  Long userId,
            @RequestBody ProductVendorCreateRequestDto dto
    ) {
        ProductVendorResponseDto response =
                productVendorService.createVendorAgainstProduct(productId, userId, dto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{productId}/vendors")
    @Operation(summary = "Get vendors mapped with product")
    public ResponseEntity<Page<ProductVendorResponseDto>> getVendorsByProduct(
            @PathVariable Long productId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ProductVendorResponseDto> response =
                productVendorService.getVendorsByProduct(productId, userId, page, size);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/vendors/{mappingId}")
    @Operation(summary = "Update product vendor mapping")
    public ResponseEntity<ProductVendorResponseDto> updateProductVendorMapping(
            @PathVariable Long mappingId,
            @RequestParam Long userId,
            @RequestBody ProductVendorUpdateRequestDto dto
    ) {
        ProductVendorResponseDto response =
                productVendorService.updateProductVendorMapping(mappingId, userId, dto);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/vendors/{mappingId}")
    @Operation(summary = "Remove vendor from product")
    public ResponseEntity<Void> removeVendorFromProduct(
            @PathVariable Long mappingId,
            @RequestParam Long userId
    ) {
        productVendorService.removeVendorFromProduct(mappingId, userId);
        return ResponseEntity.noContent().build();
    }








}