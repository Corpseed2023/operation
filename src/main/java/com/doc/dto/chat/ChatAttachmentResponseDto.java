package com.doc.dto.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatAttachmentResponseDto {

    private Long id;

    private String fileUrl;

    private String fileName;

    private String fileType;

    private Long fileSize;
}