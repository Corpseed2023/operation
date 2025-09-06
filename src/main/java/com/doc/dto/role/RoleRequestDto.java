package com.doc.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleRequestDto {

    @NotNull(message = "Role ID cannot be null")
    private Long id;

    @NotBlank(message = "Role name is mandatory")
    private String name;
}