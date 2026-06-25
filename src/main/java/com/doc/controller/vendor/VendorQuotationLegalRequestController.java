package com.doc.controller.vendor;

import com.doc.dto.vendor.VendorAgreementDecisionRequestDto;
import com.doc.dto.vendor.VendorAgreementPrepareRequestDto;
import com.doc.dto.vendor.VendorQuotationLegalRequestDto;
import com.doc.dto.vendor.VendorQuotationLegalResponseDto;
import com.doc.service.vendor.VendorQuotationLegalRequestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/vendor-quotation-legal-requests")
@RequiredArgsConstructor
public class VendorQuotationLegalRequestController {

    private final VendorQuotationLegalRequestService legalRequestService;

    @GetMapping
    @Operation(summary = "Get all vendor quotation legal requests")
    public ResponseEntity<List<VendorQuotationLegalResponseDto>> getAllLegalRequests(
            @RequestParam(required = false) Long assignedToLegal
    ) {
        List<VendorQuotationLegalResponseDto> response =
                legalRequestService.getAllLegalRequests(assignedToLegal);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create legal request for vendor quotation")
    public ResponseEntity<VendorQuotationLegalResponseDto> createLegalRequest(
            @Valid @RequestBody VendorQuotationLegalRequestDto requestDto
    ) {
        VendorQuotationLegalResponseDto response =
                legalRequestService.createLegalRequest(requestDto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @PutMapping("/{id}/prepare-agreement")
    public ResponseEntity<VendorQuotationLegalResponseDto> prepareAgreement(
            @PathVariable Long id,
            @RequestBody VendorAgreementPrepareRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                legalRequestService.prepareAgreement(id, requestDto)
        );
    }

    @PutMapping("/{id}/send-to-operation")
    public ResponseEntity<VendorQuotationLegalResponseDto> sendToOperation(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                legalRequestService.sendAgreementToOperation(id, userId)
        );
    }

    @PutMapping("/{id}/send-to-vendor")
    public ResponseEntity<VendorQuotationLegalResponseDto> sendToVendor(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                legalRequestService.sendAgreementToVendor(id, userId)
        );
    }

    @PutMapping("/{id}/decision")
    public ResponseEntity<VendorQuotationLegalResponseDto> agreementDecision(
            @PathVariable Long id,
            @RequestBody VendorAgreementDecisionRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                legalRequestService.agreementDecision(id, requestDto)
        );
    }
}