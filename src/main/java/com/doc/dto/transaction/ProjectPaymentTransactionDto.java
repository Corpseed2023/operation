package com.doc.dto.transaction;

import com.doc.entity.project.ProjectPaymentTransaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ProjectPaymentTransactionDto {

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotNull(message = "Payment date cannot be null")
    private Date paymentDate;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;

    private ProjectPaymentTransaction.TransactionType transactionType;

}

