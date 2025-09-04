package com.doc.dto.productRequiredDocument;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class   ProductRequiredDocumentsResponseDto {

    private Long id;

    private String name;

    private String description;

    private String type;

    private String country;

    private String centralName;

    private String stateName;

    private Date createdDate;

    private Date updatedDate;

    private List<Long> productIds;


}