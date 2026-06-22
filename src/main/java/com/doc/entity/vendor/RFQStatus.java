package com.doc.entity.vendor;

/**
 * RFQStatus represents the overall status of one RFQ.
 *
 * Example:
 * RFQ-CDSO-0622 is created for Cement + Steel.
 * This enum tracks the main RFQ lifecycle.
 */
public enum RFQStatus {

    /**
     * RFQ is created but not sent to any vendor yet.
     *
     * Example:
     * Procurement team created RFQ for Cement + Steel,
     * but vendors are not added/sent yet.
     */
    DRAFT,

    /**
     * RFQ has been sent to one or multiple vendors.
     *
     * Example:
     * RFQ sent to Balaji Traders, Shree Enterprises, and RK Suppliers.
     */
    SENT,

    /**
     * At least one vendor has submitted quotation.
     *
     * Example:
     * Balaji submitted quotation of Rs. 8,50,000.
     */
    QUOTATION_RECEIVED,

    /**
     * Procurement team is comparing vendor quotations.
     *
     * Example:
     * Comparing price, delivery time, quality, warranty, and payment terms.
     */
    COMPARISON_PENDING,

    /**
     * One or multiple vendors are selected for this RFQ.
     *
     * Example:
     * Balaji selected for Cement and RK Suppliers selected for Steel Bars.
     */
    VENDOR_SELECTED,

    /**
     * RFQ is cancelled before completion.
     *
     * Example:
     * Requirement cancelled or project stopped.
     */
    CANCELLED,

    /**
     * RFQ process is fully closed.
     *
     * Example:
     * Vendor selected, onboarding done, PO created, and no further RFQ action pending.
     */
    CLOSED
}