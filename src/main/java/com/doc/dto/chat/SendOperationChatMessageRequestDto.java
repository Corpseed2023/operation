package com.doc.dto.chat;

import com.doc.em.chat.OperationChatMessageType;
import lombok.Data;

import java.util.List;

@Data
public class SendOperationChatMessageRequestDto {

    private Long senderId;

    private String senderName;

    private OperationChatMessageType messageType;

    private String message;

    private Long replyToMessageId;

    private List<ChatAttachmentRequestDto> attachments;
}