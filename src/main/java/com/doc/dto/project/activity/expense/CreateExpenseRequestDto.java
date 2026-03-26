package com.doc.dto.project.activity.expense;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateExpenseRequestDto {

    private String expenseType;

    private BigDecimal amount;

    private String remark;

    private LocalDateTime expenseDate;

    private Long createdByUserId;

    private String paymentMedium;        // e.g., "CASH", "ONLINE", "UPI", "CARD", etc.

}