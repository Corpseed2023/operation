package com.doc.controller.chat;

import com.doc.dto.chat.SendOperationChatMessageRequestDto;
import com.doc.impl.chat.OperationChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class OperationChatSocketController {

    private final OperationChatService operationChatService;

    @MessageMapping("/operation-chat/{conversationId}/send")
    public void sendMessage(
            @DestinationVariable Long conversationId,
            @Payload SendOperationChatMessageRequestDto requestDto
    ) {
        operationChatService.sendMessage(conversationId, requestDto);


    }
}