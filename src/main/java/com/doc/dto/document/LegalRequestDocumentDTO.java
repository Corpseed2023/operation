package com.doc.dto.document;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

public class LegalRequestDocumentDTO {
    private Long id;
    private String LegalRequest;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private long fileSize;

    private String uuid;

    private LocalDateTime uploadedAt;
}
