package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class ProductVendorResponseDto {

    private Long mappingId;

    private Long productId;
    private String productName;

    private Long vendorId;
    private String vendorName;
    private String description;
    private String email;
    private String mobile;
    private String gstNumber;
    private String panNumber;
    private VendorStatus status;

    private String emailSubject;
    private String emailBody;
    private String agreementAttachment;

    private boolean active;
    private boolean deleted;

    private Long createdBy;
    private Long updatedBy;
    private Date createdDate;
    private Date updatedDate;

    // Finalization price/ranking details
    private Boolean finalized;
    private Long finalizationId;
    private Long rfqId;
    private String rfqNumber;
    private Long quotationId;
    private String quotationNumber;
    private Long quotationItemId;
    private String quotationItemName;

    private BigDecimal finalizedQuantity;
    private String unit;
    private BigDecimal finalizedUnitRate;
    private BigDecimal finalizedAmount;
    private BigDecimal taxPercent;
    private BigDecimal taxAmount;
    private BigDecimal totalFinalizedAmount;

    private Integer priceRank;
    private String priceLevel;

}