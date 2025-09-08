package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UserGroupDto {
    private Long userId;
    private String userName;
    private List<ProductRatingDto> products = new ArrayList<>();
}