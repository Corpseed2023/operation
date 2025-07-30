package com.doc.dto.productRequiredDocument;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductRequiredDocumentsRequestDto {

    @NotBlank(message = "Document name cannot be empty")
    private String name;

    private String description;

    @NotBlank(message = "Document type cannot be empty")
    private String type;

    private String country;

    private String centralName;

    private String stateName;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;

    @NotNull(message = "Updated by user ID cannot be null")
    private Long updatedBy;

    private List<Long> productIds;
}