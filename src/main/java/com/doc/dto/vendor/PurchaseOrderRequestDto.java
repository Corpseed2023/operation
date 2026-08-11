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

    private String poReferenceNumber;

    @NotNull(message = "Final amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Final amount must be greater than zero"
    )
    private BigDecimal finalAmount;

    /**
     * Total GST %
     *
     * Example:
     * 18 = 18%
     */
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private BigDecimal gstRate;

    /**
     * TDS %
     *
     * Example:
     * 10 = 10%
     */
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private BigDecimal tdsPercentage;

    /**
     * Place of supply / buyer state GST code.
     *
     * Example:
     * UP = 09
     * Delhi = 07
     */
    private String placeOfSupplyStateCode;

    private String scopeOfWork;

    private String termsAndConditions;

    private String remarks;

    private List<String> attachmentUrls;

    private String paymentTypeName;

    /**
     * Required during CREATE.
     */
    private Long createdBy;

    /**
     * Used during UPDATE.
     */
    private Long userId;
}