package com.doc.service;

import com.doc.dto.company.MyCompanyDocumentRequestDto;
import com.doc.dto.company.MyCompanyDocumentResponseDto;

import java.util.List;

public interface MyCompanyDocumentService {

    MyCompanyDocumentResponseDto upload(MyCompanyDocumentRequestDto request, Long currentUserId);

    MyCompanyDocumentResponseDto update(Long id, MyCompanyDocumentRequestDto request, Long currentUserId);

    List<MyCompanyDocumentResponseDto> getAll();

    MyCompanyDocumentResponseDto getById(Long id);

    List<MyCompanyDocumentResponseDto> getByType(String documentType);

    void delete(Long id);
}