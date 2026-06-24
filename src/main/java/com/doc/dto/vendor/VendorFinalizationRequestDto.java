package com.doc.dto.vendor;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class VendorFinalizationRequestDto {

    @NotNull(message = "RFQ id is required")
    private Long rfqId;

    @NotNull(message = "RFQ vendor id is required")
    private Long rfqVendorId;

    @NotNull(message = "Vendor id is required")
    private Long vendorId;

    @NotNull(message = "Quotation id is required")
    private Long quotationId;

    @NotNull(message = "Quotation item id is required")
    private Long quotationItemId;

    private String description;

    @NotNull(message = "Finalized quantity is required")
    @DecimalMin(value = "0.01", message = "Finalized quantity must be greater than 0")
    private BigDecimal finalizedQuantity;

    private String unit;

    @NotNull(message = "Finalized unit rate is required")
    @DecimalMin(value = "0.00", message = "Finalized unit rate cannot be negative")
    private BigDecimal finalizedUnitRate;

    @DecimalMin(value = "0.00", message = "Tax percent cannot be negative")
    private BigDecimal taxPercent;

    private String finalizationReason;

    private String remarks;

    @NotNull(message = "Created by is required")
    private Long createdBy;
}