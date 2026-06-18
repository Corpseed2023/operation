package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVendorCreateRequestDto {

    // Optional: if vendor already exists, send this.
    private Long existingVendorId;

    // Vendor fields
    private String name;
    private String description;
    private String email;
    private String mobile;
    private String gstNumber;
    private String panNumber;
    private VendorStatus status;
    private boolean verified;

    // Product-vendor specific mail/agreement fields
    private String emailSubject;
    private String emailBody;
    private String agreementAttachment;
}