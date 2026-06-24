package com.doc.entity.vendor;

/**
 * RFQStatus represents the overall status of one RFQ.
 *
 * Important:
 * This is RFQ-level status, not vendor-wise status.
 *
 * Example:
 * RFQ is sent to 3 vendors.
 * Vendor 1 may be SELECTED.
 * Vendor 2 may be REJECTED.
 * Vendor 3 may be QUOTATION_RECEIVED.
 *
 * But RFQ-level status tells the overall lifecycle of the RFQ.
 */
public enum RFQStatus {

    /**
     * RFQ is created but not sent to any vendor yet.
     */
    DRAFT,

    /**
     * RFQ has been sent to one or multiple vendors.
     */
    SENT,

    /**
     * At least one vendor quotation is received
     * and procurement can compare quotations.
     */
    UNDER_COMPARISON,

    /**
     * One or more vendors have been finalized.
     */
    VENDOR_FINALIZED,

    /**
     * Vendor onboarding has started after finalization.
     */
    ONBOARDING_STARTED,

    /**
     * RFQ process is completed.
     */
    CLOSED,

    /**
     * RFQ is cancelled before completion.
     */
    CANCELLED
}