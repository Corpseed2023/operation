package com.doc.controller.dashboard;

import com.doc.dto.project.dashboard.ProjectCompletionResponseDto;
import com.doc.dto.project.dashboard.ProjectOverviewResponseDto;
import com.doc.dto.project.dashboard.UserProjectDashboardResponseDto;
import com.doc.service.project.ProjectDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/operationService/api/user-dashboard")
public class ProjectDashboardController {

    private final ProjectDashboardService projectDashboardService;

    public ProjectDashboardController(ProjectDashboardService projectDashboardService) {
        this.projectDashboardService = projectDashboardService;
    }

    @GetMapping("/projects")
    @Operation(
            summary = "Get user project dashboard",
            description = "Returns total project count, running project count, and status-wise project count for the given user."
    )
    public ResponseEntity<UserProjectDashboardResponseDto> getUserProjectDashboard(
            @RequestParam Long userId,

            @RequestParam(required = false, defaultValue = "false")
            Boolean currentMonth,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        UserProjectDashboardResponseDto response =
                projectDashboardService.getUserProjectDashboard(
                        userId,
                        currentMonth,
                        fromDate,
                        toDate
                );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/overview")
    @Operation(
            summary = "Get project overview cards",
            description = "Returns project overview card counts and percentages for In Progress, Awaiting Documents, and Delayed projects."
    )
    public ResponseEntity<ProjectOverviewResponseDto> getProjectOverview(
            @RequestParam Long userId,

            @RequestParam(required = false, defaultValue = "false")
            Boolean currentMonth,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        ProjectOverviewResponseDto response =
                projectDashboardService.getProjectOverview(
                        userId,
                        currentMonth,
                        fromDate,
                        toDate
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/completion-summary")
    public ResponseEntity<ProjectCompletionResponseDto>
    getProjectCompletionSummary(
            @RequestParam Long userId
    ) {

        return ResponseEntity.ok(
                projectDashboardService
                        .getProjectCompletionSummary(userId)
        );
    }
}