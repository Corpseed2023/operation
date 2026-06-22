package com.doc.entity.vendor;

/**
 * RFQVendorStatus represents vendor-wise status inside one RFQ.
 *
 * Example:
 * One RFQ can be sent to Balaji, Shree, and RK.
 * Each vendor can have a different status.
 */
public enum RFQVendorStatus {

    /**
     * Vendor is added to RFQ but RFQ email is not sent yet.
     *
     * Example:
     * Balaji Traders added in RFQ vendor list.
     */
    ADDED,

    /**
     * RFQ has been sent to this specific vendor.
     *
     * Example:
     * RFQ email sent to Balaji Traders.
     */
    SENT,

    /**
     * Vendor has viewed/opened the RFQ.
     *
     * Optional status if your system tracks email open or portal view.
     */
    VIEWED,

    /**
     * Vendor quotation has been received or entered in system.
     *
     * Example:
     * Balaji submitted quotation of Rs. 8,50,000.
     */
    QUOTATION_RECEIVED,

    /**
     * Vendor did not respond before quotation deadline.
     *
     * Example:
     * RFQ sent to Shree Enterprises but no quotation received.
     */
    NO_RESPONSE,

    /**
     * Vendor refused to participate in the RFQ.
     *
     * Example:
     * Vendor says they cannot supply material.
     */
    DECLINED,

    /**
     * Vendor is shortlisted after initial comparison.
     *
     * Example:
     * Balaji and RK are shortlisted, Shree is rejected.
     */
    SHORTLISTED,

    /**
     * Vendor is finally selected for full or partial work.
     *
     * Example:
     * Balaji selected for Cement.
     * RK selected for Steel Bars.
     */
    SELECTED,

    /**
     * Vendor is not selected after comparison.
     *
     * Example:
     * Shree Enterprises rejected due to high price.
     */
    REJECTED
}