package com.doc.service.vendor;

import com.doc.dto.vendor.VendorOnboardingResponseDto;
import com.doc.dto.vendor.VendorOnboardingSendFormRequestDto;

public interface VendorOnboardingService {

    VendorOnboardingResponseDto sendOnboardingForm(
            Long vendorFinalizationId,
            VendorOnboardingSendFormRequestDto requestDto
    );
}