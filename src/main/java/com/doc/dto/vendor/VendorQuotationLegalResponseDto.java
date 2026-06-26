package com.doc.dto.vendor;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class VendorQuotationLegalResponseDto {

    private Long id;

    private Long vendorQuotationId;
    private String quotationNumber;

    private Long vendorId;
    private String vendorName;

    private String legalRequestTitle;
    private String notes;
    private String statusReason;
    private String status;

    private Long assignedToLegal;

    private Long createdBy;
    private Long updatedBy;

    private Date createdDate;
    private Date updatedDate;

    private Boolean deleted;

    private String agreementFileUrl;

    private List<VendorQuotationDocumentResponseDto> documents;
}