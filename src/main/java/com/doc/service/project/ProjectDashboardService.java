package com.doc.service.project;

import com.doc.dto.project.dashboard.*;
import com.doc.dto.user.UserProjectPerformanceResponseDto;
import org.springframework.data.domain.Page;

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
    List<ProjectStatusCountResponseDto> getProjectStatusWiseSummary(
            Long userId, LocalDate fromDate, LocalDate toDate
    );
    List<TeamWorkloadResponseDto> getTeamWorkload(
            Long userId, LocalDate fromDate, LocalDate toDate
    );

    List<MilestoneOverviewResponseDto> getMilestoneOverview(
            Long userId, LocalDate fromDate, LocalDate toDate
    );
     List<DueRiskQueueResponseDto> getDueRiskQueue(
            Long userId,
            Integer upcomingDays,
            Integer limit
    );
    Page<ProjectMilestoneTrackerResponseDto> getMilestoneTracker(
            Long userId,
            Long stageId,
            String search,
            int page,
            int size
    );


    UserProjectPerformanceResponseDto getUserProjectPerformance(
            Long userId,
            Long projectId
    );
}