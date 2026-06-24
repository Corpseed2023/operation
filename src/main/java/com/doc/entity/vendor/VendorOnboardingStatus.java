package com.doc.entity.vendor;

/**
 * VendorOnboardingStatus represents onboarding workflow
 * after vendor is finalized from RFQ finalization process.
 */
public enum VendorOnboardingStatus {

    /**
     * Onboarding record created after vendor finalization.
     * Registration form, NDA, agreement and bank detail form are not sent yet.
     */
    ONBOARDING_PENDING,

    /**
     * Vendor registration form, NDA, agreement and bank detail form
     * are sent to vendor.
     */
    FORM_SENT_TO_VENDOR,

    /**
     * Vendor submitted / acknowledged onboarding documents.
     *
     * Example:
     * Filled vendor form, signed NDA, signed agreement, PAN, GST,
     * cancelled cheque / bank proof, MSME certificate if applicable.
     */
    DOCUMENTS_RECEIVED,

    /**
     * Documents are waiting for legal team verification.
     */
    LEGAL_REVIEW_PENDING,

    /**
     * Legal team approved NDA and agreement.
     */
    LEGAL_APPROVED,

    /**
     * Legal team rejected or requested correction in NDA/agreement.
     */
    LEGAL_REJECTED,

    /**
     * Documents are waiting for accounts team verification.
     */
    ACCOUNTS_REVIEW_PENDING,

    /**
     * Accounts team approved PAN, GST, bank and payment details.
     */
    ACCOUNTS_APPROVED,

    /**
     * Accounts team rejected or requested correction in PAN/GST/bank/payment details.
     */
    ACCOUNTS_REJECTED,

    /**
     * Vendor onboarding completed and vendor master is activated.
     */
    VENDOR_ACTIVATED,

    /**
     * Onboarding cancelled before activation.
     */
    CANCELLED
}