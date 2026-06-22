package com.doc.controller.vendor;

import com.doc.dto.vendor.VendorQuotationRequestDto;
import com.doc.dto.vendor.VendorQuotationResponseDto;
import com.doc.service.vendor.VendorQuotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operationService/api/quotation")
@RequiredArgsConstructor
public class VendorQuotationController {

    private final VendorQuotationService vendorQuotationService;

    @PostMapping
    public ResponseEntity<VendorQuotationResponseDto> createQuotation(
            @Valid @RequestBody VendorQuotationRequestDto requestDto
    ) {
        VendorQuotationResponseDto response =
                vendorQuotationService.createVendorQuotation(requestDto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}