package com.doc.dto.vendor;

import lombok.Data;

@Data
public class ProductVendorUpdateRequestDto {


    private Long vendorId;

    private String emailSubject;

    private String emailBody;

    private String agreementAttachment;
}