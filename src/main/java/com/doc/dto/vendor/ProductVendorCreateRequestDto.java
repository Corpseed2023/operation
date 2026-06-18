package com.doc.dto.vendor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVendorCreateRequestDto {

    /*
     * Use this when vendor already exists.
     */
    private Long vendorId;

    /*
     * Use this when creating a new vendor from product screen.
     */
    @Valid
    private NewVendorDto vendor;

    /*
     * Product-vendor mapping fields.
     * These belong to ProductVendorMapping, not Vendor master.
     */
    @Size(max = 500, message = "Email subject cannot exceed 500 characters")
    private String emailSubject;

    private String emailBody;

    @Size(max = 1000, message = "Agreement attachment cannot exceed 1000 characters")
    private String agreementAttachment;
}