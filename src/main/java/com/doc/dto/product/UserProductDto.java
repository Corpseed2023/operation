package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProductDto {
    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
}