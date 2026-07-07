package com.doc.dto.project.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectOverviewResponseDto {

    private Long userId;

    private Boolean currentMonth;

    private LocalDate fromDate;

    private LocalDate toDate;

    private Long totalProjects;

    private List<ProjectOverviewCardDto> cards;
}