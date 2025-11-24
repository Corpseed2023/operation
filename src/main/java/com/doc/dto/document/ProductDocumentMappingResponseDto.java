package com.doc.dto.document;

import com.doc.em.DocumentExpiryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocumentMappingResponseDto {
    private Long mappingId;
    private Long requiredDocumentId;
    private String documentName;
    private String documentType;
    private String description;
    private boolean isMandatory;
    private Integer displayOrder;
    private String allowedFormats;
    private DocumentExpiryType expiryType;
    private Integer maxValidityYears;
}