// ProductDocumentGroupDto.java
package com.doc.dto.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocumentGroupDto {
    private Long applicantTypeId;
    private String applicantTypeName;  // "Common Documents" if null

    private List<ProductDocumentMappingResponseDto> documents;
}