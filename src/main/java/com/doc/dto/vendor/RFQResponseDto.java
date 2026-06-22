package com.doc.dto.vendor;

import com.doc.entity.vendor.RFQStatus;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RFQResponseDto {

    private Long id;
    private String rfqNumber;
    private String title;
    private String description;

    private Long productId;
    private String productName;

    private String scopeOfWork;
    private String termsAndConditions;
    private String deliveryLocation;

    private Date quotationSubmissionDeadline;
    private Date expectedStartDate;
    private Date expectedEndDate;

    private String contactPersonName;
    private String contactPersonEmail;
    private String contactPersonMobile;

    private RFQStatus status;

    private String attachmentUrl;

    private Date createdDate;
    private Date updatedDate;

    private Long createdBy;
    private Long updatedBy;

    private boolean deleted;

    private List<RFQVendorResponseDto> vendors;

    public RFQResponseDto(
            Long id,
            String rfqNumber,
            String title,
            String description,
            Long productId,
            String productName,
            String scopeOfWork,
            String termsAndConditions,
            String deliveryLocation,
            Date quotationSubmissionDeadline,
            Date expectedStartDate,
            Date expectedEndDate,
            String contactPersonName,
            String contactPersonEmail,
            String contactPersonMobile,
            RFQStatus status,
            String attachmentUrl,
            Date createdDate,
            Date updatedDate,
            Long createdBy,
            Long updatedBy,
            boolean deleted
    ) {
        this.id = id;
        this.rfqNumber = rfqNumber;
        this.title = title;
        this.description = description;
        this.productId = productId;
        this.productName = productName;
        this.scopeOfWork = scopeOfWork;
        this.termsAndConditions = termsAndConditions;
        this.deliveryLocation = deliveryLocation;
        this.quotationSubmissionDeadline = quotationSubmissionDeadline;
        this.expectedStartDate = expectedStartDate;
        this.expectedEndDate = expectedEndDate;
        this.contactPersonName = contactPersonName;
        this.contactPersonEmail = contactPersonEmail;
        this.contactPersonMobile = contactPersonMobile;
        this.status = status;
        this.attachmentUrl = attachmentUrl;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.deleted = deleted;
    }
}