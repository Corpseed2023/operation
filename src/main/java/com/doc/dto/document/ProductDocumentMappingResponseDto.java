package com.doc.dto.document;

import com.doc.em.DocumentExpiryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
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
    private String expiryTypeDescription;
    private String maxValidityYears;
    private Long applicantTypeId;
    private String applicantTypeName;
}