package com.doc.dto.vendor.request;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AddVendorQuotationRequestDto {

    private Long vendorId;

    private BigDecimal quotedAmount;

    private String remarks;

    private String quotationFilePath;

    private Long userId;
}