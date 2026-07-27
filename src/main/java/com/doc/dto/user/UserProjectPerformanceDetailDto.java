package com.doc.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProjectPerformanceDetailDto {

    private Long projectId;

    private String projectNumber;

    private String projectName;

    private Long productId;

    private String productName;

    private Long totalAssignedMilestones;

    private Long completedMilestones;

    private Long beforeTatCount;

    private Long withinTatCount;

    private Long delayedCount;

    private BigDecimal performancePercentage;
}
