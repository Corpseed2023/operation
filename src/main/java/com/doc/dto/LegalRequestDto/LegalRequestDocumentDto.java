package com.doc.dto.LegalRequestDto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LegalRequestDocumentDto {

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Long fileSize;

    private String uuid;

    private LocalDateTime uploadedAt;
}