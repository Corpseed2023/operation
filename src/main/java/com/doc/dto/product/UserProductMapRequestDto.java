package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO for creating or updating multiple user-product mappings.
 */
@Getter
@Setter
public class UserProductMapRequestDto {
    private List<Long> userIds;
    private List<Long> productIds;
    private Double rating;
    private Long createdBy;
    private Long updatedBy;
}
