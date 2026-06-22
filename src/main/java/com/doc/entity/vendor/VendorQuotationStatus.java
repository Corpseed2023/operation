package com.doc.entity.vendor;

/**
 * VendorQuotationStatus represents quotation lifecycle.
 */
public enum VendorQuotationStatus {

    /**
     * Quotation is entered but not finalized.
     */
    DRAFT,

    /**
     * Vendor quotation is submitted/received.
     */
    SUBMITTED,

    /**
     * Vendor revised the quotation.
     */
    REVISED,

    /**
     * Quotation is under comparison by procurement team.
     */
    UNDER_COMPARISON,

    /**
     * Quotation is accepted fully or partially.
     */
    ACCEPTED,

    /**
     * Quotation is rejected after comparison.
     */
    REJECTED,

    /**
     * Quotation is cancelled.
     */
    CANCELLED
}