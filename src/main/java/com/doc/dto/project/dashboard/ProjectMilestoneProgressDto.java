package com.doc.dto.project.dashboard;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMilestoneProgressDto {

    private Long milestoneId;

    private String milestoneName;

    private Integer displayOrder;

    private Integer percentage;

    private Long statusId;

    private String statusName;

    private boolean completed;

    private Long assignedUserId;

    private String assignedUserName;
}
