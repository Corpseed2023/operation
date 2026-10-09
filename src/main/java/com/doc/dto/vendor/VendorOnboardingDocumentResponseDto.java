package com.doc.dto.vendor;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class VendorOnboardingDocumentResponseDto {

    private Long id;
    private Long onboardingId;
    private String onboardingNumber;
    private String documentType;
    private String fileName;
    private String fileUrl;
    private String remarks;
    private Long uploadedBy;
    private Date uploadedDate;
    private boolean deleted;
}