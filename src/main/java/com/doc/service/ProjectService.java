package com.doc.service;

import com.doc.dto.project.ProjectRequestDto;
import com.doc.dto.project.ProjectResponseDto;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;

import java.util.List;

public interface ProjectService {

    ProjectResponseDto  createProject(ProjectRequestDto requestDto);

    ProjectResponseDto getProjectById(Long id);

    List<ProjectResponseDto> getAllProjects(int page, int size);

    ProjectResponseDto updateProject(Long id, ProjectRequestDto requestDto);

    void deleteProject(Long id);

    ProjectResponseDto addPaymentTransaction(Long projectId, ProjectPaymentTransactionDto transactionDto);
}
