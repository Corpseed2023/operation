package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for creating or updating a user-product mapping.
 */
@Getter
@Setter
public class UserProductMapRequestDto {
    private Long userId;
    private Long productId;
    private Double rating;
    private Long createdBy;
    private Long updatedBy;
}
