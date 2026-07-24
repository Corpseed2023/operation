package com.doc.em;

public enum ExpensePaymentStatus {

    /**
     * Expense is not finally approved, so payment cannot begin.
     */
    NOT_INITIATED,

    /**
     * Expense is approved and waiting for Accounts payment.
     */
    PENDING,

    /**
     * Payment has been initiated with bank/payment gateway.
     */
    PROCESSING,

    /**
     * Only part of the approved amount has been paid.
     */
    PARTIALLY_PAID,

    /**
     * Complete approved amount has been paid.
     */
    PAID,

    /**
     * Payment attempt failed.
     */
    FAILED,

    /**
     * Previously completed payment was reversed.
     */
    REVERSED,

    /**
     * Payment was cancelled before completion.
     */
    CANCELLED,

    /**
     * Client paid government fee directly.
     */
    CLIENT_PAID,



}