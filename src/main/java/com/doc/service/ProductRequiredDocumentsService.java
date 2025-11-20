package com.doc.service;

import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsRequestDto;
import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsResponseDto;

import java.util.List;
import java.util.Map;

public interface ProductRequiredDocumentsService {

    List<ProductRequiredDocumentsResponseDto> createRequiredDocuments(List<ProductRequiredDocumentsRequestDto> requestDtoList);

    ProductRequiredDocumentsResponseDto updateRequiredDocument(Long id, ProductRequiredDocumentsRequestDto requestDto);


    List<ProductRequiredDocumentsResponseDto> getRequiredDocumentsByProjectAndProduct(Long projectId, Long productId, Long userId);


    List<ProductRequiredDocumentsResponseDto> getRequiredDocumentsForAdmin(Long productId, Long userId, String stateName, String centralName);

    List<Map<String, Object>> getActiveRequiredDocumentIdsAndNames(Long userId);
}