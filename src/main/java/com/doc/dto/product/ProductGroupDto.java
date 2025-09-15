package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductGroupDto {
    private Long productId;
    private String productName;
    private List<UserRatingDto> users = new ArrayList<>();
}