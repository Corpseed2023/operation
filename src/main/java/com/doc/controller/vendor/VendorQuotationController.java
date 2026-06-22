package com.doc.controller.vendor;

import com.doc.dto.vendor.VendorQuotationRequestDto;
import com.doc.dto.vendor.VendorQuotationResponseDto;
import com.doc.service.vendor.VendorQuotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/quotation")
@RequiredArgsConstructor
public class VendorQuotationController {

    private final VendorQuotationService vendorQuotationService;

    @GetMapping("/rfq/{rfqId}")
    public ResponseEntity<List<VendorQuotationResponseDto>> getQuotationsByRfqId(
            @PathVariable Long rfqId
    ) {
        List<VendorQuotationResponseDto> response =
                vendorQuotationService.getVendorQuotationsByRfqId(rfqId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorQuotationResponseDto> getQuotationById(
            @PathVariable Long id
    ) {
        VendorQuotationResponseDto response =
                vendorQuotationService.getVendorQuotationById(id);

        return ResponseEntity.ok(response);
    }


    @PostMapping
    public ResponseEntity<VendorQuotationResponseDto> createQuotation(
            @Valid @RequestBody VendorQuotationRequestDto requestDto
    ) {
        VendorQuotationResponseDto response =
                vendorQuotationService.createVendorQuotation(requestDto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VendorQuotationResponseDto> updateQuotation(
            @PathVariable Long id,
            @Valid @RequestBody VendorQuotationRequestDto requestDto
    ) {
        VendorQuotationResponseDto response =
                vendorQuotationService.updateVendorQuotation(id, requestDto);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<VendorQuotationResponseDto>> getQuotationsByVendorId(
            @PathVariable Long vendorId
    ) {
        List<VendorQuotationResponseDto> response =
                vendorQuotationService.getVendorQuotationsByVendorId(vendorId);

        return ResponseEntity.ok(response);
    }

}