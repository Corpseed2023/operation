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

    List<ProductRequiredDocumentResponseDto> getAllPaginated(int page, int size);     // includes inactive
    List<ProductRequiredDocumentResponseDto> getActivePaginated(int page, int size);  // only active
    List<ProductRequiredDocumentResponseDto> getActiveList();                         // no pagination, active only
}