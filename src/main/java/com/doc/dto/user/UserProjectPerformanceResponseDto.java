package com.doc.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProjectPerformanceResponseDto {

    private Long userId;

    private String userName;

    private Long totalProjects;

    private Long totalCompletedMilestones;

    private Long completedBeforeTat;

    private Long completedWithinTat;

    private Long delayedMilestones;

    private BigDecimal averagePerformancePercentage;

    private List<UserProjectPerformanceDetailDto> projectPerformance;
}
