package com.doc.dto.project.activity.expense;

import com.doc.em.ApprovalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AccountsExpenseDecisionRequestDto {

    @NotNull(message = "Accounts decision status is required")
    private ApprovalStatus status;

    /*
     * Conditionally required when status = APPROVED.
     * Service-level validation still remains necessary.
     */
    @DecimalMin(
            value = "0.01",
            message = "Approved amount must be greater than zero"
    )
    private BigDecimal approvedAmount;

    @PastOrPresent(
            message = "Approval date cannot be in the future"
    )
    private LocalDate approvalDate;

    @Size(
            max = 2000,
            message = "Remark cannot exceed 2000 characters"
    )
    private String remark;
}