package com.doc.entity.vendor;

/**
 * VendorOnboardingStatus represents onboarding workflow
 * after vendor is finalized from RFQ/Award process.
 */
public enum VendorOnboardingStatus {

    /**
     * Onboarding started after vendor award.
     * Registration form/agreement has to be sent to vendor.
     */
    ONBOARDING_PENDING,

    /**
     * Registration form, agreement, PAN, GST, bank details etc.
     * are received from vendor.
     */
    DOCUMENTS_RECEIVED,

    /**
     * Procurement team verified documents, commercial terms,
     * selected quotation, scope and service/category.
     */
    PROCUREMENT_VERIFIED,

    /**
     * Vendor onboarding is sent to legal team for agreement review.
     */
    LEGAL_REVIEW,

    /**
     * Legal team requested changes in agreement/document.
     */
    LEGAL_REWORK,

    /**
     * Legal team approved agreement.
     */
    LEGAL_APPROVED,

    /**
     * Vendor onboarding is sent to accounts team for bank/GST/TDS verification.
     */
    ACCOUNTS_REVIEW,

    /**
     * Accounts rejected or requested correction.
     */
    ACCOUNTS_REJECTED,

    /**
     * Accounts verified bank details, GST, PAN, TDS and payment details.
     */
    ACCOUNTS_APPROVED,

    /**
     * Vendor onboarding completed and vendor is active for future projects.
     */
    ACTIVE,

    /**
     * Onboarding cancelled before activation.
     */
    CANCELLED
}