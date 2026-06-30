package com.doc.service.vendor;

import com.doc.dto.vendor.*;

import java.util.List;

public interface VendorFinalizationService {

    VendorFinalizationResponseDto createVendorFinalization(
            VendorFinalizationRequestDto requestDto
    );

    VendorFinalizationResponseDto getVendorFinalizationById(Long id);

    List<VendorFinalizationResponseDto> getVendorFinalizationsByRfqId(Long rfqId);

    VendorAccountsSubmissionResponseDto sendToAccounts(
            Long finalizationId,
            VendorAccountsSubmissionRequestDto requestDto
    );

    List<VendorAccountsSubmissionResponseDto> getAllSentToAccounts();

    VendorAccountsSubmissionResponseDto approveByAccounts(
            Long submissionId,
            AccountsVendorFinalizationRequestDto requestDto
    );

    VendorAccountsSubmissionResponseDto rejectByAccounts(
            Long submissionId,
            AccountsVendorFinalizationRequestDto requestDto
    );

}