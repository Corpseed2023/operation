package com.doc.service.vendor;

import com.doc.dto.vendor.SendAgreementToVendorRequestDto;
import com.doc.dto.vendor.VendorQuotationRequestDto;
import com.doc.dto.vendor.VendorQuotationResponseDto;

import java.util.List;

public interface VendorQuotationService {

    VendorQuotationResponseDto createVendorQuotation(VendorQuotationRequestDto requestDto);

    List<VendorQuotationResponseDto> getVendorQuotationsByRfqId(Long rfqId);

    VendorQuotationResponseDto getVendorQuotationById(Long id);

    VendorQuotationResponseDto updateVendorQuotation(Long id, VendorQuotationRequestDto requestDto);


    List<VendorQuotationResponseDto> getVendorQuotationsByVendorId(Long vendorId);

    VendorQuotationResponseDto sendAgreementToVendor(
            Long quotationId,
            Long userId,
            SendAgreementToVendorRequestDto requestDto
    );
}
