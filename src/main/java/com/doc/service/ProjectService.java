package com.doc.service;

import com.doc.dto.project.AssignedProjectResponseDto;
import com.doc.dto.project.ProjectRequestDto;
import com.doc.dto.project.ProjectResponseDto;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProjectService {

    ProjectResponseDto  createProject(ProjectRequestDto requestDto);

    ProjectResponseDto getProjectById(Long id);

    ProjectResponseDto updateProject(Long id, ProjectRequestDto requestDto);

    void deleteProject(Long id);

    ProjectResponseDto addPaymentTransaction(Long projectId, ProjectPaymentTransactionDto transactionDto);

    List<ProjectResponseDto> getAllProjects(Long userId, int page, int size);

    Page<AssignedProjectResponseDto> getAssignedProjects(Long userId, int page, int size);
}
