package com.doc.service;

import com.doc.dto.document.DocumentUploadRequest;
import com.doc.dto.document.DocumentUploadResponse;

public interface DocumentService {
    DocumentUploadResponse uploadDocument(DocumentUploadRequest request);
}