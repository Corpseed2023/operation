package com.doc.service;

import com.doc.dto.document.ProductDocumentMappingGroupedDto;
import com.doc.dto.document.ProductDocumentMappingRequestDto;
import com.doc.dto.document.ProductDocumentMappingResponseDto;
import com.doc.dto.document.ProductDocumentRequirementResponseDto;

import java.util.List;

public interface ProductDocumentMappingService {

    void assignDocuments(ProductDocumentMappingRequestDto request);

    List<ProductDocumentMappingResponseDto> getRequiredDocuments(Long productId, Long applicantTypeId);

}