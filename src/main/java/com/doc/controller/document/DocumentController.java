package com.doc.controller.document;

import com.doc.dto.document.DocumentUploadRequest;
import com.doc.dto.document.DocumentUploadResponse;
import com.doc.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/operationService/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestPart("file") MultipartFile file) {

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setFile(file);

        DocumentUploadResponse response = documentService.uploadDocument(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}