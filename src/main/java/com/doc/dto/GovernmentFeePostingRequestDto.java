package com.doc.dto;


import com.doc.em.ExpensePaidBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentFeePostingRequestDto {

    /**
     * ProjectExpense ID from Operation Service.
     * Used as the idempotency source ID.
     */
    private Long operationExpenseId;

    private Long projectId;

    private String projectNo;

    private String projectName;

    private String expenseCategory;

    private BigDecimal approvedAmount;

    private String currencyCode;

    private LocalDate expenseDate;

    private ExpensePaidBy paidBy;

    private Long approvedByUserId;

    private String approvedByUserName;

    private String narration;
}
