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
     * External vendor providing the service/product.
     */
    private Long vendorId;

    /**
     * true  = GST applicable
     * false = GST not applicable
     */
    @NotNull(message = "GST applicable flag is required")
    private Boolean gstApplicable;

    /**
     * Total GST percentage, for example 18.00.
     *
     * Required when gstApplicable is true.
     * Must be null or zero when gstApplicable is false.
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

    /**
     * Vendor's basic amount before GST.
     *
     * Example: 50.00
     */
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

    private Integer paymentTerms;

    /**
     * Used while updating a DRAFT Purchase Order.
     */
    private Long userId;
}