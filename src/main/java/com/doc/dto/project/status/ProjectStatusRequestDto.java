package com.doc.dto.project.status;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectStatusRequestDto {

    @NotBlank(message = "Status name is required")
    private String name;

    private String description;
}