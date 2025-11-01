package com.doc.dto.document;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CompanyDocumentResponseDto {

    private Long id;
    private Long requiredDocumentId;
    private String documentName;
    private String documentType;
    private String standardLevel;
    private String fileUrl;
    private String fileName;
    private String status;
    private LocalDate expiryDate;
    private Boolean isPermanent;
    private Integer daysUntilExpiry;
    private String expiryStatus; // "PERMANENT", "VALID", "EXPIRED"
    private boolean isReusable;
    private LocalDate verifiedDate;
    private Long verifiedById;
    private String verifiedByName;
    private Integer fileSizeKb;
    private String fileFormat;
    private boolean validationPassed;
    private String validationIssues;
    private Double qualityScore;
    private int replacementCount;
}