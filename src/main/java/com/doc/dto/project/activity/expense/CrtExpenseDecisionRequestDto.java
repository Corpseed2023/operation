package com.doc.dto.project.activity.expense;

import com.doc.em.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrtExpenseDecisionRequestDto {

    /**
     * Allowed:
     * APPROVED, REJECTED, ON_HOLD
     */
    @NotNull(message = "CRT decision status is required")
    private ApprovalStatus status;

    /**
     * Required when status is REJECTED or ON_HOLD.
     */
    @Size(
            max = 2000,
            message = "Decision remark cannot exceed 2000 characters"
    )
    private String remark;
}