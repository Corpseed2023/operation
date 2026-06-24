package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorOnboardingDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorOnboardingDocumentRequestDto {

    @NotNull(message = "Document type is required")
    private VendorOnboardingDocumentType documentType;

    private String fileName;

    @NotBlank(message = "File URL is required")
    private String fileUrl;

    private String remarks;
}