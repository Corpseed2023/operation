package com.doc.dto.role;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleRequestDto {
    @NotNull(message = "Role ID is mandatory")
    @Min(value = 1, message = "Role ID must be positive")
    private Long id;

    @NotBlank(message = "Role name is mandatory")
    private String name;

    private Long createdBy;

    private Long updatedBy;
}