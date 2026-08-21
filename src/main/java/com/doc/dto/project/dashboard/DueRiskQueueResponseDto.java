package com.doc.dto.project.dashboard;

import com.doc.entity.project.ProjectPriority;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DueRiskQueueResponseDto {

    private Long projectId;

    private String companyName;

    private String projectNumber;

    private Long milestoneId;

    private String milestoneName;

    private LocalDate dueDate;

    private Long ownerId;

    private String ownerName;

    private ProjectPriority priority;

    private boolean overdue;

    private long overdueDays;
}
