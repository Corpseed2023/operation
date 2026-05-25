package com.doc.entity.vendor;

public enum PaymentRequestStatus {
    PENDING,                    // Submitted by Procurement
    UNDER_REVIEW,               // Accounts checking
    APPROVED,                   // Ready for payment
    PAYMENT_PROCESSING,
    PAYMENT_RELEASED,
    REJECTED,
    ON_HOLD
}