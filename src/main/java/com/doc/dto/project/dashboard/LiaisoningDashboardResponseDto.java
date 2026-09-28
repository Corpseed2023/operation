package com.doc.dto.project.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class LiaisoningDashboardResponseDto {
    private long totalProjects;              // total projects that have a Liaisoning milestone
    private double totalProjectAmount;        // sum across all statuses
    private List<LiaisoningStatusSummaryDto> statusSummaries;
}