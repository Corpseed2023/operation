package com.doc.dto.project.activity.expense;

import com.doc.em.ApprovalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountsExpenseDecisionRequestDto {

    /**
     * Allowed:
     * APPROVED, REJECTED, ON_HOLD
     */
    @NotNull(message = "Accounts decision status is required")
    private ApprovalStatus status;

    /**
     * Mandatory only when status is APPROVED.
     */
    @DecimalMin(
            value = "0.01",
            message = "Approved amount must be greater than zero"
    )
    @Digits(
            integer = 13,
            fraction = 2,
            message = "Approved amount must contain a maximum of two decimal places"
    )
    private BigDecimal approvedAmount;

    /**
     * Required when:
     * - Expense is rejected
     * - Expense is placed on hold
     * - Approved amount differs from requested amount
     */
    @Size(
            max = 2000,
            message = "Decision remark cannot exceed 2000 characters"
    )
    private String remark;
}