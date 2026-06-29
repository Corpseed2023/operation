package com.doc.dto.chat;

import com.doc.em.chat.OperationChatContextType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OperationChatConversationResponseDto {

    private Long id;

    private OperationChatContextType contextType;

    private Long referenceId;

    private String title;

    private String lastMessage;

    private LocalDateTime lastMessageAt;

    private Long unreadCount;
}