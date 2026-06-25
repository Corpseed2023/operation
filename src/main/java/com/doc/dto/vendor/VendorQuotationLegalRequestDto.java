package com.doc.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorQuotationLegalRequestDto {

    @NotNull(message = "Vendor quotation id is required")
    private Long vendorQuotationId;

    @NotBlank(message = "Legal request title is required")
    private String legalRequestTitle;

    private String notes;

    private String statusReason;

    @NotNull(message = "Assigned legal user id is required")
    private Long assignedToLegal;

    @NotNull(message = "Created by is required")
    private Long createdBy;
}