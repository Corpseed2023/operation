package com.doc.dto.department;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentRequestDto {

    private String name;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;

}