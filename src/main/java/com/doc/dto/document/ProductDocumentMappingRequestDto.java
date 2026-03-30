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

    private Long applicantTypeId;

    private List<Long> requiredDocumentIds;

    private Long updatedBy;
}