package com.doc.dto.productRequiredDocument;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GetAllRequiredDocumentsRequestDto {
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @Min(value = 0, message = "Page number must be 0 or greater")
    private int page = 0;

    @Min(value = 1, message = "Size must be at least 1")
    private int size = 10;

    private String name;
    private String type;
    private String country;
    private String centralName;
    private String stateName;
    private Long productId; // New field for product filter
}