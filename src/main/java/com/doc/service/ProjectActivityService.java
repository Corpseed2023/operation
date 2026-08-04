package com.doc.service;

import com.doc.dto.project.activity.CreateCommentRequestDto;
import com.doc.dto.project.activity.CreateNoteRequestDto;
import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.dto.project.activity.expense.AccountsExpenseDecisionRequestDto;
import com.doc.dto.project.activity.expense.CreateExpenseRequestDto;
import com.doc.dto.project.activity.expense.CrtExpenseDecisionRequestDto;
import com.doc.dto.project.activity.expense.GovernmentFeeFundTransferRequestDto;
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

    // =========================================================
    // NOTES AND COMMENTS
    // =========================================================

    ProjectActivityResponseDto addNote(
            Long projectId,
            CreateNoteRequestDto request
    );

    ProjectActivityResponseDto addComment(
            Long projectId,
            CreateCommentRequestDto request
    );

    // =========================================================
    // EXPENSE CREATION — STEP 1
    // =========================================================

    /**
     * Technical/originating department raises an expense.
     *
     * Initial state:
     * approvalStage = CRT_REVIEW
     * approvalStatus = PENDING
     * paymentStatus = NOT_INITIATED
     */
    ProjectActivityResponseDto addExpense(
            Long projectId,
            CreateExpenseRequestDto request
    );

    // =========================================================
    // PROJECT ACTIVITY LISTING
    // =========================================================

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

    // =========================================================
    // CRT DECISION — STEP 2
    // =========================================================

    /**
     * CRT declares how the government fee will be funded.
     *
     * CLIENT_TO_COMPANY:
     * Client deposited money into a company bank.
     *
     * COMPANY:
     * Company will fund the fee.
     *
     * CLIENT_DIRECT:
     * Client paid the government portal directly.
     */
    ProjectExpenseResponseDto takeCrtExpenseDecision(
            Long projectId,
            Long expenseId,
            Long userId,
            CrtExpenseDecisionRequestDto request
    );

    // =========================================================
    // ACCOUNTS DECISION — STEP 3
    // =========================================================

    /**
     * Accounts verifies and approves/rejects the expense.
     *
     * CLIENT_TO_COMPANY creates:
     * - RECEIPT voucher
     * - JOURNAL voucher
     *
     * COMPANY creates:
     * - JOURNAL voucher
     */
    ProjectExpenseResponseDto takeAccountsExpenseDecision(
            Long projectId,
            Long expenseId,
            Long userId,
            AccountsExpenseDecisionRequestDto request
    );

    // =========================================================
    // FUND TRANSFER — STEP 4
    // =========================================================

    /**
     * Transfers money between company banks.
     *
     * Example:
     * Dr Axis Bank
     *    Cr HDFC Bank
     *
     * After successful CONTRA posting:
     * paymentStatus = PROCESSING
     *
     * This does not pay the government and does not modify
     * Government Fee Payable.
     */
    ProjectExpenseResponseDto transferGovernmentFeeFunds(
            Long projectId,
            Long expenseId,
            Long userId,
            GovernmentFeeFundTransferRequestDto request
    );

    // =========================================================
    // EXPENSE QUEUES
    // =========================================================

    List<ProjectExpenseResponseDto> getExpenseApprovalQueue(
            Long userId,
            ExpenseApprovalStage approvalStage,
            ApprovalStatus approvalStatus
    );

    List<ProjectExpenseResponseDto> getExpensePaymentQueue(
            Long userId,
            ExpensePaymentStatus paymentStatus
    );

    // =========================================================
    // EXPENSE RETRIEVAL
    // =========================================================

    List<ProjectExpenseResponseDto> getExpensesByProject(
            Long projectId,
            Long userId
    );

    ProjectExpenseResponseDto getExpenseById(
            Long expenseId,
            Long userId
    );
}