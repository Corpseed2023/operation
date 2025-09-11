package com.doc.service;

import com.doc.dto.project.AssignedProjectResponseDto;
import com.doc.dto.project.ProjectMilestoneResponseDto;
import com.doc.dto.project.ProjectRequestDto;
import com.doc.dto.project.ProjectResponseDto;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.entity.project.Project;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProjectService {
    ProjectResponseDto createProject(ProjectRequestDto requestDto);
    List<ProjectResponseDto> getAllProjects(Long userId, int page, int size);
    void deleteProject(Long id);
    ProjectResponseDto addPaymentTransaction(Long projectId, ProjectPaymentTransactionDto transactionDto);
    Page<AssignedProjectResponseDto> getAssignedProjects(Long userId, int page, int size);
    ProjectMilestoneResponseDto getProjectMilestones(Long projectId, Long userId);
    void updateMilestoneVisibilities(Project project, Long updatedById); // Keep this method
}