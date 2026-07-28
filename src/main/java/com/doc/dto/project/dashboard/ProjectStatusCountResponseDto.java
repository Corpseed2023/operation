package com.doc.dto.project.dashboard;

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
public class ProjectStatusCountResponseDto {

    private Long statusId;

    private String statusName;

    private Long projectCount;

    private BigDecimal percentage;
}
