// src/main/java/com/doc/service/ProductRequiredDocumentService.java
package com.doc.service;

import com.doc.dto.document.ProductRequiredDocumentRequestDto;
import com.doc.dto.document.ProductRequiredDocumentResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductRequiredDocumentService {
    ProductRequiredDocumentResponseDto create(ProductRequiredDocumentRequestDto dto);
    ProductRequiredDocumentResponseDto update(Long id, ProductRequiredDocumentRequestDto dto);
    void softDelete(Long id);
    ProductRequiredDocumentResponseDto getById(Long id);
    Page<ProductRequiredDocumentResponseDto> getAllPaged(int page, int size);
    Page<ProductRequiredDocumentResponseDto> getAllActivePaged(int page, int size);
    List<ProductRequiredDocumentResponseDto> getAllActive();
}