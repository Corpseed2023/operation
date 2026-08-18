package com.doc.dto.company;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MyCompanyDocumentRequestDto {

    @NotNull(message = "Required document ID is required")
    private Long requiredDocumentId;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File URL is required")
    private String fileUrl;

    private Integer fileSizeKb;

    private String fileFormat;

    private String documentNumber;

    private String remarks;
}