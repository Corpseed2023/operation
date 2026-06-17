package com.doc.dto.vendor.request;



import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelectVendorQuotationRequestDto {

    private Long quotationId;

    private Long userId;
}