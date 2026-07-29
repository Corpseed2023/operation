package com.doc.service;

import com.doc.dto.project.lifecycle.CreateProjectLifecycleRequestDto;
import com.doc.dto.project.lifecycle.ProjectLifecycleDecisionDto;
import com.doc.dto.project.lifecycle.ProjectLifecycleResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProjectLifecycleRequestService {

    ProjectLifecycleResponseDto createRequest(
            CreateProjectLifecycleRequestDto requestDto
    );

    ProjectLifecycleResponseDto reviewRequest(
            Long requestId,
            ProjectLifecycleDecisionDto decisionDto
    );

    Page<ProjectLifecycleResponseDto> getPendingRequests(
            Long adminUserId,
            int page,
            int size
    );

    Page<ProjectLifecycleResponseDto> getMyRequests(
            Long userId,
            int page,
            int size
    );

    List<ProjectLifecycleResponseDto> getProjectRequestHistory(
            Long projectId,
            Long userId
    );

    ProjectLifecycleResponseDto getRequestById(
            Long requestId,
            Long userId
    );
}