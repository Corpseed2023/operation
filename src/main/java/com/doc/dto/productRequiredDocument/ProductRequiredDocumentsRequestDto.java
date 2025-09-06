package com.doc.dto.productRequiredDocument;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductRequiredDocumentsRequestDto {

    private String name;
    private String description;
    private String type;
    private String country;
    private String centralName;
    private String stateName;
    private Long createdBy;
    private Long updatedBy;
    private List<Long> productIds = new ArrayList<>();
}