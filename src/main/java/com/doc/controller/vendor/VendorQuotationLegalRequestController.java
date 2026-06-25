package com.doc.controller.vendor;

import com.doc.dto.vendor.SendAgreementToProcurementRequestDto;
import com.doc.dto.vendor.VendorAgreementDecisionRequestDto;
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
        return ResponseEntity.ok(
                legalRequestService.getAllLegalRequests(assignedToLegal)
        );
    }

    @PostMapping
    @Operation(summary = "Create legal request for vendor quotation")
    public ResponseEntity<VendorQuotationLegalResponseDto> createLegalRequest(
            @Valid @RequestBody VendorQuotationLegalRequestDto requestDto
    ) {
        return new ResponseEntity<>(
                legalRequestService.createLegalRequest(requestDto),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}/send-to-procurement")
    @Operation(summary = "Upload agreement PDF and send to procurement")
    public ResponseEntity<VendorQuotationLegalResponseDto> sendToProcurement(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody SendAgreementToProcurementRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                legalRequestService.sendAgreementToProcurement(id, userId, requestDto)
        );
    }

    @PutMapping("/{id}/decision")
    @Operation(summary = "Procurement marks agreement agreed or disagreed")
    public ResponseEntity<VendorQuotationLegalResponseDto> agreementDecision(
            @PathVariable Long id,
            @Valid @RequestBody VendorAgreementDecisionRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                legalRequestService.agreementDecision(id, requestDto)
        );
    }
}