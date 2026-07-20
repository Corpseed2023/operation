package com.doc.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class PurchaseOrderRequestDto {

    @NotNull(message = "Procurement Assignment ID is required")
    private Long procurementAssignmentId;

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    private String poReferenceNumber;           // Vendor quotation / reference number

    @NotNull(message = "Final Amount is required")
    @Positive(message = "Final amount must be positive")
    private BigDecimal finalAmount;

    // ==================== GST BREAKUP ====================
    private BigDecimal gstRate;                 // e.g., 18.0

    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;

    private BigDecimal tdsPercentage;

    private BigDecimal totalTaxAmount;
    private BigDecimal grandTotal;              // Most important - final payable amount

    // ==================== COMMERCIAL DETAILS ====================
    @NotBlank(message = "Scope of Work is required")
    private String scopeOfWork;

    private String termsAndConditions;

    private String remarks;

    private LocalDate validTillDate;

    // Payment Type
    private String paymentTypeName;             // e.g., "FULL", "PARTIAL", "INSTALLMENT"

    // Attachments (URLs after uploading files)
    private List<String> attachmentUrls;

    @NotNull(message = "CreatedBy user ID is required")
    private Long createdBy;

    private Long userId;
}