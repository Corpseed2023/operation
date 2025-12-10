package com.doc.service;

import com.doc.dto.document.DocumentChecklistDTO;
import com.doc.dto.project.*;
import com.doc.dto.project.projectHistory.MilestoneHistoryResponseDto;
import com.doc.dto.project.projectHistory.ProjectHistoryResponseDto;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.entity.project.Project;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProjectService {

    ProjectResponseDto createProject(ProjectRequestDto requestDto);

    List<ProjectResponseDto> getAllProjects(Long userId, int page, int size);

    long getProjectCount(Long userId);

    ProjectResponseDto addPaymentTransaction(Long projectId, ProjectPaymentTransactionDto transactionDto);

    Page<AssignedProjectResponseDto> getAssignedProjects(Long userId, int page, int size);

    ProjectMilestoneResponseDto getProjectMilestones(Long projectId, Long userId);

    void updateMilestoneVisibilities(Project project, Long updatedById);

    ProjectResponseDto addPaymentByUnbilledNumber(String unbilledNumber, ProjectPaymentTransactionDto dto);

    void deleteProject(Long id);


    ProjectHistoryResponseDto getProjectHistory(Long projectId);

    // In ProjectService.java
    MilestoneHistoryResponseDto getMilestoneHistory(Long projectId, Long milestoneId, Long requestingUserId);


    void setApplicantType(Long projectId, Long applicantId);

    List<DocumentChecklistDTO> getDocumentChecklist(Long projectId, Long milestoneId);

    void checkMilestoneAccess(Long projectId, Long milestoneId, Long userId);
}