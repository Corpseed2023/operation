package com.doc.dto.milestone;

import com.doc.dto.department.DepartmentResponseDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO for returning Milestone data.
 */
@Getter
@Setter
@NoArgsConstructor
public class MilestoneResponseDto {

    private Long id;
    private String name;
    private String description;
    private List<DepartmentResponseDto> departmentResponseDtos;
}