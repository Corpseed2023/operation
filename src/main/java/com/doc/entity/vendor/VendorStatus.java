package com.doc.entity.vendor;

import org.hibernate.annotations.Comment;

/**
 * Vendor Status Enum - Similar to ProjectStatus / MilestoneStatus in your system
 */
public enum VendorStatus {

    ACTIVE("Active", "Vendor is active and can be used"),
    INACTIVE("Inactive", "Vendor is temporarily inactive"),
    UNDER_REVIEW("Under Review", "Vendor is being evaluated"),
    BLACKLISTED("Blacklisted", "Vendor is blacklisted due to poor performance or issues"),
    SUSPENDED("Suspended", "Vendor is suspended temporarily"),
    VERIFIED("Verified", "Vendor has been fully verified and approved");

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