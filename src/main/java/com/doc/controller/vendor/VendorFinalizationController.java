package com.doc.controller.vendor;

import com.doc.dto.vendor.*;
import com.doc.service.vendor.VendorFinalizationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/vendor-finalizations")
@RequiredArgsConstructor
public class VendorFinalizationController {

    private final VendorFinalizationService vendorFinalizationService;

    @GetMapping("/accounts")
    @Operation(summary = "Get all vendor accounts submissions")
    public ResponseEntity<List<VendorAccountsSubmissionResponseDto>> getAllSentToAccounts() {
        return ResponseEntity.ok(vendorFinalizationService.getAllSentToAccounts());
    }

    @PostMapping
    @Operation(summary = "Finalize vendor for RFQ quotation item")
    public ResponseEntity<VendorFinalizationResponseDto> createVendorFinalization(
            @Valid @RequestBody VendorFinalizationRequestDto requestDto
    ) {
        return new ResponseEntity<>(
                vendorFinalizationService.createVendorFinalization(requestDto),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vendor finalization by ID")
    public ResponseEntity<VendorFinalizationResponseDto> getVendorFinalizationById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                vendorFinalizationService.getVendorFinalizationById(id)
        );
    }

    @GetMapping("/rfq/{rfqId}")
    @Operation(summary = "Get all vendor finalizations by RFQ ID")
    public ResponseEntity<List<VendorFinalizationResponseDto>> getVendorFinalizationsByRfqId(
            @PathVariable Long rfqId
    ) {
        return ResponseEntity.ok(
                vendorFinalizationService.getVendorFinalizationsByRfqId(rfqId)
        );
    }

    @PutMapping("/{finalizationId}/send-to-accounts")
    @Operation(summary = "Send finalized vendor documents to Accounts")
    public ResponseEntity<VendorAccountsSubmissionResponseDto> sendToAccounts(
            @PathVariable Long finalizationId,
            @Valid @RequestBody VendorAccountsSubmissionRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                vendorFinalizationService.sendToAccounts(finalizationId, requestDto)
        );
    }

    @PutMapping("/accounts/{submissionId}/approve")
    @Operation(summary = "Approve vendor accounts submission")
    public ResponseEntity<VendorAccountsSubmissionResponseDto> approveByAccounts(
            @PathVariable Long submissionId,
            @Valid @RequestBody AccountsVendorFinalizationRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                vendorFinalizationService.approveByAccounts(submissionId, requestDto)
        );
    }

    @PutMapping("/accounts/{submissionId}/reject")
    @Operation(summary = "Reject vendor accounts submission")
    public ResponseEntity<VendorAccountsSubmissionResponseDto> rejectByAccounts(
            @PathVariable Long submissionId,
            @Valid @RequestBody AccountsVendorFinalizationRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                vendorFinalizationService.rejectByAccounts(submissionId, requestDto)
        );
    }
}