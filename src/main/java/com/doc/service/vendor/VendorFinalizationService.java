package com.doc.service.vendor;

import com.doc.dto.vendor.SendFinalVendorToAccountsRequestDto;
import com.doc.dto.vendor.VendorFinalizationRequestDto;
import com.doc.dto.vendor.VendorFinalizationResponseDto;

import java.util.List;

public interface VendorFinalizationService {

    VendorFinalizationResponseDto createVendorFinalization(
            VendorFinalizationRequestDto requestDto
    );

    VendorFinalizationResponseDto getVendorFinalizationById(Long id);

    List<VendorFinalizationResponseDto> getVendorFinalizationsByRfqId(Long rfqId);

    VendorFinalizationResponseDto sendToAccounts(
            Long finalizationId,
            SendFinalVendorToAccountsRequestDto requestDto
    );

    List<VendorFinalizationResponseDto> getAllSentToAccounts();
}