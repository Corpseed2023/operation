package com.doc.impl;

import com.doc.dto.project.activity.*;
import com.doc.dto.project.activity.expense.ApproveExpenseRequestDto;
import com.doc.dto.project.activity.expense.CreateExpenseRequestDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;
import com.doc.em.ActivityType;
import com.doc.em.ApprovalStatus;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectActivity;
import com.doc.entity.project.activity.ProjectComment;
import com.doc.entity.project.activity.ProjectExpense;
import com.doc.entity.project.activity.ProjectNote;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
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

import java.math.BigDecimal;
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
    public ProjectActivityResponseDto addExpense(Long projectId, CreateExpenseRequestDto request) {

        // Validate User
        User user = validateUser(request.getCreatedByUserId());

        // Validate Project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId, "ERR_PROJECT_NOT_FOUND"));

        // Basic validations
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Expense amount must be greater than zero", "ERR_INVALID_AMOUNT");
        }

        if (request.getExpenseType() == null || request.getExpenseType().trim().isEmpty()) {
            throw new ValidationException("Expense type is required", "ERR_EXPENSE_TYPE_REQUIRED");
        }

        String summary = request.getExpenseType().trim() + " - ₹" + request.getAmount();

        // Create Activity
        ProjectActivity activity = createActivity(
                project,
                ActivityType.EXPENSE,
                "Expense Added",
                summary,
                user
        );

        activity = activityRepository.save(activity);

        // Create Expense
        ProjectExpense expense = new ProjectExpense();
        expense.setProject(project);
        expense.setActivity(activity);
        expense.setExpenseType(request.getExpenseType().trim());
        expense.setAmount(request.getAmount());
        expense.setRemark(request.getRemark());
        expense.setExpenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDateTime.now());
        expense.setPaymentMedium(request.getPaymentMedium());
        expense.setCreatedByUserId(user.getId());
        expense.setCreatedByUserName(user.getFullName());
        expense.setApprovalStatus(ApprovalStatus.PENDING);   // Default status

        expenseRepository.save(expense);

        // Use clean DTO instead of raw entity to avoid circular reference
        ProjectExpenseResponseDto expenseDto = mapToExpenseDto(expense);

        return mapResponse(activity, expenseDto);
    }



    @Override
    @Transactional
    public void approveExpense(Long projectId, Long userId, Long expenseId, ApproveExpenseRequestDto request) {

        User user = validateUser(userId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));

        ProjectExpense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found", "ERR_EXPENSE_NOT_FOUND"));

        if (!expense.getProject().getId().equals(project.getId())) {
            throw new ValidationException("Expense does not belong to this project", "ERR_INVALID_PROJECT");
        }


        ApprovalStatus newStatus;
        try {
            newStatus = ApprovalStatus.valueOf(request.getStatus().toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status. Allowed: APPROVED, REJECTED, ON_HOLD", "ERR_INVALID_STATUS");
        }

        if (newStatus == ApprovalStatus.PENDING) {
            throw new ValidationException("Cannot manually set status to PENDING", "ERR_INVALID_STATUS");
        }

        // Prevent re-processing final states
        if (expense.getApprovalStatus() == ApprovalStatus.APPROVED ||
                expense.getApprovalStatus() == ApprovalStatus.REJECTED) {
            throw new ValidationException("Expense is already " + expense.getApprovalStatus(), "ERR_ALREADY_PROCESSED");
        }

        // ==================== REJECTION REMARK VALIDATION ====================
        if (newStatus == ApprovalStatus.REJECTED) {
            if (request.getRejectionRemark() == null || request.getRejectionRemark().trim().isEmpty()) {
                throw new ValidationException("Rejection remark is required when rejecting an expense", "ERR_REJECTION_REMARK_REQUIRED");
            }
            expense.setRejectionRemark(request.getRejectionRemark().trim());
        } else {
            expense.setRejectionRemark(null);
        }

        expense.setApprovalStatus(newStatus);
        expense.setApproved(newStatus == ApprovalStatus.APPROVED);
        expense.setApprovedByUserId(user.getId());
        expense.setApprovedByUserName(user.getFullName();
        expense.setApprovedDate(LocalDateTime.now());

        expenseRepository.save(expense);

        createExpenseApprovalActivity(project, expense, user, newStatus);
    }

    private void createExpenseApprovalActivity(Project project, ProjectExpense expense, User user, ApprovalStatus status) {
        ProjectActivity activity = new ProjectActivity();
        activity.setProject(project);
        activity.setActivityType(ActivityType.EXPENSE);
        activity.setSystemGenerated(true);
        activity.setCreatedByUserId(user.getId());
        activity.setCreatedByUserName(user.getFullName());
        activity.setActivityDate(LocalDateTime.now());
        activity.setCreatedDate(LocalDateTime.now());
        activity.setDeleted(false);

        if (status == ApprovalStatus.APPROVED) {
            activity.setTitle("Expense Approved");
            activity.setSummary("Expense of ₹" + expense.getAmount() + " approved by " + user.getFullName());
        } else if (status == ApprovalStatus.REJECTED) {
            activity.setTitle("Expense Rejected");
            activity.setSummary("Expense of ₹" + expense.getAmount() + " rejected by " + user.getFullName());
        } else if (status == ApprovalStatus.ON_HOLD) {
            activity.setTitle("Expense On Hold");
            activity.setSummary("Expense of ₹" + expense.getAmount() + " kept on hold by " + user.getFullName());
        }

        activityRepository.save(activity);
    }



    private User validateUser(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }


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



    @Override
    public List<ProjectExpenseResponseDto> getExpensesByProject(Long projectId, Long userId) {

        // ==================== VALIDATE USER ====================
        validateUser(userId);

        // ==================== VALIDATE PROJECT ====================
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId,
                        "ERR_PROJECT_NOT_FOUND"));

        // Fetch all expenses for this project, ordered by expense date descending
        List<ProjectExpense> expenses = expenseRepository
                .findByProjectIdOrderByExpenseDateDesc(projectId);

        // Convert to DTO list
        return expenses.stream()
                .map(this::mapToExpenseDto)
                .toList();
    }

    private ProjectExpenseResponseDto mapToExpenseDto(ProjectExpense expense) {
        ProjectExpenseResponseDto dto = new ProjectExpenseResponseDto();

        // Expense fields
        dto.setExpenseId(expense.getId());
        dto.setActivityId(expense.getActivity() != null ? expense.getActivity().getId() : null);
        dto.setExpenseType(expense.getExpenseType());
        dto.setAmount(expense.getAmount());
        dto.setRemark(expense.getRemark());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setPaymentMedium(expense.getPaymentMedium());
        dto.setApprovalStatus(expense.getApprovalStatus());
        dto.setApproved(expense.isApproved());
        dto.setApprovedByUserId(expense.getApprovedByUserId());
        dto.setCreatedByUserId(expense.getCreatedByUserId());
        dto.setCreatedByUserName(expense.getCreatedByUserName());

        if (expense.getProject() != null) {
            Project project = expense.getProject();
            dto.setProjectId(project.getId());
            dto.setProjectNo(project.getProjectNo());
            dto.setProjectName(project.getName());
            dto.setUnbilledNumber(project.getUnbilledNumber());
            dto.setProductName(project.getProduct().getProductName());
        }

        return dto;
    }

    @Override
    public List<ProjectExpenseResponseDto> getExpenseList(Long userId, ApprovalStatus approvalStatus) {

        // Validate user exists and is active
        validateUser(userId);

        List<ProjectExpense> expenses;

        if (approvalStatus == null || approvalStatus == ApprovalStatus.ALL) {
            // Fetch ALL expenses ordered by expense date descending
            expenses = expenseRepository.findAllExpensesOrderByExpenseDateDesc();
        } else {
            // Fetch expenses by specific approval status
            expenses = expenseRepository.findByApprovalStatusOrderByExpenseDateDesc(approvalStatus);
        }

        // Convert entity list to DTO list
        return expenses.stream()
                .map(this::mapToExpenseDto)
                .toList();
    }






}