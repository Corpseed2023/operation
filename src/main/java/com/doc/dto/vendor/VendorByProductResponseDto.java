package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorByProductResponseDto {

    private Long vendorId;

    private String vendorName;

    private String email;

    private String mobile;

    private String gstNumber;

    private String panNumber;

    private VendorStatus vendorStatus;

    private boolean mappingActive;
}