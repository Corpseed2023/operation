package com.doc.dto.chat;

import lombok.Data;

@Data
public class ChatAttachmentRequestDto {

    private String fileUrl;

    private String fileName;

    private String fileType;

    private Long fileSize;
}