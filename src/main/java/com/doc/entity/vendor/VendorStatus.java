package com.doc.entity.vendor;

public enum VendorStatus {

    PROSPECTIVE("Prospective", "Basic vendor created but not fully onboarded"),

    ONBOARDING("Onboarding", "Vendor selected and onboarding/legal/accounts verification is in progress"),

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


    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}