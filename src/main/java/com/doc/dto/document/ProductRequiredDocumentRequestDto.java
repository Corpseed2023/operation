package com.doc.dto.document;

import com.doc.em.DocumentExpiryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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

    private String maxValidityYears;

    private String expiryTypeDescription;


    @PositiveOrZero
    private Integer maxFileSizeKb;

    @Size(max = 100)
    private String allowedFormats = "pdf,jpg,png";

    // ==================== NEW FIELDS ====================
    @Size(max = 500, message = "Applicability cannot exceed 500 characters")
    private String applicability;

    @Size(max = 1000, message = "Remarks cannot exceed 1000 characters")
    private String remarks;
    // ===================================================

    @NotNull(message = "Created by user ID is required")
    private Long createdBy;

    @NotNull(message = "Updated by user ID is required")
    private Long updatedBy;


}