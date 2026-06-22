package com.doc.dto.vendor;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class VendorQuotationItemResponseDto {

    private Long id;

    private Long quotationId;

    private String itemType;

    private Integer sequenceNo;

    private String itemName;

    private String description;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal unitRate;

    private BigDecimal amount;

    private BigDecimal taxPercent;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private String remarks;

    private Long createdBy;
    private Long updatedBy;

    private Date createdDate;
    private Date updatedDate;

    private Boolean deleted;
}