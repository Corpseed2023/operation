package com.doc.dto.project.activity.expense;

import com.doc.em.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
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

    /**
     * Required when Accounts approves.
     */
    private BigDecimal approvedAmount;

    /**
     * Accounting date selected by the Accounts team.
     * This date will be used while creating the Government Fee voucher.
     */
    private LocalDate approvalDate;

    @Size(
            max = 2000,
            message = "Remark cannot exceed 2000 characters"
    )
    private String remark;
}