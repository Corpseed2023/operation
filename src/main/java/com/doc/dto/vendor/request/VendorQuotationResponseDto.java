package com.doc.dto.procurement;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class VendorQuotationResponseDto {

    private Long id;

    private Long vendorId;
    private String vendorName;
    private String vendorEmail;
    private String vendorMobile;
    private String gstNumber;
    private String panNumber;

    private BigDecimal quotedAmount;

    private String remarks;
    private String quotationFilePath;

    private boolean selected;

    private Date createdDate;
    private Date updatedDate;
}