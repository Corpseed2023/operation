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

//    private List<RFQVendorResponseDto> vendors;


}