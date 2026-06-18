package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ProductVendorResponseDto {

    private Long mappingId;

    private Long productId;
    private String productName;

    private Long vendorId;
    private String vendorName;
    private String description;
    private String email;
    private String mobile;
    private String gstNumber;
    private String panNumber;
    private VendorStatus status;
    private String emailSubject;
    private String emailBody;
    private String agreementAttachment;

    private boolean active;
    private boolean deleted;

    private Long createdBy;
    private Long updatedBy;
    private Date createdDate;
    private Date updatedDate;
}