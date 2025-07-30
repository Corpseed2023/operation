package com.doc.service;


import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsRequestDto;
import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsResponseDto;

import java.util.List;

public interface ProductRequiredDocumentsService {

    ProductRequiredDocumentsResponseDto createRequiredDocument(ProductRequiredDocumentsRequestDto requestDto);

    ProductRequiredDocumentsResponseDto getRequiredDocumentById(Long id);

    List<ProductRequiredDocumentsResponseDto> getAllRequiredDocuments(int page, int size, String name, String type, String country, String centralName, String stateName);

    ProductRequiredDocumentsResponseDto updateRequiredDocument(Long id, ProductRequiredDocumentsRequestDto requestDto);

    void deleteRequiredDocument(Long id);
}