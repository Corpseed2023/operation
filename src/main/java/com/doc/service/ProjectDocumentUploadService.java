package com.doc.service;

import com.doc.dto.project.DocumentResponseDto;
import com.doc.dto.project.ProjectDocumentUploadRequestDto;
import com.doc.dto.project.ProjectDocumentStatusUpdateDto;

import java.util.UUID;

/**
 * Service interface for managing project document uploads and status updates.
 */
public interface ProjectDocumentUploadService {

    /**
     * Uploads a document for a specific milestone assignment in a project.
     *
     * @param requestDto the request DTO containing upload details
     * @return the response DTO with uploaded document details
     */
    DocumentResponseDto uploadDocument(ProjectDocumentUploadRequestDto requestDto);

    /**
     * Updates the status of an uploaded document.
     *
     * @param documentId the UUID of the document to update
     * @param updateDto  the update DTO containing new status and remarks
     * @return the response DTO with updated document details
     */
    DocumentResponseDto updateDocumentStatus(UUID documentId, ProjectDocumentStatusUpdateDto updateDto);
}