package com.doc.entity.project;

/**
 * Enum representing the possible statuses of a project milestone assignment.
 */
public enum MilestoneStatus {
    NEW,         // Initial state after becoming visible
    IN_PROGRESS, // User has started working (e.g., calling client)
    ON_HOLD,     // Milestone paused (e.g., awaiting client response)
    COMPLETED,   // Milestone finished (e.g., documents verified)
    REJECTED     // Milestone failed (e.g., invalid documents, requires rework)
}