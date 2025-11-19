// src/main/java/com/doc/dto/productRequiredDocument/ProductRequiredDocumentsResponseDto.java

package com.doc.dto.productRequiredDocument;

import com.doc.dto.project.DocumentResponseDto;
import com.doc.em.DocumentExpiryType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ProductRequiredDocumentsResponseDto {

    private Long id;

    private String name;

    private String description;

    private String type;

    private String country;

    private String centralName;

    private String stateName;

    private DocumentExpiryType expiryType;

    private boolean isMandatory;

    private Integer maxValidityYears;

    private Integer minFileSizeKb;

    private String allowedFormats;

    private Long createdBy;

    private Long updatedBy;

    private Date createdDate;

    private Date updatedDate;

    private boolean isActive;

    private boolean isDeleted;

    private List<Long> productIds = new ArrayList<>();

    // Only populated when fetching for a specific project (shows uploaded files)
    private List<DocumentResponseDto> uploads = new ArrayList<>();

    // Helper flags (optional but very useful in frontend)
    public boolean isFixed() {
        return expiryType == DocumentExpiryType.FIXED;
    }

    public boolean isExpiring() {
        return expiryType == DocumentExpiryType.EXPIRING;
    }

    public boolean isUnknownExpiry() {
        return expiryType == DocumentExpiryType.UNKNOWN;
    }
}