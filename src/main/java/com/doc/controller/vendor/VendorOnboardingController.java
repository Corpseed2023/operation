package com.doc.controller.vendor;

import com.doc.dto.vendor.VendorOnboardingResponseDto;
import com.doc.dto.vendor.VendorOnboardingSendFormRequestDto;
import com.doc.service.vendor.VendorOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operationService/api/vendor-onboarding")
@RequiredArgsConstructor
public class VendorOnboardingController {

    private final VendorOnboardingService vendorOnboardingService;

    @PostMapping("/send-form/{vendorFinalizationId}")
    @Operation(summary = "Send vendor onboarding form after vendor finalization")
    public ResponseEntity<VendorOnboardingResponseDto> sendOnboardingForm(
            @PathVariable Long vendorFinalizationId,
            @Valid @RequestBody VendorOnboardingSendFormRequestDto requestDto
    ) {
        VendorOnboardingResponseDto response =
                vendorOnboardingService.sendOnboardingForm(vendorFinalizationId, requestDto);

        return ResponseEntity.ok(response);
    }
}