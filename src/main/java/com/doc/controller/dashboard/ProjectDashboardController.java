package com.doc.controller.dashboard;

import com.doc.dto.project.dashboard.*;
import com.doc.dto.user.UserProjectPerformanceResponseDto;
import com.doc.service.project.ProjectDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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

    @GetMapping("/status-wise-summary")
    public ResponseEntity<List<ProjectStatusCountResponseDto>>
    getProjectStatusWiseSummary(
            @RequestParam Long userId
    ) {

        return ResponseEntity.ok(
                projectDashboardService
                        .getProjectStatusWiseSummary(userId)
        );
    }
    @GetMapping("/milestone-overview")
    public ResponseEntity<List<MilestoneOverviewResponseDto>>
    getMilestoneOverview(
            @RequestParam Long userId
    ) {

        return ResponseEntity.ok(
                projectDashboardService.getMilestoneOverview(userId)
        );
    }

    @GetMapping("/team-workload")
    public ResponseEntity<List<TeamWorkloadResponseDto>> getTeamWorkload(
            @RequestParam Long userId
    ) {

        return ResponseEntity.ok(
                projectDashboardService.getTeamWorkload(userId)
        );
    }

    @GetMapping("/due-risk-queue")
    public ResponseEntity<List<DueRiskQueueResponseDto>> getDueRiskQueue(
            @RequestParam Long userId,

            @RequestParam(
                    required = false,
                    defaultValue = "7"
            )
            Integer upcomingDays,

            @RequestParam(
                    required = false,
                    defaultValue = "5"
            )
            Integer limit
    ) {

        return ResponseEntity.ok(
                projectDashboardService.getDueRiskQueue(
                        userId,
                        upcomingDays,
                        limit
                )
        );
    }


    @GetMapping("/milestone-tracker")
    public ResponseEntity<
            Page<ProjectMilestoneTrackerResponseDto>
            > getMilestoneTracker(

            @RequestParam Long userId,

            @RequestParam (required = false) Long departmentId,

            @RequestParam(required = false)
            Long stageId,

            @RequestParam(required = false)
            String search,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        Page<ProjectMilestoneTrackerResponseDto> response =
                projectDashboardService
                        .getMilestoneTracker(
                                userId,
                                stageId,
                                search,
                                page,
                                size
                        );

        return ResponseEntity.ok(response);
    }


/// User performance for milestones assigned to the user,
/// grouped by project
    @GetMapping("/user-performance/{userId}")
    public ResponseEntity<UserProjectPerformanceResponseDto>
    getUserProjectPerformance(
            @PathVariable Long userId,
            @RequestParam(required = false) Long projectId
    ) {

        return ResponseEntity.ok(
                projectDashboardService.getUserProjectPerformance(
                        userId,
                        projectId
                )
        );
    }
}