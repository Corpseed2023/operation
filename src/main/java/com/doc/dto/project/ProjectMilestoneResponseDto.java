package com.doc.dto.project;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProjectMilestoneResponseDto {
    private ProjectDetailsDto projectDetails;
    private List<AssignedMilestoneDto> milestones;
}