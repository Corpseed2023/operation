package com.doc.controller.vendor;

import com.doc.dto.vendor.*;
import com.doc.entity.vendor.RFQStatus;
import com.doc.service.vendor.VendorRFQService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/rfq")
public class RFQController {

    @Autowired
    private VendorRFQService vendorRFQService;

    @GetMapping
    @Operation(summary = "Get All RFQs")
    public ResponseEntity<Page<RFQResponseDto>> getAllRFQs(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) RFQStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<RFQResponseDto> response =
                vendorRFQService.getAllRFQs(productId, status, userId, page, size);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create RFQ")
    public ResponseEntity<RFQResponseDto> createRFQ(
            @RequestParam Long userId,
            @Valid @RequestBody RFQCreateRequestDto requestDto
    ) {
        RFQResponseDto response = vendorRFQService.createRFQ(userId, requestDto);
        return ResponseEntity.ok(response);
    }
    @PutMapping("/{rfqId}")
    @Operation(summary = "Update RFQ and vendor mapping")
    public ResponseEntity<RFQResponseDto> updateRFQ(
            @PathVariable Long rfqId,
            @RequestParam Long userId,
            @Valid @RequestBody RFQUpdateRequestDto requestDto
    ) {
        RFQResponseDto response = vendorRFQService.updateRFQ(
                rfqId,
                userId,
                requestDto
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{rfqId}/send-to-vendors")
    @Operation(summary = "Send RFQ mail to mapped vendors")
    public ResponseEntity<RFQResponseDto> sendRFQToVendors(
            @PathVariable Long rfqId,
            @RequestParam Long userId,
            @RequestBody RFQSendMailRequestDto requestDto
    ) {
        RFQResponseDto response = vendorRFQService.sendRFQToVendors(
                rfqId,
                userId,
                requestDto
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rfqId}/vendors")
    public ResponseEntity<List<RFQVendorResponseDto>> getVendorsByRfqId(
            @PathVariable Long rfqId
    ) {
        List<RFQVendorResponseDto> response =
                vendorRFQService.getVendorsByRfqId(rfqId);

        return ResponseEntity.ok(response);
    }


}
