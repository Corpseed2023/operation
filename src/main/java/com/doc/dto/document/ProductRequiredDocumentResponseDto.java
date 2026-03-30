package com.doc.dto.document;

import com.doc.em.DocumentExpiryType;
import lombok.Data;

import java.util.Date;

@Data
public class ProductRequiredDocumentResponseDto {

    private Long id;
    private String name;
    private String description;
    private String type;
    private String country;
    private String centralName;
    private String stateName;
    private DocumentExpiryType expiryType;
    private boolean isMandatory;
    private String maxValidityYears;
    private String expiryTypeDescription;


    private String allowedFormats;

    private String applicability;
    private String remarks;

    private Long createdBy;
    private Long updatedBy;
    private Date createdDate;
    private Date updatedDate;
    private boolean isActive;

    private Integer maxFileSizeKb;
}