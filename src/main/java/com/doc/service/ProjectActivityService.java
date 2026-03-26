package com.doc.service;

import com.doc.dto.project.activity.CreateCommentRequestDto;
import com.doc.dto.project.activity.expense.ApproveExpenseRequestDto;
import com.doc.dto.project.activity.expense.CreateExpenseRequestDto;
import com.doc.dto.project.activity.CreateNoteRequestDto;
import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;
import com.doc.em.ActivityType;
import com.doc.em.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ProjectActivityService {

    ProjectActivityResponseDto addNote(Long projectId, CreateNoteRequestDto request);

    ProjectActivityResponseDto addComment(Long projectId, CreateCommentRequestDto request);

    ProjectActivityResponseDto addExpense(Long projectId, CreateExpenseRequestDto request);

    Page<ProjectActivityResponseDto> getAllActivities(Long projectId, Pageable pageable);

    Page<ProjectActivityResponseDto> getActivitiesByType(Long projectId, ActivityType type, Pageable pageable);

    Page<ProjectActivityResponseDto> getActivitiesByDateRange(
            Long projectId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );



    List<ProjectExpenseResponseDto> getExpenseList(Long userId, ApprovalStatus approvalStatus);


    List<ProjectExpenseResponseDto> getExpensesByProject(Long projectId, Long userId);

    void approveExpense(Long projectId, Long userId, Long expenseId, ApproveExpenseRequestDto request);


}
