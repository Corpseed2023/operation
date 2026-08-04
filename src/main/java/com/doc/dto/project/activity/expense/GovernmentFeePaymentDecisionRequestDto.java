package com.doc.dto.project.activity.expense;

import com.doc.em.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GovernmentFeePaymentDecisionRequestDto {

    @NotNull(message = "Payment verification decision is required")
    private ApprovalStatus status;

    @Size(max = 2000, message = "Verification remark cannot exceed 2000 characters")
    private String remark;
}
