package com.doc.dto.vendor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PurchaseOrderRequestDto {

    @NotNull(message = "Procurement Assignment ID is required")
    private Long procurementAssignmentId;

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    private BigDecimal gstAmount;

    private String scopeOfWork;

    private LocalDate validTillDate;

    private String paymentTypeName;   // e.g. "PARTIAL", "FULL"

    private String termsAndConditions;

    @NotNull(message = "CreatedBy user ID is required")
    private Long createdBy;           // ← Fixed
}