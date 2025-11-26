// src/main/java/com/doc/dto/document/ProductDocumentRequirementResponseDto.java
package com.doc.dto.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocumentRequirementResponseDto {

    private Long productId;

    private String productName;

    private List<ProductDocumentMappingResponseDto> productDocumentMappingResponseDtos;
}