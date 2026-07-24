package com.doc.em;

public enum ExpenseApprovalStage {

    CRT_REVIEW,
    ACCOUNTS_REVIEW,

    /**
     * Workflow finished by approval, rejection or cancellation.
     */
    COMPLETED
}