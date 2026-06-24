package com.doc.dto.vendor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VendorOnboardingSendFormRequestDto {

    @NotNull(message = "Created by is required")
    private Long createdBy;

    private String serviceCategory;

    private String onboardedFor;

    private String remarks;

    private String subject;

    private String message;

    @Valid
    @NotEmpty(message = "At least one document is required")
    private List<VendorOnboardingDocumentRequestDto> documents;
}