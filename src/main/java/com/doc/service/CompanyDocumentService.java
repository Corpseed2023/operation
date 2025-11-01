package com.doc.service;


import com.doc.dto.document.*;

import java.util.List;

public interface CompanyDocumentService {

    CompanyDocumentResponseDto uploadCompanyDocument(CompanyDocumentUploadRequestDto request);

    CompanyDocumentResponseDto updateStatus(Long docId, CompanyDocumentStatusUpdateDto updateDto);

    List<CompanyDocumentResponseDto> getVerifiedDocuments(Long companyId);

    CompanyDocCheckResponseDto checkAvailability(Long companyId, Long requiredDocumentId);
}