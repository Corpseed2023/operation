package com.doc.service;

import com.doc.dto.project.activity.CreateCommentRequestDto;
import com.doc.dto.project.activity.CreateExpenseRequestDto;
import com.doc.dto.project.activity.CreateNoteRequestDto;
import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.em.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

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
}
