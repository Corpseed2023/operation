package com.doc.service;

import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;

public interface ExpenseAccountPostingService {

    void postGovernmentFeeExpense(Long expenseId);

    ProjectExpenseResponseDto retryGovernmentFeePosting(
            Long expenseId,
            Long userId
    );
}