package com.doc.dto.desigantion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DesignationRequestDto {

    @NotNull(message = "Designation ID cannot be null")
    private Long id;

    @NotBlank(message = "Designation name cannot be empty")
    private String name;

    @NotNull(message = "Weight value cannot be null")
    private Long weightValue;

    @NotNull(message = "Department ID cannot be null")
    private Long departmentId;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;
}