package com.doc.entity.vendor;

/**
 * VendorFinalizationStatus represents the lifecycle after quotation comparison
 * and before vendor onboarding.
 */
public enum VendorFinalizationStatus {

    /**
     * Finalization is being prepared but not completed.
     */
    DRAFT,

    /**
     * Vendor is finalized for item/service after quotation comparison.
     */
    FINALIZED,

    /**
     * Finalized vendor moved to onboarding stage.
     * Vendor form, agreement, NDA and bank detail form are sent.
     */
    ONBOARDING_STARTED,

    /**
     * Finalization cancelled before onboarding.
     */
    CANCELLED,

    /**
     * Finalized vendor details sent to Accounts Department for verification.
     */
    SENT_TO_ACCOUNTS,

    /**
     * Accounts Department verified and approved finalized vendor details.
     */
    ACCOUNTS_APPROVED,

    /**
     * Accounts Department rejected finalized vendor details.
     */
    ACCOUNTS_REJECTED
}