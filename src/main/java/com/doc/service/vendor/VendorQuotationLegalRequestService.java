package com.doc.service.vendor;

import com.doc.dto.vendor.VendorAgreementDecisionRequestDto;
import com.doc.dto.vendor.VendorAgreementPrepareRequestDto;
import com.doc.dto.vendor.VendorQuotationLegalRequestDto;
import com.doc.dto.vendor.VendorQuotationLegalResponseDto;

import java.util.List;

public interface VendorQuotationLegalRequestService {

    VendorQuotationLegalResponseDto createLegalRequest(
            VendorQuotationLegalRequestDto requestDto
    );

    List<VendorQuotationLegalResponseDto> getAllLegalRequests(Long assignedToLegal);

    VendorQuotationLegalResponseDto prepareAgreement(
            Long id,
            VendorAgreementPrepareRequestDto requestDto
    );

    VendorQuotationLegalResponseDto sendAgreementToOperation(Long id, Long userId);

    VendorQuotationLegalResponseDto sendAgreementToVendor(Long id, Long userId);

    VendorQuotationLegalResponseDto agreementDecision(
            Long id,
            VendorAgreementDecisionRequestDto requestDto
    );
}