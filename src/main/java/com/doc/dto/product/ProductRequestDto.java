package com.doc.dto.product;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ProductRequestDto {

    private Long productId ;

    @NotBlank(message = "Product name cannot be empty")
    private String productName;

    private String description;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;

    @NotNull(message = "Updated by user ID cannot be null")
    private Long updatedBy;

    private Date date;

    private boolean isActive = true;

}