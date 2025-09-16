package com.doc.service;

import com.doc.dto.productRequiredDocument.GetAllRequiredDocumentsRequestDto;
import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsRequestDto;
import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsResponseDto;

import java.util.List;

public interface ProductRequiredDocumentsService {

    List<ProductRequiredDocumentsResponseDto> createRequiredDocuments(List<ProductRequiredDocumentsRequestDto> requestDtoList);

    ProductRequiredDocumentsResponseDto getRequiredDocumentById(Long id, Long userId);


    ProductRequiredDocumentsResponseDto updateRequiredDocument(Long id, ProductRequiredDocumentsRequestDto requestDto);

    void deleteRequiredDocument(Long id);

    List<ProductRequiredDocumentsResponseDto> getRequiredDocumentsByProjectAndProduct(Long projectId, Long productId, Long userId);

    List<ProductRequiredDocumentsResponseDto> getAllRequiredDocuments(Long userId, int page, int size, String name, String type, String country, String centralName, String stateName, Long productId);
}