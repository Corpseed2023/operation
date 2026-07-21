package com.doc.service.project;

import com.doc.dto.project.dashboard.ProjectOverviewResponseDto;
import com.doc.dto.project.dashboard.UserProjectDashboardResponseDto;
import com.doc.repository.projection.VendorAssignmentCountProjection;

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


}