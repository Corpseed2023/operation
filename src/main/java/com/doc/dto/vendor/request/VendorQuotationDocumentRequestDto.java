package com.doc.dto.vendor.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorQuotationDocumentRequestDto {

    private String fileName;

    @NotBlank(message = "File URL is required")
    private String fileUrl;

    private String fileType;

    private Long fileSizeKb;

}