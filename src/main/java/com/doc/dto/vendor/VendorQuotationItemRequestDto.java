package com.doc.dto.vendor;

import com.doc.entity.vendor.QuotationItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class VendorQuotationItemRequestDto {

    private QuotationItemType itemType;

    private Integer sequenceNo;

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String description;

    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    private String unit;

    @DecimalMin(value = "0.00", message = "Unit rate cannot be negative")
    private BigDecimal unitRate;

    @DecimalMin(value = "0.00", message = "Tax percent cannot be negative")
    private BigDecimal taxPercent;

    private String remarks;
}