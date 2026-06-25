package com.doc.dto.project.sales;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentWiseMilestoneDto {

    private Long departmentId;
    private String departmentName;

    private long totalMilestones;
    private long completedMilestones;
    private long inProgressMilestones;
    private long pendingMilestones;

    private List<MilestoneAssignmentStatusDto> milestones;
}