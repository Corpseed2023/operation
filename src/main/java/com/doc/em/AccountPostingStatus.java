package com.doc.em;

public enum AccountPostingStatus {

    /**
     * Expense is not approved or does not require accounting.
     */
    NOT_REQUIRED,

    /**
     * Approved and waiting to be posted to Account Service.
     */
    PENDING,

    /**
     * Voucher successfully posted in Account Service.
     */
    POSTED,

    /**
     * Client paid directly; no accounting voucher required.
     */
    SKIPPED,

    /**
     * Account Service call failed.
     */
    FAILED
}