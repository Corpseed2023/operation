package com.doc.dto.project.dashboard;

import lombok.*;

import java.math.BigDecimal;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCompletionResponseDto {

    private Long totalProjectCount;

    private Long completedProjectCount;

    private BigDecimal completionPercentage;
}
