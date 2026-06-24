package com.doc.dto.vendor;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class VendorOnboardingResponseDto {

    private Long id;
    private String onboardingNumber;

    private Long vendorId;
    private String vendorName;
    private String vendorEmail;

    private Long rfqId;
    private String rfqNumber;

    private Long vendorFinalizationId;

    private String serviceCategory;
    private String onboardedFor;

    private Date formSentDate;

    private String status;
    private String remarks;

    private Long createdBy;
    private Long updatedBy;

    private Date createdDate;
    private Date updatedDate;

    private Boolean deleted;
}