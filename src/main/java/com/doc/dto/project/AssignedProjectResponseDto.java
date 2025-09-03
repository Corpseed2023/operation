package com.doc.dto.project;

import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignedProjectResponseDto {
    private ProjectDetailsDto project;
    private List<AssignedMilestoneDto> assignedMilestones;
}
