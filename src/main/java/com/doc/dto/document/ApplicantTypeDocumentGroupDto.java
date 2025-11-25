// src/main/java/com/doc/dto/document/ApplicantTypeDocumentGroupDto.java
package com.doc.dto.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantTypeDocumentGroupDto {

    private Long applicantTypeId;

    private String applicantTypeName; // e.g., "Brand Owner", "Importer"

    private List<ProductDocumentMappingResponseDto> documents;
}