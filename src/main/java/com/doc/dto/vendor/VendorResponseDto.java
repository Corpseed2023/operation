package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class VendorResponseDto {

    private Long id;

    private String vendorCode;

    private String name;

    private String description;

    private String email;

    private String mobile;

    private String gstNumber;

    private String panNumber;

    private VendorStatus status;

    private boolean isVerified;

    private Long createdBy;
    private Long updatedBy;

    private Date createdDate;
    private Date updatedDate;

    private boolean isDeleted;
}