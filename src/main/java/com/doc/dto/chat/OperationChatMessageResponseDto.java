package com.doc.dto.chat;

import com.doc.em.chat.OperationChatMessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OperationChatMessageResponseDto {

    private Long id;

    private Long conversationId;

    private Long senderId;

    private String senderName;

    private OperationChatMessageType messageType;

    private String message;

    private Long replyToMessageId;

    private List<ChatAttachmentResponseDto> attachments;

    private LocalDateTime createdAt;
}