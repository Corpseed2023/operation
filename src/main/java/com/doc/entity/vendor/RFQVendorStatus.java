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
     * Vendor is not selected after comparison.
     *
     * Example:
     * Shree Enterprises rejected due to high price.
     */
    REJECTED
}