package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ProductResponseDto {

    private Long id;

    private String productName;

    private String description;

    private Long createdBy;

    private Long updatedBy;

    private LocalDate date;

    private Date createdDate;

    private Date updatedDate;

    private boolean isActive;

}