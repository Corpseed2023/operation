package com.doc.dto.productRequiredDocument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetRequiredDocumentsByProductRequestDto {
    private Long projectId;
    private String stateName;
    private String centralName;
}