package com.doc.dto.project.activity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateExpenseRequestDto {

    private String expenseType;

    private BigDecimal amount;

    private String currency;

    private String description;

    private LocalDateTime expenseDate;

    private Long createdByUserId;
}
