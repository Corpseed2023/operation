package com.doc.dto.project.portal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProjectPortalApprovalQueueResponseDto {

    private Long userId;
    private String requestedStatus;
    private int totalRequests;

    private List<ProjectPortalDetailResponseDto> requests =
            new ArrayList<>();
}