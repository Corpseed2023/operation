package com.doc.dto.project.dashboard;

import com.doc.entity.project.ProjectPriority;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMilestoneTrackerResponseDto {

    private Long projectId;

    private String projectNumber;

    private BigDecimal projectValue;

    private Long companyId;

    private String companyName;

    private Long productId;

    private String serviceName;

    private Long stageId;

    private String stage;

    private Integer overallPercentage;

    private Long currentMilestoneId;

    private String currentMilestoneName;

    private Long pendingDocumentCount;

    private LocalDate dueDate;

    private ProjectPriority priority;

    private Long ownerId;

    private String ownerName;

    @Builder.Default
    private List<ProjectMilestoneProgressDto> milestones =
            new ArrayList<>();
}
