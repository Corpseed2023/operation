package com.doc.dto.project.activity.expense;

import com.doc.em.ApprovalStatus;
import com.doc.em.ExpensePaidBy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrtExpenseDecisionRequestDto {

    @NotNull(message = "CRT decision status is required")
    private ApprovalStatus status;

    /**
     * Required only when CRT approves the expense.
     *
     * COMPANY = Accounts team will pay.
     * CLIENT  = Client will pay directly.
     */
    private ExpensePaidBy expensePaidBy;

    @Size(
            max = 2000,
            message = "Remark cannot exceed 2000 characters"
    )
    private String remark;
}