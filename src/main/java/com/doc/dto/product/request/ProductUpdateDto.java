package com.doc.dto.product.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductUpdateDto {

    @NotBlank(message = "Product name is required")
    private String productName;

    private String description;

    @NotNull(message = "Active status is required")
    private Boolean active;

    private Boolean requiresClientPortal;

    private String expectedPortalName;

    private String defaultPortalUrl;

}