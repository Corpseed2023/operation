package com.doc.dto.project.activity.expense;

import com.doc.em.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountsExpenseDecisionRequestDto {

    @NotNull(message = "Accounts decision status is required")
    private ApprovalStatus status;

    /**
     * Required when Accounts approves.
     */
    private BigDecimal approvedAmount;

    @Size(
            max = 2000,
            message = "Remark cannot exceed 2000 characters"
    )
    private String remark;
}