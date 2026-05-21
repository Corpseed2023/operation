package com.doc.entity.project;   // or com.doc.entity.vendor - you can decide

/**
 * Procurement Milestone Status Enum
 * Used in ProcurementMilestoneAssignment entity
 */
public enum ProcurementStatus {

    DRAFT("Draft", "Initial stage - just received assignment"),
    REQUIREMENT_FINALIZED("Requirement Finalized", "Scope and requirements understood"),
    VENDOR_SHORTLISTED("Vendor Shortlisted", "Vendors evaluated and shortlisted"),
    VENDOR_FINALIZED("Vendor Finalized", "Best vendor selected and approved"),
    PO_CREATED("PO Created", "Purchase Order drafted"),
    PO_APPROVED("PO Approved", "PO approved internally"),
    PO_RELEASED("PO Released", "PO sent to vendor"),
    ADVANCE_PAID("Advance Paid", "Advance payment processed (if applicable)"),
    IN_PROGRESS("In Progress", "Vendor has started the work"),
    UNDER_REVIEW("Under Review", "Vendor submitted proofs - under quality check"),
    COMPLETED("Completed", "Work completed and verified"),
    PAYMENT_REQUESTED("Payment Requested", "Payment request sent to Accounts team"),
    PAYMENT_DONE("Payment Done", "Final payment released"),
    ON_HOLD("On Hold", "Work is temporarily on hold"),
    CANCELLED("Cancelled", "Procurement cancelled"),
    REJECTED("Rejected", "Vendor work rejected - rework required");

    private final String displayName;
    private final String description;

    ProcurementStatus(String displayName, String description) {
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