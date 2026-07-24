package com.doc.dto.project.activity.expense;

import com.doc.em.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateExpenseRequestDto {

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Expense category is required")
    private ExpenseCategory expenseCategory;

    @NotNull(message = "Expense amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Expense amount must be greater than zero"
    )
    private BigDecimal amount;

    @NotBlank(message = "Expense remark is required")
    @Size(
            max = 2000,
            message = "Expense remark cannot exceed 2000 characters"
    )
    private String remark;

    @PastOrPresent(
            message = "Expense date cannot be in the future"
    )
    private LocalDateTime expenseDate;

    /**
     * In a production-standard application this should preferably
     * be obtained from the authenticated JWT user.
     */
    @NotNull(message = "Created by user ID is required")
    private Long createdByUserId;

    @Size(
            max = 1000,
            message = "Attachment URL cannot exceed 1000 characters"
    )
    private String attachmentUrl;

    /**
     * Optional FSSAI application number, challan number,
     * portal ID or another external reference.
     */
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