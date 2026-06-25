package com.doc.controller.vendor;

import com.doc.dto.vendor.*;
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

    @PutMapping("/{id}/send-to-procurement")
    public ResponseEntity<VendorQuotationLegalResponseDto> sendToProcurement(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody SendAgreementToProcurementRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                legalRequestService.sendAgreementToProcurement(id, userId, requestDto)
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