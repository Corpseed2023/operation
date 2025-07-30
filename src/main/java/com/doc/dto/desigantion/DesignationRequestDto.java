package com.doc.dto.desigantion;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
// do one thing use JPA query for
@Getter
@Setter
public class DesignationRequestDto {

    @NotBlank(message = "Designation name cannot be empty")
    private String name;

    @NotNull(message = "Weight value cannot be null")
    @PositiveOrZero(message = "Weight value must be a non-negative number")
    private Long weightValue;

    @NotNull(message = "Department ID cannot be null")
    private Long departmentId;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;


}