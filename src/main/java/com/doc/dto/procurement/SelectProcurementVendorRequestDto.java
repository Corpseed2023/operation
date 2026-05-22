package com.doc.dto.procurement;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SelectProcurementVendorRequestDto {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotNull(message = "User ID is required")
    private Long userId;

    private BigDecimal estimatedAmount;

    private BigDecimal finalAmount;

    private String remarks;
}