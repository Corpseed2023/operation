package com.doc.impl;

import com.doc.dto.project.activity.*;
import com.doc.em.ActivityType;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectActivity;
import com.doc.entity.project.activity.ProjectComment;
import com.doc.entity.project.activity.ProjectExpense;
import com.doc.entity.project.activity.ProjectNote;
import com.doc.entity.user.User;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.projectRepo.activity.ProjectActivityRepository;
import com.doc.repository.projectRepo.activity.ProjectCommentRepository;
import com.doc.repository.projectRepo.activity.ProjectExpenseRepository;
import com.doc.repository.projectRepo.activity.ProjectNoteRepository;
import com.doc.service.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectActivityServiceImpl implements ProjectActivityService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectActivityRepository activityRepository;
    private final ProjectNoteRepository noteRepository;
    private final ProjectCommentRepository commentRepository;
    private final ProjectExpenseRepository expenseRepository;

    // ---------------------------------------------------
    // CREATE NOTE
    // ---------------------------------------------------

    @Override
    public ProjectActivityResponseDto addNote(Long projectId, CreateNoteRequestDto request) {

        User user = validateUser(request.getCreatedByUserId());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectActivity activity = createActivity(
                project,
                ActivityType.NOTE,
                "Note Added",
                request.getNoteText(),
                user
        );

        activity = activityRepository.save(activity);

        ProjectNote note = new ProjectNote();
        note.setProject(project);
        note.setActivity(activity);
        note.setNoteText(request.getNoteText());
        note.setCreatedDate(LocalDateTime.now());
        note.setCreatedByUserId(user.getId());
        note.setCreatedByUserName(user.getFullName());

        noteRepository.save(note);

        return mapResponse(activity, note);
    }

    // ---------------------------------------------------
    // CREATE COMMENT
    // ---------------------------------------------------

    @Override
    public ProjectActivityResponseDto addComment(Long projectId, CreateCommentRequestDto request) {

        User user = validateUser(request.getCreatedByUserId());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectActivity activity = createActivity(
                project,
                ActivityType.COMMENT,
                "Comment Added",
                request.getCommentText(),
                user
        );

        activity = activityRepository.save(activity);

        ProjectComment comment = new ProjectComment();
        comment.setProject(project);
        comment.setActivity(activity);
        comment.setCommentText(request.getCommentText());
        comment.setParentCommentId(request.getParentCommentId());
        comment.setCreatedDate(LocalDateTime.now());
        comment.setCreatedByUserId(user.getId());
        comment.setCreatedByUserName(user.getFullName());

        commentRepository.save(comment);

        return mapResponse(activity, comment);
    }

    // ---------------------------------------------------
    // CREATE EXPENSE
    // ---------------------------------------------------

    @Override
    public ProjectActivityResponseDto addExpense(Long projectId, CreateExpenseRequestDto request) {

        User user = validateUser(request.getCreatedByUserId());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        String summary = request.getExpenseType() + " ₹" + request.getAmount();

        ProjectActivity activity = createActivity(
                project,
                ActivityType.EXPENSE,
                "Expense Added",
                summary,
                user
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
        expense.setCreatedByUserId(user.getId());
        expense.setCreatedByUserName(user.getFullName());

        expenseRepository.save(expense);

        return mapResponse(activity, expense);
    }

    // ---------------------------------------------------
    // FETCH ALL ACTIVITIES
    // ---------------------------------------------------

    @Override
    public Page<ProjectActivityResponseDto> getAllActivities(Long projectId, Pageable pageable) {

        pageable = normalizePageable(pageable);

        Page<ProjectActivity> page = activityRepository
                .findByProjectIdAndDeletedFalseOrderByActivityDateDesc(projectId, pageable);

        List<ProjectActivityResponseDto> content = page.getContent()
                .stream()
                .map(activity -> {

                    if (activity.getActivityType() == ActivityType.COMMENT) {

                        ProjectComment comment =
                                commentRepository.findByActivityId(activity.getId()).orElse(null);

                        if (comment != null && comment.getParentCommentId() != null) {
                            return null; // hide child comments
                        }
                    }

                    return mapTimeline(activity);

                })
                .filter(Objects::nonNull)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                content,
                pageable,
                page.getTotalElements()
        );
    }

    // ---------------------------------------------------
    // FETCH BY TYPE
    // ---------------------------------------------------

    @Override
    public Page<ProjectActivityResponseDto> getActivitiesByType(Long projectId, ActivityType type, Pageable pageable) {

        pageable = normalizePageable(pageable);

        if (type == ActivityType.COMMENT) {
            return activityRepository
                    .findParentCommentActivities(projectId, type, pageable)
                    .map(this::mapTimeline);
        }

        return activityRepository
                .findByProjectIdAndActivityTypeAndDeletedFalseOrderByActivityDateDesc(projectId, type, pageable)
                .map(this::mapTimeline);
    }

    // ---------------------------------------------------
    // FETCH BY DATE RANGE
    // ---------------------------------------------------

    @Override
    public Page<ProjectActivityResponseDto> getActivitiesByDateRange(
            Long projectId,
            LocalDate start,
            LocalDate end,
            Pageable pageable) {

        pageable = normalizePageable(pageable);

        Page<ProjectActivity> page = activityRepository
                .findByProjectIdAndActivityDateBetweenAndDeletedFalseOrderByActivityDateDesc(
                        projectId,
                        start.atStartOfDay(),
                        end.atTime(23, 59, 59),
                        pageable
                );

        List<ProjectActivityResponseDto> content = page.getContent()
                .stream()
                .map(activity -> {

                    if (activity.getActivityType() == ActivityType.COMMENT) {

                        ProjectComment comment =
                                commentRepository.findByActivityId(activity.getId()).orElse(null);

                        if (comment != null && comment.getParentCommentId() != null) {
                            return null;
                        }
                    }

                    return mapTimeline(activity);

                })
                .filter(Objects::nonNull)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                content,
                pageable,
                page.getTotalElements()
        );
    }



    @Override
    @Transactional
    public void approveExpense(Long projectId, Long userId, Long expenseId) {

        // Validate user
        User user = validateUser(userId);

        // Fetch project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Fetch expense
        ProjectExpense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // Validate expense belongs to project
        if (!expense.getProject().getId().equals(project.getId())) {
            throw new RuntimeException("Expense does not belong to the given project");
        }

        // Check if already approved
        if (expense.isApproved()) {
            throw new RuntimeException("Expense is already approved");
        }

        // ✅ Approve expense
        expense.setApproved(true);
        expense.setApprovedByUserId(user.getId());
        expense.setApprovalStatus("APPROVED");

        expenseRepository.save(expense);

        // ✅ Create Project Activity
        ProjectActivity activity = new ProjectActivity();
        activity.setProject(project);
        activity.setActivityType(ActivityType.EXPENSE);

        activity.setTitle("Expense Approved");

        activity.setSummary(
                "Expense of " + expense.getAmount() + " " + expense.getCurrency() +
                        " approved by " + user.getFullName()
        );

        activity.setActivityDate(LocalDateTime.now());

        activity.setCreatedByUserId(user.getId());
        activity.setCreatedByUserName(user.getFullName());

        activity.setSystemGenerated(true);
        activity.setDeleted(false);

        activity.setCreatedDate(LocalDateTime.now());
        activity.setUpdatedDate(LocalDateTime.now());

        activityRepository.save(activity);
    }

    // ---------------------------------------------------
    // VALIDATE USER
    // ---------------------------------------------------

    private User validateUser(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    // ---------------------------------------------------
    // CREATE ACTIVITY
    // ---------------------------------------------------

    private ProjectActivity createActivity(
            Project project,
            ActivityType type,
            String title,
            String summary,
            User user
    ) {

        ProjectActivity activity = new ProjectActivity();
        activity.setProject(project);
        activity.setActivityType(type);
        activity.setTitle(title);
        activity.setSummary(summary);
        activity.setActivityDate(LocalDateTime.now());
        activity.setCreatedByUserId(user.getId());
        activity.setCreatedByUserName(user.getFullName());
        activity.setCreatedDate(LocalDateTime.now());
        activity.setDeleted(false);

        return activity;
    }

    // ---------------------------------------------------
    // MAP RESPONSE
    // ---------------------------------------------------

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

    // ---------------------------------------------------
    // TIMELINE MAPPER
    // ---------------------------------------------------

    private ProjectActivityResponseDto mapTimeline(ProjectActivity activity) {

        Object details = null;

        switch (activity.getActivityType()) {

            case NOTE:
                details = noteRepository.findByActivityId(activity.getId()).orElse(null);
                break;

            case COMMENT:

                ProjectComment rootComment =
                        commentRepository.findByActivityId(activity.getId()).orElse(null);

                if (rootComment != null) {

                    List<ProjectComment> allComments =
                            commentRepository.findByProjectId(activity.getProject().getId());

                    details = buildCommentTree(rootComment, allComments);
                }

                break;

            case EXPENSE:
                details = expenseRepository.findByActivityId(activity.getId()).orElse(null);
                break;
        }

        return mapResponse(activity, details);
    }

    // ---------------------------------------------------
    // BUILD COMMENT TREE
    // ---------------------------------------------------

    private ProjectCommentResponseDto buildCommentTree(ProjectComment root, List<ProjectComment> allComments) {

        ProjectCommentResponseDto dto = new ProjectCommentResponseDto();
        dto.setId(root.getId());
        dto.setCommentText(root.getCommentText());
        dto.setParentCommentId(root.getParentCommentId());
        dto.setCreatedDate(root.getCreatedDate());
        dto.setCreatedByUserId(root.getCreatedByUserId());
        dto.setCreatedByUserName(root.getCreatedByUserName());

        List<ProjectCommentResponseDto> children = allComments.stream()
                .filter(c -> Objects.equals(root.getId(), c.getParentCommentId()))
                .map(child -> buildCommentTree(child, allComments))
                .toList();

        dto.setChildren(children);

        return dto;
    }

    private Pageable normalizePageable(Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();

        if (page > 0) {
            page = page - 1;
        }

        return org.springframework.data.domain.PageRequest.of(
                page,
                size,
                pageable.getSort()
        );
    }
}