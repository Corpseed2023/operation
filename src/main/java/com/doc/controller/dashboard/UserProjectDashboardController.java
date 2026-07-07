package com.doc.controller.dashboard;

import com.doc.dto.project.dashboard.UserProjectDashboardResponseDto;
import com.doc.service.project.UserProjectDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/operationService/api/user-dashboard")
public class UserProjectDashboardController {

    private final UserProjectDashboardService userProjectDashboardService;

    public UserProjectDashboardController(
            UserProjectDashboardService userProjectDashboardService
    ) {
        this.userProjectDashboardService = userProjectDashboardService;
    }

    @GetMapping("/projects")
    @Operation(
            summary = "Get user project dashboard",
            description = "Returns total project count, running project count, and status-wise project count for the given user."
    )
    public ResponseEntity<UserProjectDashboardResponseDto> getUserProjectDashboard(
            @RequestParam Long userId,

            // true = current month data
            @RequestParam(required = false, defaultValue = "false") Boolean currentMonth,

            // custom date range
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        UserProjectDashboardResponseDto response =
                userProjectDashboardService.getUserProjectDashboard(
                        userId,
                        currentMonth,
                        fromDate,
                        toDate
                );

        return ResponseEntity.ok(response);
    }
}