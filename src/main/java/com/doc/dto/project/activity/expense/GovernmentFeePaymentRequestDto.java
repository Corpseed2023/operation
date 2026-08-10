package com.doc.dto.project.activity.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class GovernmentFeePaymentRequestDto {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(
            value = "0.001",
            message = "Payment amount must be greater than zero"
    )
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    private LocalDate paymentDate;

    /**
     * HOW payment was made.
     * Example: NET_BANKING, NEFT, RTGS, UPI.
     */
    @NotBlank(message = "Payment mode is required")
    @Pattern(
            regexp = "(?i)^(NET_BANKING|NEFT|RTGS|IMPS|UPI|CARD|BANK_TRANSFER|CHEQUE|DEMAND_DRAFT|OTHER)$",
            message = "Payment mode must be NET_BANKING, NEFT, RTGS, IMPS, UPI, CARD, BANK_TRANSFER, CHEQUE, DEMAND_DRAFT or OTHER"
    )
    private String paymentMode="UPI";

    /**
     * Account Service ledger ID of the COMPANY BANK
     * from which government payment was actually made.
     *
     * Example:
     * 83 = Axis Bank
     * 2  = HDFC Bank
     */
    @NotNull(message = "Payment bank ledger ID is required")
    @Positive(message = "Payment bank ledger ID must be greater than zero")
    private Long paymentBankLedgerId;

    /**
     * Optional display snapshot.
     * Do not use this value for accounting validation.
     */
    @Size(
            max = 150,
            message = "Payment bank name cannot exceed 150 characters"
    )
    private String paymentBankName;

    @NotBlank(message = "Government payment reference is required")
    @Size(
            max = 150,
            message = "Payment reference cannot exceed 150 characters"
    )
    private String paymentReference;

    @NotBlank(message = "Government payment receipt URL is required")
    @Size(
            max = 1000,
            message = "Payment receipt URL cannot exceed 1000 characters"
    )
    private String paymentReceiptUrl;

    @Size(
            max = 2000,
            message = "Payment remark cannot exceed 2000 characters"
    )
    private String remark;
}