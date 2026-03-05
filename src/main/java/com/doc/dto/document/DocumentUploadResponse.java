package com.doc.dto.document;

import lombok.Data;

@Data
public class DocumentUploadResponse {
    private Long id;
    private String uuid;
    private String fileName;
    private String url;
    private String message = "Document uploaded successfully";
}