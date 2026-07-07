package com.doc.dto.project.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProjectDashboardResponseDto {

    private Long userId;

    private Long totalProjects;

    private Long runningProjects;

    private Long openProjects;

    private Long inProgressProjects;

    private Long completedProjects;

    private Long cancelledProjects;

    private Long refundedProjects;

    private Long reopenedProjects;

    private List<ProjectStatusCountDto> statusCounts;


}