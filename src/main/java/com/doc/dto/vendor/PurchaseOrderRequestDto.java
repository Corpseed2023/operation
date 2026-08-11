package com.doc.dto.vendor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class PurchaseOrderRequestDto {

    @NotNull(message = "Procurement assignment ID is required")
    private Long procurementAssignmentId;

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    /**
     * Finalized vendor/item used to obtain the GST percentage.
     * GST will not be accepted manually from the frontend.
     */
    @NotNull(message = "Vendor finalization ID is required")
    private Long vendorFinalizationId;

    private String poReferenceNumber;

    /**
     * Basic PO amount before GST and TDS.
     */
    @NotNull(message = "Final amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Final amount must be greater than zero"
    )
    private BigDecimal finalAmount;

    /**
     * TDS percentage.
     *
     * Example:
     * 10 = 10%
     */
    @NotNull(message = "TDS percentage is required")
    @DecimalMin(
            value = "0.00",
            message = "TDS percentage cannot be negative"
    )
    @DecimalMax(
            value = "100.00",
            message = "TDS percentage cannot be greater than 100"
    )
    private BigDecimal tdsPercentage;

    private String scopeOfWork;

    private String termsAndConditions;

    private String remarks;

    private List<String> attachmentUrls;

    private String paymentTypeName;

    /**
     * Required during Purchase Order creation.
     */
    private Long createdBy;

    /**
     * Used while updating a DRAFT Purchase Order.
     */
    private Long userId;
}