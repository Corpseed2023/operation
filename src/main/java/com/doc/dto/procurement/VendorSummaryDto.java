package com.doc.dto.procurement;

import com.doc.entity.vendor.VendorStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorSummaryDto {

    private Long id;
    private String vendorCode;
    private String name;
    private String email;
    private String mobile;
    private String gstNumber;
    private String panNumber;
    private VendorStatus status;
    private boolean verified;
}