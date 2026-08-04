package com.doc.dto.project.activity.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class GovernmentFeePaymentRequestDto {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.001", message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    private LocalDate paymentDate;

    /** Kept as String because the accounting enum belongs to Account Service. */
    @NotBlank(message = "Payment mode is required")
    @Pattern(
            regexp = "(?i)^(NET_BANKING|NEFT|RTGS|IMPS|UPI|CARD|BANK_TRANSFER|CHEQUE|DEMAND_DRAFT|OTHER)$",
            message = "Payment mode must be NET_BANKING, NEFT, RTGS, IMPS, UPI, CARD, BANK_TRANSFER, CHEQUE, DEMAND_DRAFT or OTHER"
    )
    private String paymentMode;

    @NotBlank(message = "Government payment reference is required")
    @Size(max = 150, message = "Payment reference cannot exceed 150 characters")
    private String paymentReference;

    @NotBlank(message = "Government payment receipt URL is required")
    @Size(max = 1000, message = "Payment receipt URL cannot exceed 1000 characters")
    private String paymentReceiptUrl;

    @Size(max = 2000, message = "Payment remark cannot exceed 2000 characters")
    private String remark;
}
