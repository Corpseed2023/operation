package com.doc.entity.vendor;

/**
 * VendorQuotationStatus represents quotation lifecycle.
 *
 * This status belongs only to VendorQuotation.
 * Do not use this status for RFQ, RFQVendor, Vendor, Award, Onboarding
 */
public enum VendorQuotationStatus {

    /**
     * Quotation is entered but not finalized.
     *
     * Example:
     * Procurement started entering quotation details,
     * but item rates or attachment are still pending.
     */
    DRAFT,

    /**
     * Vendor quotation is submitted/received.
     *
     * Example:
     * Vendor sent quotation by mail and procurement manually entered it in ERP.
     */
    SUBMITTED,

    /**
     * Vendor revised the quotation.
     *
     * Example:
     * Vendor first quoted Rs. 25,000,
     * then revised it to Rs. 22,000.
     */
    REVISED,

    /**
     * Quotation is under comparison by procurement team.
     *
     * Example:
     * Procurement is comparing price, TAT, scope, payment terms and experience.
     */
    UNDER_COMPARISON,

    /**
     * Quotation is accepted fully.
     *
     * Example:
     * All quotation items of this vendor are selected/awarded.
     */
    ACCEPTED,

    /**
     * Quotation is partially accepted.
     *
     * Example:
     * Vendor quotation has 3 items,
     * but only 1 or 2 items are awarded.
     *
     * This is important because RFQVendorAward supports item-wise award.
     */
    PARTIALLY_ACCEPTED,

    /**
     * Quotation is rejected after comparison.
     *
     * Example:
     * Vendor was not selected because price was high,
     * TAT was long or scope was incomplete.
     */
    REJECTED,

    /**
     * Quotation is cancelled.
     *
     * Example:
     * RFQ cancelled, vendor withdrew quotation,
     * or quotation was entered incorrectly and cancelled.
     */
    CANCELLED,
    AGREEMENT_SENT_TO_PROCUREMENT,
    AGREEMENT_SENT_TO_VENDOR
}