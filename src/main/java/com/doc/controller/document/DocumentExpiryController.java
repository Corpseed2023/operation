package com.doc.controller.document;

import com.doc.dto.document.DocumentExpiryResponseDto;
import com.doc.impl.DocumentExpiryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/operationService/api/document-expiry")
public class DocumentExpiryController {

    private final DocumentExpiryService documentExpiryService;

    public DocumentExpiryController(DocumentExpiryService documentExpiryService) {
        this.documentExpiryService = documentExpiryService;
    }

    /**
     * Upload document/image/pdf/doc/docx and check expiry automatically.
     *
     * API:
     * POST /operationService/api/document-expiry/check
     */
    @PostMapping("/check")
    public ResponseEntity<DocumentExpiryResponseDto> checkDocumentExpiry(
            @RequestParam("file") MultipartFile file
    ) {
        DocumentExpiryResponseDto response = documentExpiryService.checkExpiry(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-url")
    public ResponseEntity<DocumentExpiryResponseDto> checkDocumentExpiryFromUrl(
            @RequestParam("fileUrl") String fileUrl
    ) {
        DocumentExpiryResponseDto response = documentExpiryService.checkExpiryFromUrl(fileUrl);
        return ResponseEntity.ok(response);
    }
}