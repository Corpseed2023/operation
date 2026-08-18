package com.doc.service;

import com.doc.dto.company.MyCompanyDocumentRequestDto;
import com.doc.dto.company.MyCompanyDocumentResponseDto;

import java.util.List;

public interface MyCompanyDocumentService {

    MyCompanyDocumentResponseDto uploadOrReplace(MyCompanyDocumentRequestDto request, Long currentUserId);

    List<MyCompanyDocumentResponseDto> getAll();

    MyCompanyDocumentResponseDto getByRequiredDocumentId(Long requiredDocumentId);
}