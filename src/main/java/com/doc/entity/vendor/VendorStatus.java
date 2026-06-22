package com.doc.entity.vendor;

/**
 * Vendor master status.
 *
 * Important:
 * This status is only for vendor master.
 * Onboarding statuses like LEGAL_APPROVED, ACCOUNTS_APPROVED
 * should be maintained separately in VendorOnboardingStatus.
 */
public enum VendorStatus {

    PROSPECTIVE("Prospective", "Basic vendor created but not fully onboarded"),

    ACTIVE("Active", "Vendor is fully approved and can be used for PO creation"),

    INACTIVE("Inactive", "Vendor is temporarily inactive"),

    BLACKLISTED("Blacklisted", "Vendor is blacklisted due to poor performance or compliance issue"),

    SUSPENDED("Suspended", "Vendor is temporarily suspended");

    private final String displayName;
    private final String description;

    VendorStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}