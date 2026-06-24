package com.doc.controller.vendor;

import com.doc.dto.vendor.VendorFinalizationRequestDto;
import com.doc.dto.vendor.VendorFinalizationResponseDto;
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

    @PostMapping
    @Operation(summary = "Finalize vendor for RFQ quotation item")
    public ResponseEntity<VendorFinalizationResponseDto> createVendorFinalization(
            @Valid @RequestBody VendorFinalizationRequestDto requestDto
    ) {
        VendorFinalizationResponseDto response =
                vendorFinalizationService.createVendorFinalization(requestDto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vendor finalization by ID")
    public ResponseEntity<VendorFinalizationResponseDto> getVendorFinalizationById(
            @PathVariable Long id
    ) {
        VendorFinalizationResponseDto response =
                vendorFinalizationService.getVendorFinalizationById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/rfq/{rfqId}")
    @Operation(summary = "Get all vendor finalizations by RFQ ID")
    public ResponseEntity<List<VendorFinalizationResponseDto>> getVendorFinalizationsByRfqId(
            @PathVariable Long rfqId
    ) {
        List<VendorFinalizationResponseDto> response =
                vendorFinalizationService.getVendorFinalizationsByRfqId(rfqId);

        return ResponseEntity.ok(response);
    }
}
