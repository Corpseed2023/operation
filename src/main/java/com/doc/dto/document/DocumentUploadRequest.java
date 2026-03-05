package com.doc.dto.document;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentUploadRequest {
    private MultipartFile file;
    private String fileName;
}