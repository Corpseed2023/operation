package com.doc.entity.vendor;

/**
 * RFQVendorAwardStatus represents the lifecycle of vendor award/selection.
 */
public enum RFQVendorAwardStatus {


    /**
     * Award is being prepared but not finalized.
     */
    DRAFT,


    /**
     * Vendor is selected/awarded for item or service.
     */
    AWARDED,

    /**
     * Award is approved internally by procurement/manager.
     */
    APPROVED,

    /**
     * Award moved to vendor onboarding stage.
     */
    ONBOARDING_STARTED,

}