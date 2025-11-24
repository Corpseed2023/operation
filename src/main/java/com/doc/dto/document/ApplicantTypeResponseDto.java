package com.doc.dto.document;

import lombok.Data;

@Data
public class ApplicantTypeResponseDto {
    private Long id;
    private String name;
    private String description;
    private boolean isActive;
}