package com.doc.controller.vendor;

import com.doc.dto.vendor.VendorRequestDto;
import com.doc.dto.vendor.VendorResponseDto;
import com.doc.service.vendor.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/vendors")
public class VendorController {


    @Autowired
    private VendorService vendorService;

    @PostMapping
    public ResponseEntity<VendorResponseDto> createVendor(
            @RequestParam Long userId,
            @RequestBody VendorRequestDto dto) {

        VendorResponseDto response = vendorService.createVendor(userId, dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Vendor")
    public ResponseEntity<VendorResponseDto> updateVendor(
            @PathVariable Long id,
            @RequestParam @Parameter(description = "User ID who is updating this vendor (for audit)") Long userId,
            @RequestBody VendorRequestDto dto) {

        VendorResponseDto response = vendorService.updateVendor(id, userId, dto);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get Vendor by ID")
    public ResponseEntity<VendorResponseDto> getVendorById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }


    @GetMapping("/{id}/details")
    @Operation(summary = "Get Vendor Details, RFQs, Forms Etc By id")
    public ResponseEntity<VendorResponseDto> getVendorDetailsById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getVendorDetailsById(id));
    }

    @GetMapping
    @Operation(summary = "Get all vendors with pagination and search")
    public ResponseEntity<Page<VendorResponseDto>> getAllVendors(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        Page<VendorResponseDto> vendors = vendorService.getAllVendors(userId, page, size, keyword);
        return ResponseEntity.ok(vendors);
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete vendor")
    public ResponseEntity<Void> deleteVendor(
            @PathVariable Long id,
            @RequestParam @Parameter(description = "User ID who is deleting this vendor") Long userId) {

        vendorService.deleteVendor(id);
        return ResponseEntity.noContent().build();
    }


}