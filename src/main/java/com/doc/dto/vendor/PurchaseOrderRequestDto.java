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

    private Long procurementAssignmentId;

    private Long vendorId;

    /**
     * Finalized vendor item used by the backend to obtain the GST percentage.
     */
    @NotNull(message = "Vendor finalization ID is required")
    private Long vendorFinalizationId;

    private String poReferenceNumber;

    @NotNull(message = "Final amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Final amount must be greater than zero"
    )
    private BigDecimal finalAmount;

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
     * Required while creating a Purchase Order.
     */
    private Long createdBy;
    private Integer paymentTerms;

    /**
     * Used while updating a DRAFT Purchase Order.
     */
    private Long userId;
}
