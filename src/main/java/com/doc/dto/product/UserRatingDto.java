package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRatingDto {
    private Long userId;
    private String userName;
    private Double rating;
}