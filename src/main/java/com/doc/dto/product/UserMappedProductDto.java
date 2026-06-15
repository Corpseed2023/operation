package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class UserMappedProductDto {

    private Long mappingId;

    private Long productId;
    private String productName;

    private Double rating;
    private Boolean assigned;

    private Long createdBy;
    private Long updatedBy;

    private Date createdDate;
    private Date updatedDate;
}