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

    /**
     * Required while creating a Purchase Order.
     */
    private Long procurementAssignmentId;

    /**
     * Required while creating a Purchase Order. Optional while updating when
     * the existing PO vendor must remain unchanged.
     */
    private Long vendorId;

    /**
     * True when GST must be calculated for this Purchase Order.
     */
    @NotNull(message = "GST applicable flag is required")
    private Boolean gstApplicable;

    /**
     * Total GST percentage, for example 18.00. Required and greater than zero
     * only when gstApplicable is true. Keep null or 0 when it is false.
     */
    @DecimalMin(
            value = "0.00",
            message = "GST percentage cannot be negative"
    )
    @DecimalMax(
            value = "100.00",
            message = "GST percentage cannot be greater than 100"
    )
    private BigDecimal gstPercentage;

    private String poReferenceNumber;

    @NotNull(message = "Final amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Final amount must be greater than zero"
    )
    private BigDecimal finalAmount;

    private String scopeOfWork;

    private String termsAndConditions;

    private String remarks;

    private List<String> attachmentUrls;

    private String paymentTypeName;

    /**
     * Required while creating a Purchase Order.
     */
    private Long createdBy;

    /**
     * Commercial payment terms agreed with the external vendor.
     * Examples: "100% Advance", "Net 30 days".
     */
    private String paymentTerms;

    /**
     * Used while updating a DRAFT Purchase Order.
     */
    private Long userId;
}