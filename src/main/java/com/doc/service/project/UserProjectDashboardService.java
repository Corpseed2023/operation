package com.doc.service.project;

import com.doc.dto.project.dashboard.UserProjectDashboardResponseDto;

import java.time.LocalDate;

public interface UserProjectDashboardService {

    UserProjectDashboardResponseDto getUserProjectDashboard(
            Long userId,
            Boolean currentMonth,
            LocalDate fromDate,
            LocalDate toDate
    );
}