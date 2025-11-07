package com.doc.service;

import com.doc.dto.project.*;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.entity.project.Project;
import org.springframework.data.domain.Page;

public interface ProjectService {

    ProjectResponseDto createProject(ProjectRequestDto requestDto);

    Page<ProjectResponseDto> getAllProjects(Long userId, int page, int size);

    void deleteProject(Long id);

    ProjectResponseDto addPaymentTransaction(Long projectId, ProjectPaymentTransactionDto transactionDto);

    Page<AssignedProjectResponseDto> getAssignedProjects(Long userId, int page, int size);

    ProjectMilestoneResponseDto getProjectMilestones(Long projectId, Long userId);

    void updateMilestoneVisibilities(Project project, Long updatedById);

    ProjectResponseDto addPaymentByUnbilledNumber(String unbilledNumber, ProjectPaymentTransactionDto dto);
}