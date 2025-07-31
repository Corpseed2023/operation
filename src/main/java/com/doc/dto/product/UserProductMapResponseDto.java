package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * DTO for returning user-product mapping details.
 */
@Getter
@Setter
public class UserProductMapResponseDto {
    private Long id;
    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
    private Double rating;
    private Long createdBy;
    private Long updatedBy;
    private Date createdDate;
    private Date updatedDate;
    private boolean isDeleted;
}
