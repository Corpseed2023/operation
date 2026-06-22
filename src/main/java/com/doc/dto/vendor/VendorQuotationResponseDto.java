package com.doc.dto.vendor;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class VendorQuotationResponseDto {

    private Long id;

    private Long rfqId;
    private Long rfqVendorId;

    private Long vendorId;
    private String vendorName;
    private String vendorEmail;
    private String vendorMobile;

    private String quotationNumber;

    private Date quotationDate;
    private Date validTill;

    private Integer versionNo;
    private Boolean latest;

    private String currency;

    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;

    private Integer deliveryDays;

    private String paymentTerms;
    private String warrantyTerms;
    private String remarks;

    private String quotationAttachmentUrl;

    private String status;

    private Long createdBy;
    private Long updatedBy;

    private Date createdDate;
    private Date updatedDate;

    private Boolean deleted;

    private List<VendorQuotationItemResponseDto> items;
}