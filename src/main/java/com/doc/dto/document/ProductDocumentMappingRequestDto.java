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
     * Multiple applicant types can be selected in one request.
     *
     * Example:
     * Brand Owner = 4
     * Importer    = 5
     *
     * applicantTypeIds = [4, 5]
     */
    private List<Long> applicantTypeIds;

    /*
     * Multiple documents can be selected.
     *
     * These documents will be mapped with every
     * selected applicant type.
     */
    private List<Long> requiredDocumentIds;

    private Long updatedBy;
}