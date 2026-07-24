package com.doc.service.project;

import com.doc.dto.project.dashboard.*;

import java.time.LocalDate;
import java.util.List;

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
    List<ProjectStatusCountResponseDto> getProjectStatusWiseSummary(Long userId);
    List<MilestoneOverviewResponseDto> getMilestoneOverview(Long userId);
}