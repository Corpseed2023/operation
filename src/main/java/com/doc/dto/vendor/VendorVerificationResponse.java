package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorVerificationResponse {

    private Long vendorId;
    private String vendorName;
    private String email;
    private String mobile;
    private String gstNumber;
    private String panNumber;
    private VendorStatus status;
    private boolean verified;
}
