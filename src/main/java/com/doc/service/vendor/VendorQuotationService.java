package com.doc.service.vendor;

import com.doc.dto.vendor.VendorQuotationRequestDto;
import com.doc.dto.vendor.VendorQuotationResponseDto;

public interface VendorQuotationService {

    VendorQuotationResponseDto createVendorQuotation(VendorQuotationRequestDto requestDto);

}
