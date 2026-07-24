package com.doc.dto.project.activity.expense;

import com.doc.em.ApprovalStatus;
import com.doc.em.ExpensePaidBy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountsExpenseDecisionRequestDto {

    @NotNull(message = "Accounts decision status is required")
    private ApprovalStatus status;

    /**
     * Required when status is APPROVED.
     */
    @DecimalMin(
            value = "0.01",
            message = "Approved amount must be greater than zero"
    )
    @Digits(
            integer = 13,
            fraction = 2,
            message = "Approved amount can contain maximum two decimal places"
    )
    private BigDecimal approvedAmount;

    /**
     * Required for approved GOVERNMENT_FEE expenses.
     */
    private ExpensePaidBy paidBy;

    @Size(
            max = 2000,
            message = "Decision remark cannot exceed 2000 characters"
    )
    private String remark;
}