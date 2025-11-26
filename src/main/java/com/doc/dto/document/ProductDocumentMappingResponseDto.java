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

    private Long applicantTypeId;

    private String applicantTypeName; // e.g., "Brand Owner", "Importer"

    public ProductDocumentMappingResponseDto(Long mappingId,
                                             Long requiredDocumentId,
                                             String documentName,
                                             String documentType,
                                             String description,
                                             boolean isMandatory,
                                             Integer displayOrder,
                                             String allowedFormats,
                                             DocumentExpiryType expiryType,
                                             Integer maxValidityYears) {
        this.mappingId = mappingId;
        this.requiredDocumentId = requiredDocumentId;
        this.documentName = documentName;
        this.documentType = documentType;
        this.description = description;
        this.isMandatory = isMandatory;
        this.displayOrder = displayOrder;
        this.allowedFormats = allowedFormats;
        this.expiryType = expiryType;
        this.maxValidityYears = maxValidityYears;
    }
}