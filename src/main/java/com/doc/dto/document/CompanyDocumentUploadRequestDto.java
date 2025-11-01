package com.doc.dto.document;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CompanyDocumentUploadRequestDto {

    @NotNull
    private Long companyId;

    @NotNull
    private Long requiredDocumentId;

    @NotBlank
    @Size(max = 255)
    private String fileName;

    @NotNull
    private Long uploadedById;

    @NotNull
    private Long createdById;

    private LocalDate expiryDate;

    private Boolean isPermanent;

    private Integer fileSizeKb;
    private String fileFormat;
}