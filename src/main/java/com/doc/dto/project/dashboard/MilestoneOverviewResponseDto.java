package com.doc.dto.project.dashboard;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneOverviewResponseDto {

    private Long milestoneId;

    private String milestoneName;

    private Long totalProjects;

    private Long completedProjects;

    private BigDecimal completionPercentage;
}
