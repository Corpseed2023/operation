package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRatingDto {
    private Long productId;
    private String productName;
    private Double rating;
}