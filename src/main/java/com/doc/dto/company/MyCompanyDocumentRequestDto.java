package com.doc.dto.company;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MyCompanyDocumentRequestDto {

    @NotBlank(message = "Document type is required")
    private String documentType;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File URL is required")
    private String fileUrl;

    private Integer fileSizeKb;

    private String fileFormat;

    private String documentNumber;

    private String remarks;
}