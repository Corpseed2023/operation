package com.doc.service;

import com.doc.dto.project.activity.expense.GovernmentFeeFundTransferRequestDto;
import com.doc.dto.project.activity.expense.GovernmentFeePaymentRequestDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;

public interface ExpenseAccountPostingService {

    /** Step 3: posts approval-time government-fee vouchers. */
    void postGovernmentFeeExpense(Long expenseId);

    /** Retries only a failed Step 3 Account Service posting. */
    ProjectExpenseResponseDto retryGovernmentFeePosting(
            Long expenseId,
            Long userId
    );

    /**
     * Step 4: creates the inter-bank CONTRA voucher and changes payment status
     * from PENDING to PROCESSING after Account Service confirms the posting.
     */
    ProjectExpenseResponseDto transferGovernmentFeeFunds(
            Long expenseId,
            Long userId,
            GovernmentFeeFundTransferRequestDto request
    );

    /**
     * Step 5: posts the final PAYMENT voucher and marks the expense PAID only
     * after Account Service confirms the voucher.
     */
    ProjectExpenseResponseDto completeGovernmentFeePayment(
            Long expenseId,
            Long userId,
            GovernmentFeePaymentRequestDto request
    );
}
