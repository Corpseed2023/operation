package com.doc.entity.project;

import lombok.Getter;

/**
 * Enum representing the status of a project document upload.
 */
@Getter
public enum DocumentStatus {
    PENDING("Document not yet uploaded"),
    UPLOADED("Document uploaded, awaiting verification"),
    VERIFIED("Document verified"),
    REJECTED("Document rejected (requires remarks)");

    private final String description;

    DocumentStatus(String description) {
        this.description = description;
    }
}