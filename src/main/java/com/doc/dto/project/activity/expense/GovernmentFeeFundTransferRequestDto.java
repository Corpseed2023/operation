package com.doc.dto.project.activity.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class GovernmentFeeFundTransferRequestDto {

    @NotNull(message = "From bank ledger ID is required")
    @Positive(message = "From bank ledger ID must be greater than zero")
    private Long fromBankLedgerId;

    @NotBlank(message = "From bank name is required")
    @Size(max = 150)
    private String fromBankName;

    @NotNull(message = "To bank ledger ID is required")
    @Positive(message = "To bank ledger ID must be greater than zero")
    private Long toBankLedgerId;

    @NotBlank(message = "To bank name is required")
    @Size(max = 150)
    private String toBankName;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Transfer amount must be greater than zero"
    )
    private BigDecimal amount;

    @NotNull(message = "Transfer date is required")
    @PastOrPresent(
            message = "Transfer date cannot be in the future"
    )
    private LocalDate transferDate;

    @NotBlank(message = "Transfer reference is required")
    @Size(max = 150)
    private String transferReference;

    @Size(max = 1000)
    private String transferProofUrl;

    @Size(max = 2000)
    private String remark;
}