package com.doc.entity.vendor;


public enum ProcurementOrderStatus {
    DRAFT,
    PENDING_APPROVAL,      // Sent to Accounts
    APPROVED,
    REJECTED,
    RELEASED,              // Triggered to Vendor
    PARTIALLY_COMPLETED,
    COMPLETED,
    CANCELLED
}
