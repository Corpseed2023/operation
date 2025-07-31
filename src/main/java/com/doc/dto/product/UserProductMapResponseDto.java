package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * DTO for retrieving user-product mapping details.
 */
@Getter
@Setter
public class UserProductMapResponseDto {

    private Long id;
    private Long userId;
    private Long productId;
    private String userFullName;
    private String productName;
    private Double rating;
    private Date createdDate;
    private Date updatedDate;
    private boolean isDeleted;
}
