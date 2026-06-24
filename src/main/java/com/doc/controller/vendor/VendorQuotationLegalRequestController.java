package com.doc.controller.vendor;

import com.doc.dto.vendor.VendorQuotationLegalRequestDto;
import com.doc.dto.vendor.VendorQuotationLegalResponseDto;
import com.doc.service.vendor.VendorQuotationLegalRequestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operationService/api/vendor-quotation-legal-requests")
@RequiredArgsConstructor
public class VendorQuotationLegalRequestController {

    private final VendorQuotationLegalRequestService legalRequestService;

    @PostMapping
    @Operation(summary = "Create legal request for vendor quotation")
    public ResponseEntity<VendorQuotationLegalResponseDto> createLegalRequest(
            @Valid @RequestBody VendorQuotationLegalRequestDto requestDto
    ) {
        VendorQuotationLegalResponseDto response =
                legalRequestService.createLegalRequest(requestDto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}