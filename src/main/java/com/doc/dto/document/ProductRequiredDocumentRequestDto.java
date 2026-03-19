package com.doc.dto.document;

import com.doc.em.DocumentExpiryType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProductRequiredDocumentRequestDto {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Type is required")
    @Size(max = 50, message = "Type cannot exceed 50 characters")
    private String type;

    @Size(max = 255)
    private String country = "";

    @Size(max = 255)
    private String centralName = "";

    @Size(max = 255)
    private String stateName = "";

    @NotNull(message = "Expiry type is required")
    private DocumentExpiryType expiryType = DocumentExpiryType.UNKNOWN;

    private boolean isMandatory = true;

    @PositiveOrZero
    private Integer maxValidityYears;

    @PositiveOrZero
    private Integer minFileSizeKb;

    @Size(max = 100)
    private String allowedFormats = "pdf,jpg,png";

    @NotNull(message = "Created by user ID is required")
    private Long createdBy;

    @NotNull(message = "Updated by user ID is required")
    private Long updatedBy;
}