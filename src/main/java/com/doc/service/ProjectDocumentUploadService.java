package com.doc.service;

import com.doc.dto.project.DocumentResponseDto;
import com.doc.dto.project.ProjectDocumentUploadRequestDto;
import com.doc.dto.project.ProjectDocumentStatusUpdateDto;


/**
 * Service interface for managing project document uploads and status updates.
 */
public interface ProjectDocumentUploadService {


    DocumentResponseDto uploadDocument(ProjectDocumentUploadRequestDto requestDto);

    DocumentResponseDto updateDocumentStatus(Long documentId, ProjectDocumentStatusUpdateDto updateDto);

}