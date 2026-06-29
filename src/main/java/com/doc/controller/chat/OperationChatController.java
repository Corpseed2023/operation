package com.doc.controller.chat;

import com.doc.dto.chat.*;
import com.doc.impl.chat.OperationChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operationService/api/chats")
@RequiredArgsConstructor
public class OperationChatController {

    private final OperationChatService operationChatService;

    @PostMapping("/start")
    public ResponseEntity<OperationChatConversationResponseDto> startChat(
            @RequestBody StartOperationChatRequestDto requestDto
    ) {
        return ResponseEntity.ok(operationChatService.startChat(requestDto));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<OperationChatMessageResponseDto> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendOperationChatMessageRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                operationChatService.sendMessage(conversationId, requestDto)
        );
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<Page<OperationChatMessageResponseDto>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(
                operationChatService.getMessages(conversationId, userId, page, size)
        );
    }

    @PatchMapping("/{conversationId}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable Long conversationId,
            @RequestParam Long userId
    ) {
        operationChatService.markAsRead(conversationId, userId);
        return ResponseEntity.ok("Chat marked as read");
    }
}