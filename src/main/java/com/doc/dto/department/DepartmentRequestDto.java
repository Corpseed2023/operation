package com.doc.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentRequestDto {

    @NotNull(message = "Department ID cannot be null")
    private Long id;

    @NotBlank(message = "Department name cannot be empty")
    private String name;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;
}