package com.doc.entity.research;

import lombok.Getter;

@Getter
public enum TechnicalResearchCaseStatus {

    PENDING_ASSIGNMENT(
            "Pending Assignment",
            "Case is waiting for manager assignment"
    ),

    ASSIGNED(
            "Assigned",
            "Case has been assigned to a technical user"
    ),

    IN_PROGRESS(
            "In Progress",
            "Technical research is in progress"
    ),

    AWAITING_INFORMATION(
            "Awaiting Information",
            "Additional information is required from sales or customer"
    ),

    UNDER_REVIEW(
            "Under Review",
            "Research has been submitted for review"
    ),

    REVISION_REQUIRED(
            "Revision Required",
            "Research requires additional work or correction"
    ),

    COMPLETED(
            "Completed",
            "Research has been reviewed and completed"
    ),

    REJECTED(
            "Rejected",
            "Research case has been rejected"
    ),

    CANCELLED(
            "Cancelled",
            "Research case has been cancelled"
    );

    private final String displayName;
    private final String description;

    TechnicalResearchCaseStatus(
            String displayName,
            String description
    ) {
        this.displayName = displayName;
        this.description = description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}