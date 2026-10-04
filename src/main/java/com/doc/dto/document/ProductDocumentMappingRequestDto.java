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

    /*
     * Optional.
     *
     * null / empty:
     * document is global for the product.
     *
     * [4]:
     * document belongs to applicant type 4.
     *
     * [4, 5]:
     * document belongs to applicant types 4 and 5.
     */
    private List<Long> applicantTypeIds;

    private List<Long> requiredDocumentIds;

    private Long updatedBy;
}