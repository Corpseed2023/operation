package com.doc.controller.vendor;

import com.doc.dto.vendor.*;
import com.doc.entity.vendor.VendorRestrictionRequestStatus;
import com.doc.entity.vendor.VendorStatus;
import com.doc.exception.ValidationException;
import com.doc.service.vendor.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
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
    public ResponseEntity<Page<VendorResponseDto>> getAllVendors(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {

        VendorStatus vendorStatus = null;

        if (status != null && !status.trim().isEmpty()) {
            try {
                vendorStatus = VendorStatus.valueOf(
                        status.trim().toUpperCase()
                );
            } catch (IllegalArgumentException ex) {
                throw new ValidationException(
                        "Invalid vendor status: " + status,
                        "ERR_INVALID_VENDOR_STATUS"
                );
            }
        }

        Page<VendorResponseDto> vendors =
                vendorService.getAllVendors(
                        userId,
                        page,
                        size,
                        keyword,
                        vendorStatus
                );

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

    @PostMapping("/{id}/restrict")
    @Operation(summary = "Create suspension/blacklist request for an active vendor")
    public ResponseEntity<VendorRestrictionResponseDto> addRestrictionsToVendor(
            @PathVariable Long id,
            @RequestParam
            @Parameter(description = "User ID creating the restriction request")
            Long userId,
            @RequestBody VendorRestrictionRequestDto dto) {

        VendorRestrictionResponseDto response =
                vendorService.restrictVendor(id, userId, dto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/restriction-requests/accounts")
    @Operation(
            summary = "Get vendor restriction requests for Accounts with status filter"
    )
    public ResponseEntity<Page<VendorRestrictionResponseDto>>
    getAccountsRestrictionRequests(

            @RequestParam
            @Parameter(description = "Accounts user ID")
            Long userId,

            @RequestParam(defaultValue = "1")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(required = false)
            @Parameter(
                    description = """
                        Optional status filter. Default is PENDING_ACCOUNTS.
                        Allowed: PENDING_ACCOUNTS, ACCOUNTS_REJECTED,
                        PENDING_ADMIN, ADMIN_REJECTED, FINAL_APPROVED
                        """
            )
            String status) {

        VendorRestrictionRequestStatus requestStatus =
                parseRestrictionStatus(status);

        Page<VendorRestrictionResponseDto> response =
                vendorService.getAccountsRestrictionRequests(
                        userId,
                        page,
                        size,
                        requestStatus
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/restriction-requests/admin")
    @Operation(
            summary = "Get vendor restriction requests for Admin with status filter"
    )
    public ResponseEntity<Page<VendorRestrictionResponseDto>>
    getAdminRestrictionRequests(

            @RequestParam
            @Parameter(description = "Admin user ID")
            Long userId,

            @RequestParam(defaultValue = "1")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(required = false)
            @Parameter(
                    description = """
                        Optional status filter. Default is PENDING_ADMIN.
                        Allowed: PENDING_ADMIN, ADMIN_REJECTED, FINAL_APPROVED
                        """
            )
            String status) {

        VendorRestrictionRequestStatus requestStatus =
                parseRestrictionStatus(status);

        Page<VendorRestrictionResponseDto> response =
                vendorService.getAdminRestrictionRequests(
                        userId,
                        page,
                        size,
                        requestStatus
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/restriction-requests/{requestId}/accounts-review")
    @Operation(summary = "Approve or reject vendor restriction request by Accounts")
    public ResponseEntity<VendorRestrictionResponseDto>
    reviewVendorRestrictionByAccounts(

            @PathVariable Long requestId,

            @RequestParam
            @Parameter(description = "Accounts user ID reviewing the request")
            Long userId,

            @Valid
            @RequestBody VendorRestrictionAccountsReviewDto dto) {

        VendorRestrictionResponseDto response =
                vendorService.reviewRestrictionByAccounts(
                        requestId,
                        userId,
                        dto
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/restriction-requests/{requestId}/admin-review")
    @Operation(
            summary = "Final approve or reject vendor restriction request by Admin"
    )
    public ResponseEntity<VendorRestrictionResponseDto>
    reviewVendorRestrictionByAdmin(

            @PathVariable Long requestId,

            @RequestParam
            @Parameter(description = "Admin user ID reviewing the request")
            Long userId,

            @Valid
            @RequestBody VendorRestrictionAdminReviewDto dto) {

        VendorRestrictionResponseDto response =
                vendorService.reviewRestrictionByAdmin(
                        requestId,
                        userId,
                        dto
                );

        return ResponseEntity.ok(response);
    }

    private VendorRestrictionRequestStatus parseRestrictionStatus(
            String status) {

        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        try {
            return VendorRestrictionRequestStatus.valueOf(
                    status.trim().toUpperCase()
            );

        } catch (IllegalArgumentException exception) {

            throw new ValidationException(
                    "Invalid vendor restriction request status: " + status,
                    "ERR_INVALID_VENDOR_RESTRICTION_STATUS"
            );
        }
    }
}