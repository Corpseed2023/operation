package com.doc.dto.vendor;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class VendorFinalizationResponseDto {

    private Long id;

    private Long rfqId;
    private String rfqNumber;

    private Long rfqVendorId;

    private Long vendorId;
    private String vendorName;
    private String vendorEmail;
    private String vendorMobile;

    private Long quotationId;
    private String quotationNumber;

    private Long quotationItemId;
    private String quotationItemName;

    private String description;

    private BigDecimal finalizedQuantity;
    private String unit;
    private BigDecimal finalizedUnitRate;
    private BigDecimal finalizedAmount;
    private BigDecimal taxPercent;
    private BigDecimal taxAmount;
    private BigDecimal totalFinalizedAmount;

    private String finalizationReason;
    private String remarks;

    private String status;

    private Long finalizedBy;
    private Date finalizedDate;

    private Long createdBy;
    private Long updatedBy;

    private Date createdDate;
    private Date updatedDate;

    private Boolean deleted;

    private String finalVendorAttachmentUrl;
    private String finalVendorRemarks;
    private Boolean sentToAccounts;
    private Long sentToAccountsBy;
    private Date sentToAccountsDate;

    private Integer priceRank;
    private String priceLevel;
}