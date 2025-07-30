package com.doc.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentTypeRequestDto {

    @NotBlank(message = "Payment type name cannot be empty")
    private String name;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;

    @NotNull(message = "Updated by user ID cannot be null")
    private Long updatedBy;
}
