package com.doc.service.vendor;

import com.doc.dto.vendor.VendorQuotationRequestDto;
import com.doc.dto.vendor.VendorQuotationResponseDto;

import java.util.List;

public interface VendorQuotationService {

    VendorQuotationResponseDto createVendorQuotation(VendorQuotationRequestDto requestDto);

    VendorQuotationResponseDto getVendorQuotationById(Long id);

    List<VendorQuotationResponseDto> getAllVendorQuotations();

    VendorQuotationResponseDto updateVendorQuotation(Long id, VendorQuotationRequestDto requestDto);
}
