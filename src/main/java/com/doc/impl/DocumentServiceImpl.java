package com.doc.impl;

import com.doc.config.S3Service;
import com.doc.dto.document.DocumentUploadRequest;
import com.doc.dto.document.DocumentUploadResponse;
import com.doc.entity.document.Document;
import com.doc.repository.DocumentRepository;
import com.doc.service.DocumentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final S3Service s3Service;

    public DocumentServiceImpl(DocumentRepository documentRepository, S3Service s3Service) {
        this.documentRepository = documentRepository;
        this.s3Service = s3Service;
    }

    @Override
    public DocumentUploadResponse uploadDocument(DocumentUploadRequest request) {
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        try {
            String s3Key = s3Service.uploadFile(file);
            String fileUrl = s3Service.getFullUrl(s3Key);

            String finalFileName = request.getFileName() != null && !request.getFileName().isBlank()
                    ? request.getFileName()
                    : file.getOriginalFilename();

            String uuid = UUID.randomUUID().toString();

            Document document = new Document();
            document.setFileName(finalFileName);
            document.setUuid(uuid);
            document.setFileUrl(fileUrl);

            document = documentRepository.save(document);

            // Build response
            DocumentUploadResponse response = new DocumentUploadResponse();
            response.setId(document.getId());
            response.setUuid(document.getUuid());
            response.setFileName(document.getFileName());
            response.setUrl(document.getFileUrl());

            return response;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
}