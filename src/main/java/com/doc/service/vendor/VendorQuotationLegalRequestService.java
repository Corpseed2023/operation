package com.doc.service.vendor;

import com.doc.dto.vendor.VendorQuotationLegalRequestDto;
import com.doc.dto.vendor.VendorQuotationLegalResponseDto;

import java.util.List;

public interface VendorQuotationLegalRequestService {

    VendorQuotationLegalResponseDto createLegalRequest(
            VendorQuotationLegalRequestDto requestDto
    );

    List<VendorQuotationLegalResponseDto> getAllLegalRequests(Long assignedToLegal);

}