package com.doc.service;

import com.doc.dto.project.activity.expense.GovernmentFeeFundTransferRequestDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;

public interface ExpenseAccountPostingService {

    /**
     * Step 3:
     * Creates approval-time accounting entries.
     *
     * CLIENT_TO_COMPANY:
     * - RECEIPT voucher
     * - JOURNAL voucher
     *
     * COMPANY:
     * - JOURNAL voucher
     */
    void postGovernmentFeeExpense(
            Long expenseId
    );

    /**
     * Retries a failed Step 3 Account Service posting.
     */
    ProjectExpenseResponseDto retryGovernmentFeePosting(
            Long expenseId,
            Long userId
    );

    /**
     * Step 4:
     * Transfers government-fee funds between company banks.
     *
     * Example:
     * Dr Axis Bank
     *    Cr HDFC Bank
     *
     * On success, payment status changes from PENDING
     * to PROCESSING.
     */
    ProjectExpenseResponseDto transferGovernmentFeeFunds(
            Long expenseId,
            Long userId,
            GovernmentFeeFundTransferRequestDto request
    );
}