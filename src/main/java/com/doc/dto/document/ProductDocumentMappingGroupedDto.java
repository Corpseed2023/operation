// src/main/java/com/doc/dto/document/ProductDocumentMappingGroupedDto.java
package com.doc.dto.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocumentMappingGroupedDto {
    private Long applicantTypeId;
    private String applicantTypeName;
    private List<ProductDocumentMappingResponseDto> documents = new ArrayList<>();
}