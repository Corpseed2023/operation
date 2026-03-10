package com.doc.impl;

import com.doc.dto.project.activity.CreateCommentRequestDto;
import com.doc.dto.project.activity.CreateExpenseRequestDto;
import com.doc.dto.project.activity.CreateNoteRequestDto;
import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.em.ActivityType;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectActivity;
import com.doc.entity.project.activity.ProjectComment;
import com.doc.entity.project.activity.ProjectExpense;
import com.doc.entity.project.activity.ProjectNote;
import com.doc.repository.ProjectRepository;
import com.doc.repository.projectRepo.activity.ProjectActivityRepository;
import com.doc.repository.projectRepo.activity.ProjectCommentRepository;
import com.doc.repository.projectRepo.activity.ProjectExpenseRepository;
import com.doc.repository.projectRepo.activity.ProjectNoteRepository;
import com.doc.service.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectActivityServiceImpl implements ProjectActivityService {

    private final ProjectRepository projectRepository;
    private final ProjectActivityRepository activityRepository;
    private final ProjectNoteRepository noteRepository;
    private final ProjectCommentRepository commentRepository;
    private final ProjectExpenseRepository expenseRepository;

    @Override
    public ProjectActivityResponseDto addNote(Long projectId, CreateNoteRequestDto request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectActivity activity = createActivity(
                project,
                ActivityType.NOTE,
                "Note Added",
                request.getNoteText(),
                request.getCreatedByUserId(),
                request.getCreatedByUserName()
        );

        activity = activityRepository.save(activity);

        ProjectNote note = new ProjectNote();
        note.setProject(project);
        note.setActivity(activity);
        note.setNoteText(request.getNoteText());
        note.setCreatedDate(LocalDateTime.now());

        noteRepository.save(note);

        return mapResponse(activity, note);
    }

    @Override
    public ProjectActivityResponseDto addComment(Long projectId, CreateCommentRequestDto request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectActivity activity = createActivity(
                project,
                ActivityType.COMMENT,
                "Comment Added",
                request.getCommentText(),
                request.getCreatedByUserId(),
                request.getCreatedByUserName()
        );

        activity = activityRepository.save(activity);

        ProjectComment comment = new ProjectComment();
        comment.setProject(project);
        comment.setActivity(activity);
        comment.setCommentText(request.getCommentText());
        comment.setParentCommentId(request.getParentCommentId());
        comment.setCreatedDate(LocalDateTime.now());

        commentRepository.save(comment);

        return mapResponse(activity, comment);
    }

    @Override
    public ProjectActivityResponseDto addExpense(Long projectId, CreateExpenseRequestDto request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        String summary = request.getExpenseType() + " ₹" + request.getAmount();

        ProjectActivity activity = createActivity(
                project,
                ActivityType.EXPENSE,
                "Expense Added",
                summary,
                request.getCreatedByUserId(),
                null
        );

        activity = activityRepository.save(activity);

        ProjectExpense expense = new ProjectExpense();
        expense.setProject(project);
        expense.setActivity(activity);
        expense.setExpenseType(request.getExpenseType());
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setCreatedByUserId(request.getCreatedByUserId());

        expenseRepository.save(expense);

        return mapResponse(activity, expense);
    }

    @Override
    public Page<ProjectActivityResponseDto> getAllActivities(Long projectId, Pageable pageable) {

        return activityRepository
                .findByProjectIdAndDeletedFalseOrderByActivityDateDesc(projectId, pageable)
                .map(this::mapTimeline);
    }

    @Override
    public Page<ProjectActivityResponseDto> getActivitiesByType(Long projectId, ActivityType type, Pageable pageable) {

        return activityRepository
                .findByProjectIdAndActivityTypeAndDeletedFalseOrderByActivityDateDesc(projectId, type, pageable)
                .map(this::mapTimeline);
    }

    @Override
    public Page<ProjectActivityResponseDto> getActivitiesByDateRange(
            Long projectId,
            LocalDate start,
            LocalDate end,
            Pageable pageable) {

        return activityRepository
                .findByProjectIdAndActivityDateBetweenAndDeletedFalseOrderByActivityDateDesc(
                        projectId,
                        start.atStartOfDay(),
                        end.atTime(23, 59, 59),
                        pageable
                )
                .map(this::mapTimeline);
    }

    private ProjectActivity createActivity(
            Project project,
            ActivityType type,
            String title,
            String summary,
            Long userId,
            String userName
    ) {

        ProjectActivity activity = new ProjectActivity();
        activity.setProject(project);
        activity.setActivityType(type);
        activity.setTitle(title);
        activity.setSummary(summary);
        activity.setActivityDate(LocalDateTime.now());
        activity.setCreatedByUserId(userId);
        activity.setCreatedByUserName(userName);
        activity.setCreatedDate(LocalDateTime.now());
        activity.setDeleted(false);

        return activity;
    }

    private ProjectActivityResponseDto mapResponse(ProjectActivity activity, Object details) {

        ProjectActivityResponseDto dto = new ProjectActivityResponseDto();
        dto.setActivityId(activity.getId());
        dto.setActivityType(activity.getActivityType());
        dto.setTitle(activity.getTitle());
        dto.setSummary(activity.getSummary());
        dto.setActivityDate(activity.getActivityDate());
        dto.setCreatedByUserId(activity.getCreatedByUserId());
        dto.setCreatedByUserName(activity.getCreatedByUserName());
        dto.setDetails(details);

        return dto;
    }

    private ProjectActivityResponseDto mapTimeline(ProjectActivity activity) {

        Object details = null;

        switch (activity.getActivityType()) {

            case NOTE:
                details = noteRepository.findByActivityId(activity.getId()).orElse(null);
                break;

            case COMMENT:
                details = commentRepository.findByActivityId(activity.getId()).orElse(null);
                break;

            case EXPENSE:
                details = expenseRepository.findByActivityId(activity.getId()).orElse(null);
                break;
        }

        return mapResponse(activity, details);
    }
}