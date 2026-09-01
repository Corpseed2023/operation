package com.doc.service.project;

import com.doc.dto.project.activity.CreateCommentRequestDto;
import com.doc.dto.project.activity.CreateNoteRequestDto;
import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.dto.project.activity.expense.AccountsExpenseDecisionRequestDto;
import com.doc.dto.project.activity.expense.CreateExpenseRequestDto;
import com.doc.dto.project.activity.expense.CrtExpenseDecisionRequestDto;
import com.doc.dto.project.activity.expense.GovernmentFeeFundTransferRequestDto;
import com.doc.dto.project.activity.expense.GovernmentFeePaymentRequestDto;
import com.doc.dto.project.activity.expense.GovernmentFeePaymentDecisionRequestDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;
import com.doc.em.ActivityType;
import com.doc.em.ApprovalStatus;
import com.doc.em.ExpenseApprovalStage;
import com.doc.em.ExpensePaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ProjectActivityService {

    ProjectActivityResponseDto addNote(
            Long projectId,
            CreateNoteRequestDto request
    );

    ProjectActivityResponseDto addComment(
            Long projectId,
            CreateCommentRequestDto request
    );

    ProjectActivityResponseDto addExpense(
            Long projectId,
            CreateExpenseRequestDto request
    );

    Page<ProjectActivityResponseDto> getAllActivities(
            Long projectId,
            Pageable pageable
    );

    Page<ProjectActivityResponseDto> getActivitiesByType(
            Long projectId,
            ActivityType type,
            Pageable pageable
    );

    Page<ProjectActivityResponseDto> getActivitiesByDateRange(
            Long projectId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    ProjectExpenseResponseDto takeCrtExpenseDecision(
            Long projectId,
            Long expenseId,
            Long userId,
            CrtExpenseDecisionRequestDto request
    );

    ProjectExpenseResponseDto takeAccountsExpenseDecision(
            Long projectId,
            Long expenseId,
            Long userId,
            AccountsExpenseDecisionRequestDto request
    );

    List<ProjectExpenseResponseDto> getExpenseApprovalQueue(
            Long userId,
            ExpenseApprovalStage approvalStage,
            ApprovalStatus approvalStatus
    );

    List<ProjectExpenseResponseDto> getExpensePaymentQueue(
            Long userId,
            ExpensePaymentStatus paymentStatus
    );

    List<ProjectExpenseResponseDto> getExpensesByProject(
            Long projectId,
            Long userId
    );

    ProjectExpenseResponseDto getExpenseById(
            Long expenseId,
            Long userId
    );

    /** Step 4 project-level entry point used by ProjectExpenseController. */
    ProjectExpenseResponseDto transferGovernmentFeeFunds(
            Long projectId,
            Long expenseId,
            Long userId,
            GovernmentFeeFundTransferRequestDto request
    );

    /** Step 5A: Technical submits portal payment details and receipt. */
    ProjectExpenseResponseDto submitGovernmentFeePaymentProof(
            Long projectId,
            Long expenseId,
            Long userId,
            GovernmentFeePaymentRequestDto request
    );

    /** Step 5B: Accounts verifies or rejects the submitted payment proof. */
    ProjectExpenseResponseDto takeGovernmentFeePaymentDecision(
            Long projectId,
            Long expenseId,
            Long userId,
            GovernmentFeePaymentDecisionRequestDto request
    );
}
