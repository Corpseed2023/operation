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
     * RFQ is cancelled before completion.
     *
     * Example:
     * Requirement cancelled or project stopped.
     */
    CANCELLED,

}