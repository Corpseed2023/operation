package com.doc.service;

import com.doc.dto.document.ProductRequiredDocumentRequestDto;
import com.doc.dto.document.ProductRequiredDocumentResponseDto;

import java.util.List;

public interface ProductRequiredDocumentService {
    ProductRequiredDocumentResponseDto create(ProductRequiredDocumentRequestDto dto);
    ProductRequiredDocumentResponseDto update(Long id, ProductRequiredDocumentRequestDto dto);

    List<ProductRequiredDocumentResponseDto> getActivePaginated(Long userId, int page, int size);
    List<ProductRequiredDocumentResponseDto> importFromS3(String s3Url, Long createdBy);

    void softDelete(Long id);
}