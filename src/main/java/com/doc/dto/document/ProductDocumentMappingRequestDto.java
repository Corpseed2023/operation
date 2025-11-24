// src/main/java/com/doc/dto/document/ProductDocumentMappingRequestDto.java
package com.doc.dto.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocumentMappingRequestDto {

    private Long productId;

    private Long applicantTypeId;        // null = global (applies to all applicant types)

    private List<Long> requiredDocumentIds;

    private Long updatedBy;
}