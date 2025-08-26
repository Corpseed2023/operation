package com.doc.entity.project;

/**
 * Enum representing the possible statuses of a project.
 */
public enum ProjectStatus {
    OPEN,        // Initial state, no milestones started
    IN_PROGRESS, // At least one milestone visible and in progress
    COMPLETED,   // All milestones completed
    CANCELLED,   // Project cancelled (e.g., client request)
    REFUNDED     // Project refunded (reverts visibility)
}