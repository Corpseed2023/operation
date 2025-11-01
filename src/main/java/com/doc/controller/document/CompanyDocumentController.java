package com.doc.controller.document;


import com.doc.dto.document.CompanyDocCheckResponseDto;
import com.doc.dto.document.CompanyDocumentResponseDto;
import com.doc.dto.document.CompanyDocumentStatusUpdateDto;
import com.doc.dto.document.CompanyDocumentUploadRequestDto;
import com.doc.service.CompanyDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/companies/{companyId}/documents")
@Validated
public class CompanyDocumentController {

    @Autowired
    private CompanyDocumentService companyDocumentService;

    // 1. Upload new / replace company-level doc
    @PostMapping
    @Operation(summary = "Upload company document (CRT uploads, Manager verifies later)",
            description = "Supports FIXED/EXPIRING. Expiry required only for EXPIRING docs.")
    public ResponseEntity<CompanyDocumentResponseDto> uploadCompanyDocument(
            @Parameter(description = "Company ID") @PathVariable Long companyId,
            @Valid @RequestBody CompanyDocumentUploadRequestDto request) {
        request.setCompanyId(companyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyDocumentService.uploadCompanyDocument(request));
    }

    // 2. Verify / Reject company doc (Manager only)
    @PutMapping("/{docId}/status")
    @Operation(summary = "Verify or Reject company document (Manager/Verifier)",
            description = "On VERIFY: auto-copies expiry & sets quality score")
    public ResponseEntity<CompanyDocumentResponseDto> updateCompanyDocumentStatus(
            @Parameter(description = "Company ID") @PathVariable Long companyId,
            @Parameter(description = "Document ID") @PathVariable Long docId,
            @Valid @RequestBody CompanyDocumentStatusUpdateDto updateDto) {
        return ResponseEntity.ok(companyDocumentService.updateStatus(docId, updateDto));
    }

    // 3. Get all verified & reusable docs for company
    @GetMapping("/verified")
    @Operation(summary = "Get all VERIFIED reusable docs for company",
            description = "Only non-expired, verified, validated docs")
    public ResponseEntity<List<CompanyDocumentResponseDto>> getVerifiedCompanyDocuments(
            @Parameter(description = "Company ID") @PathVariable Long companyId) {
        return ResponseEntity.ok(companyDocumentService.getVerifiedDocuments(companyId));
    }

    // 4. Check if doc is available for reuse in project
    @GetMapping("/check")
    @Operation(summary = "Check if required doc is verified & reusable",
            description = "Returns expiry status, days left, validation")
    public ResponseEntity<CompanyDocCheckResponseDto> checkDocumentAvailability(
            @Parameter(description = "Company ID") @PathVariable Long companyId,
            @Parameter(description = "Required Document ID") @RequestParam Long requiredDocumentId) {
        return ResponseEntity.ok(companyDocumentService.checkAvailability(companyId, requiredDocumentId));
    }
}