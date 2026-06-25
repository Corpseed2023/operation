package com.doc.service;

import com.doc.dto.project.reopen.ProjectReopenCreateRequestDto;
import com.doc.dto.project.reopen.ProjectReopenDecisionDto;
import com.doc.dto.project.reopen.ProjectReopenRequestResponseDto;

import java.util.List;

public interface ProjectReopenRequestService {

    ProjectReopenRequestResponseDto createReopenRequest(ProjectReopenCreateRequestDto dto);

    ProjectReopenRequestResponseDto requesterManagerDecision(
            Long requestId,
            ProjectReopenDecisionDto dto
    );

    ProjectReopenRequestResponseDto responsibleManagerDecision(
            Long requestId,
            ProjectReopenDecisionDto dto
    );

    List<ProjectReopenRequestResponseDto> getRequesterManagerPendingRequests(Long managerId);

    List<ProjectReopenRequestResponseDto> getResponsibleManagerPendingRequests(Long managerId);

    List<ProjectReopenRequestResponseDto> getProjectReopenRequests(Long projectId);
}