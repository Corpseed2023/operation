package com.doc.service.project;

import com.doc.dto.project.dashboard.ProjectCompletionResponseDto;
import com.doc.dto.project.dashboard.ProjectOverviewResponseDto;
import com.doc.dto.project.dashboard.UserProjectDashboardResponseDto;

import java.time.LocalDate;

public interface ProjectDashboardService {

    UserProjectDashboardResponseDto getUserProjectDashboard(
            Long userId,
            Boolean currentMonth,
            LocalDate fromDate,
            LocalDate toDate
    );

    ProjectOverviewResponseDto getProjectOverview(
            Long userId,
            Boolean currentMonth,
            LocalDate fromDate,
            LocalDate toDate
    );
    ProjectCompletionResponseDto getProjectCompletionSummary(Long userId);
}