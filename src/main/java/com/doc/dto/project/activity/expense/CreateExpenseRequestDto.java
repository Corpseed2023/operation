package com.doc.dto.project.activity.expense;

import com.doc.em.ExpenseCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateExpenseRequestDto {

    @NotNull(message = "Department ID is required")
    @Positive(message = "Department ID must be greater than zero")
    private Long departmentId;

    @NotNull(message = "Expense category is required")
    private ExpenseCategory expenseCategory;

    @NotNull(message = "Expense amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Expense amount must be greater than zero"
    )
    @Digits(
            integer = 12,
            fraction = 2,
            message = "Expense amount can contain up to 12 digits and 2 decimal places"
    )
    private BigDecimal amount;

    @NotBlank(message = "Expense remark is required")
    @Size(
            max = 2000,
            message = "Expense remark cannot exceed 2000 characters"
    )
    private String remark;

    @PastOrPresent(message = "Expense date cannot be in the future")
    private LocalDateTime expenseDate;

    @NotNull(message = "Created by user ID is required")
    @Positive(message = "Created by user ID must be greater than zero")
    private Long createdByUserId;

    @Size(
            max = 1000,
            message = "Attachment URL cannot exceed 1000 characters"
    )
    private String attachmentUrl;

    @Size(
            max = 150,
            message = "External reference cannot exceed 150 characters"
    )
    private String externalReference;

    @Pattern(
            regexp = "^[A-Za-z]{3}$",
            message = "Currency code must contain exactly three letters"
    )
    private String currencyCode = "INR";
}