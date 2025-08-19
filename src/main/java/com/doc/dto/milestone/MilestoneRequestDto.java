package com.doc.dto.milestone;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO for creating or updating a Milestone.
 */
@Getter
@Setter
@NoArgsConstructor
public class MilestoneRequestDto {

    @NotBlank(message = "Milestone name cannot be empty")
    private String name;

    private String description;

    private List<Long> departmentIds;

}