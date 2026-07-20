package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorGSTRegistrationType;
import com.doc.entity.vendor.VendorStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class VendorResponseDto {

    private Long id;

    private String name;

    private String description;

    private String email;

    private String mobile;

    private String gstNumber;

    private String panNumber;

    private VendorStatus status;

    private Long createdBy;
    private Long updatedBy;

    private Date createdDate;
    private Date updatedDate;

    private VendorGSTRegistrationType gstRegistrationType;

    private boolean isDeleted;

    private List<RFQVendorResponseDto> rfqs;
    private List<VendorQuotationResponseDto> quotations;
    private List<VendorFinalizationResponseDto> finalizations;
    private List<VendorOnboardingResponseDto> onboardingForms;
}