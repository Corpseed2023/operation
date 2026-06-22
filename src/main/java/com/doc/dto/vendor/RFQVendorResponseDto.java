package com.doc.dto.vendor;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RFQVendorResponseDto {

    private Long rfqVendorId;

    private Long vendorId;

    private String vendorName;

    private String vendorEmail;

    private String vendorMobile;

    private String gstNumber;

    private String panNumber;

    private String vendorStatus;
}