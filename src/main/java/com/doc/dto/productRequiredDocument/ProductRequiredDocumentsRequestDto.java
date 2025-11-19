// src/main/java/com/doc/dto/productRequiredDocument/ProductRequiredDocumentsRequestDto.java

package com.doc.dto.productRequiredDocument;

import com.doc.em.DocumentExpiryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductRequiredDocumentsRequestDto {

    private Long id; // Required for update, but not for create

    @NotBlank(message = "Document name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotBlank(message = "Document type is required")
    @Size(max = 50)
    private String type;

    @Size(max = 255)
    private String country;

    @Size(max = 255)
    private String centralName;

    @Size(max = 255)
    private String stateName;

    @NotNull(message = "Expiry type is required")
    private DocumentExpiryType expiryType = DocumentExpiryType.UNKNOWN;

    private Boolean isMandatory = true;

    private Integer maxValidityYears;

    private Integer minFileSizeKb;

    @Size(max = 100)
    private String allowedFormats = "pdf,jpg,png";

    @NotNull(message = "Created by user ID is required")
    private Long createdBy;

    @NotNull(message = "Updated by user ID is required")
    private Long updatedBy;

    private List<Long> productIds = new ArrayList<>();
}