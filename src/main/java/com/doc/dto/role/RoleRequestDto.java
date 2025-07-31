package com.doc.dto.role;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleRequestDto {

    @NotBlank(message = "Role name is mandatory")
    private String name;
}
