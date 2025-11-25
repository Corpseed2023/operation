package com.doc.service;

import com.doc.dto.project.status.ProjectStatusRequestDto;
import com.doc.dto.project.status.ProjectStatusResponseDto;

import java.util.List;

public interface ProjectStatusService {
    ProjectStatusResponseDto createStatus(ProjectStatusRequestDto requestDto);
    ProjectStatusResponseDto updateStatus(Long id, ProjectStatusRequestDto requestDto);
    List<ProjectStatusResponseDto> getAllStatuses();
    ProjectStatusResponseDto getStatusById(Long id);
    void deleteStatus(Long id);
}