package com.doc.service;



import com.doc.dto.project.status.ProjectStatusRequestDto;
import com.doc.dto.project.status.ProjectStatusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectStatusService {

    ProjectStatusResponseDto createStatus(ProjectStatusRequestDto requestDto);

    ProjectStatusResponseDto updateStatus(Long id, ProjectStatusRequestDto requestDto);

    Page<ProjectStatusResponseDto> getAllStatuses(Pageable pageable);

    ProjectStatusResponseDto getStatusById(Long id);

    void deleteStatus(Long id);
}