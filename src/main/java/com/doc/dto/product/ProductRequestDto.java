package com.doc.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequestDto {

    private Long productId;

    @NotBlank(message = "Product name cannot be empty")
    private String productName;

    private String description;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;

    @NotNull(message = "Updated by user ID cannot be null")
    private Long updatedBy;

    private boolean isActive = true;

    private boolean requiresClientPortal = false;

    private String expectedPortalName;

    private String defaultPortalUrl;
}