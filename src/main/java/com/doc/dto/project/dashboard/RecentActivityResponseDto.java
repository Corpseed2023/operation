package com.doc.dto.project.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecentActivityResponseDto {
    private String activityType;   // MILESTONE_COMPLETED, PROJECT_IN_PROGRESS, PROJECT_REWORK, PROJECT_COMPLETED
    private String title;          // "Milestone completed", "Project moved to In Progress", etc.
    private String description;    // "Documentation completed for ABC Recycling Pvt. Ltd."
    private String colorCode;      // GREEN, BLUE, ORANGE — for the UI dot
    private LocalDateTime timestamp;
}