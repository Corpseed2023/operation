package com.doc.service.vendor;

import com.doc.dto.vendor.*;

import java.util.List;

public interface VendorQuotationLegalRequestService {

    VendorQuotationLegalResponseDto createLegalRequest(
            VendorQuotationLegalRequestDto requestDto
    );

    List<VendorQuotationLegalResponseDto> getAllLegalRequests(Long assignedToLegal);

    VendorQuotationLegalResponseDto sendAgreementToProcurement(
            Long id,
            Long userId,
            SendAgreementToProcurementRequestDto requestDto
    );

    VendorQuotationLegalResponseDto agreementDecision(
            Long id,
            VendorAgreementDecisionRequestDto requestDto
    );
}