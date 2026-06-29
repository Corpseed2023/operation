package com.doc.dto.chat;

import com.doc.em.chat.OperationChatContextType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartOperationChatRequestDto {

    @NotNull(message = "Context type is required")
    private OperationChatContextType contextType;

    @NotNull(message = "Reference id is required")
    private Long referenceId;

    @NotNull(message = "Created by user id is required")
    private Long createdBy;

    @NotNull(message = "Receiver user id is required")
    private Long receiverId;
}