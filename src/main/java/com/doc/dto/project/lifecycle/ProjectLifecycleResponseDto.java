package com.doc.dto.project.lifecycle;

import com.doc.entity.project.ProjectLifecycleAction;
import com.doc.entity.project.ProjectLifecycleRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProjectLifecycleResponseDto {

    private Long id;

    private Long projectId;
    private String projectNumber;
    private String projectName;

    private ProjectLifecycleAction actionType;
    private ProjectLifecycleRequestStatus requestStatus;

    private Long requestedById;
    private String requestedByName;

    private Long reviewedById;
    private String reviewedByName;

    private String requestReason;
    private String reviewRemark;

    private Long previousProjectStatusId;
    private String previousProjectStatusName;

    private Long currentProjectStatusId;
    private String currentProjectStatusName;

    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
}