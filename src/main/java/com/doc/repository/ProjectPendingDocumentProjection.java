package com.doc.repository;

public interface ProjectPendingDocumentProjection {
    Long getProjectId();

    Long getTotalRequiredDocuments();

    Long getUploadedDocuments();

    Long getPendingDocuments();
}
