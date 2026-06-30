package com.doc.impl.chat;

import com.doc.dto.chat.ChatAttachmentRequestDto;
import com.doc.dto.chat.ChatAttachmentResponseDto;
import com.doc.dto.chat.OperationChatConversationResponseDto;
import com.doc.dto.chat.OperationChatMessageResponseDto;
import com.doc.dto.chat.SendOperationChatMessageRequestDto;
import com.doc.dto.chat.StartOperationChatRequestDto;
import com.doc.em.chat.OperationChatConversationStatus;
import com.doc.em.chat.OperationChatMessageType;
import com.doc.entity.chat.OperationChatAttachment;
import com.doc.entity.chat.OperationChatConversation;
import com.doc.entity.chat.OperationChatMessage;
import com.doc.entity.chat.OperationChatParticipant;
import com.doc.entity.user.User;
import com.doc.repository.UserRepository;
import com.doc.repository.chat.OperationChatAttachmentRepository;
import com.doc.repository.chat.OperationChatConversationRepository;
import com.doc.repository.chat.OperationChatMessageRepository;
import com.doc.repository.chat.OperationChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationChatService {

    private final OperationChatConversationRepository conversationRepository;
    private final OperationChatParticipantRepository participantRepository;
    private final OperationChatMessageRepository messageRepository;
    private final OperationChatAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public OperationChatConversationResponseDto startChat(StartOperationChatRequestDto requestDto) {

        validateStartChatRequest(requestDto);

        User createdByUser = userRepository.findActiveUserById(requestDto.getCreatedBy())
                .orElseThrow(() -> new RuntimeException("Created by user not found or inactive"));

        User receiverUser = userRepository.findActiveUserById(requestDto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver user not found or inactive"));

        if (createdByUser.getId().equals(receiverUser.getId())) {
            throw new RuntimeException("Sender and receiver cannot be same user");
        }

        OperationChatConversation conversation = conversationRepository
                .findByContextTypeAndReferenceIdAndDeletedFalse(
                        requestDto.getContextType(),
                        requestDto.getReferenceId()
                )
                .orElseGet(() -> createConversation(requestDto));

        addParticipantIfMissing(conversation, createdByUser);
        addParticipantIfMissing(conversation, receiverUser);

        return toConversationDto(conversation, createdByUser.getId());
    }

    private void validateStartChatRequest(StartOperationChatRequestDto requestDto) {

        if (requestDto == null) {
            throw new RuntimeException("Request body is required");
        }

        if (requestDto.getContextType() == null) {
            throw new RuntimeException("Context type is required");
        }

        if (requestDto.getReferenceId() == null) {
            throw new RuntimeException("Reference id is required");
        }

        if (requestDto.getCreatedBy() == null) {
            throw new RuntimeException("Created by user id is required");
        }

        if (requestDto.getReceiverId() == null) {
            throw new RuntimeException("Receiver user id is required");
        }
    }

    private OperationChatConversation createConversation(StartOperationChatRequestDto requestDto) {

        OperationChatConversation conversation = OperationChatConversation.builder()
                .contextType(requestDto.getContextType())
                .referenceId(requestDto.getReferenceId())
                .title(requestDto.getContextType().name() + "-" + requestDto.getReferenceId())
                .status(OperationChatConversationStatus.OPEN)
                .createdBy(requestDto.getCreatedBy())
                .lastMessage("Chat started")
                .lastMessageAt(LocalDateTime.now())
                .deleted(false)
                .build();

        return conversationRepository.save(conversation);
    }

    private void addParticipantIfMissing(OperationChatConversation conversation, User user) {

        boolean exists = participantRepository
                .existsByConversation_IdAndUser_IdAndActiveTrue(
                        conversation.getId(),
                        user.getId()
                );

        if (exists) {
            return;
        }

        OperationChatParticipant participant = OperationChatParticipant.builder()
                .conversation(conversation)
                .user(user)
                .active(true)
                .build();

        participantRepository.save(participant);
    }

    @Transactional
    public OperationChatMessageResponseDto sendMessage(
            Long conversationId,
            SendOperationChatMessageRequestDto requestDto
    ) {

        if (conversationId == null) {
            throw new RuntimeException("Conversation id is required");
        }

        if (requestDto == null) {
            throw new RuntimeException("Request body is required");
        }

        if (requestDto.getSenderId() == null) {
            throw new RuntimeException("Sender id is required");
        }

        // Fetch conversation
        OperationChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Chat conversation not found"));

        // Validation checks
        if (conversation.isDeleted()) {
            throw new RuntimeException("Chat conversation is deleted");
        }

        if (conversation.getStatus() == OperationChatConversationStatus.CLOSED) {
            throw new RuntimeException("Chat conversation is closed. Please reopen the chat to send messages.");
        }

        User sender = userRepository.findActiveUserById(requestDto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender user not found or inactive"));

        // Validate that sender is a participant
        validateParticipant(conversationId, sender.getId());

        // Determine message type (text, attachment, or both)
        OperationChatMessageType finalMessageType = resolveMessageType(requestDto);

        // Validate content based on message type
        validateMessageContent(requestDto, finalMessageType);

        // Build and save message
        OperationChatMessage message = OperationChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .messageType(finalMessageType)
                .message(requestDto.getMessage())
                .replyToMessageId(requestDto.getReplyToMessageId())
                .edited(false)
                .deleted(false)
                .build();

        OperationChatMessage savedMessage = messageRepository.save(message);

        // Save attachments if any
        saveAttachments(savedMessage, requestDto.getAttachments());

        // Update conversation's last message info
        conversation.setLastMessage(buildLastMessage(requestDto, finalMessageType));
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Convert to response DTO
        OperationChatMessageResponseDto responseDto = toMessageDto(savedMessage);

        // Real-time broadcast to all participants
        messagingTemplate.convertAndSend(
                "/topic/operation-chat/conversation/" + conversationId,
                responseDto
        );

        return responseDto;
    }


    private OperationChatMessageType resolveMessageType(SendOperationChatMessageRequestDto requestDto) {

        boolean hasText = requestDto.getMessage() != null
                && !requestDto.getMessage().trim().isEmpty();

        boolean hasAttachment = requestDto.getAttachments() != null
                && !requestDto.getAttachments().isEmpty();

        if (hasText && hasAttachment) {
            return OperationChatMessageType.TEXT_WITH_ATTACHMENT;
        }

        if (hasAttachment) {
            return OperationChatMessageType.ATTACHMENT;
        }

        return OperationChatMessageType.TEXT;
    }

    private void validateMessageContent(
            SendOperationChatMessageRequestDto requestDto,
            OperationChatMessageType messageType
    ) {

        if (messageType == OperationChatMessageType.TEXT) {
            if (requestDto.getMessage() == null || requestDto.getMessage().trim().isEmpty()) {
                throw new RuntimeException("Message or attachment is required");
            }
        }

        if (messageType == OperationChatMessageType.ATTACHMENT
                || messageType == OperationChatMessageType.TEXT_WITH_ATTACHMENT) {

            boolean validAttachmentExists = requestDto.getAttachments()
                    .stream()
                    .anyMatch(attachment ->
                            attachment.getFileUrl() != null
                                    && !attachment.getFileUrl().trim().isEmpty()
                    );

            if (!validAttachmentExists) {
                throw new RuntimeException("Valid attachment file url is required");
            }
        }
    }

    private void saveAttachments(
            OperationChatMessage message,
            List<ChatAttachmentRequestDto> attachments
    ) {

        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        for (ChatAttachmentRequestDto attachmentDto : attachments) {

            if (attachmentDto.getFileUrl() == null
                    || attachmentDto.getFileUrl().trim().isEmpty()) {
                continue;
            }

            OperationChatAttachment attachment = OperationChatAttachment.builder()
                    .message(message)
                    .fileUrl(attachmentDto.getFileUrl())
                    .fileName(attachmentDto.getFileName())
                    .fileType(attachmentDto.getFileType())
                    .fileSize(attachmentDto.getFileSize())
                    .build();

            attachmentRepository.save(attachment);
        }
    }

    @Transactional(readOnly = true)
    public Page<OperationChatMessageResponseDto> getMessages(
            Long conversationId,
            Long userId,
            int page,
            int size
    ) {

        validateParticipant(conversationId, userId);

        int validPage = Math.max(page, 0);
        int validSize = size <= 0 ? 30 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                validPage,
                validSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return messageRepository
                .findMessagesByConversationId(
                        conversationId,
                        pageable
                )
                .map(this::toMessageDto);
    }

    @Transactional
    public void markAsRead(Long conversationId, Long userId) {

        if (conversationId == null) {
            throw new RuntimeException("Conversation id is required");
        }

        if (userId == null) {
            throw new RuntimeException("User id is required");
        }

        OperationChatParticipant participant = participantRepository
                .findByConversation_IdAndUser_IdAndActiveTrue(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Chat participant not found"));

        participant.setLastReadAt(LocalDateTime.now());
        participantRepository.save(participant);
    }

    private void validateParticipant(Long conversationId, Long userId) {

        if (conversationId == null) {
            throw new RuntimeException("Conversation id is required");
        }

        if (userId == null) {
            throw new RuntimeException("User id is required");
        }

        boolean exists = participantRepository
                .existsByConversation_IdAndUser_IdAndActiveTrue(
                        conversationId,
                        userId
                );

        if (!exists) {
            throw new RuntimeException("User is not allowed to access this chat");
        }
    }

    private String buildLastMessage(
            SendOperationChatMessageRequestDto requestDto,
            OperationChatMessageType messageType
    ) {

        if (messageType == OperationChatMessageType.ATTACHMENT) {
            return "Attachment";
        }

        if (messageType == OperationChatMessageType.TEXT_WITH_ATTACHMENT) {
            String message = requestDto.getMessage();

            if (message == null || message.trim().isEmpty()) {
                return "Attachment";
            }

            return message.trim() + " + Attachment";
        }

        return requestDto.getMessage().trim();
    }

    private OperationChatConversationResponseDto toConversationDto(
            OperationChatConversation conversation,
            Long userId
    ) {

        OperationChatParticipant participant = participantRepository
                .findByConversation_IdAndUser_IdAndActiveTrue(
                        conversation.getId(),
                        userId
                )
                .orElse(null);

        long unreadCount = 0;

        if (participant != null) {
            if (participant.getLastReadAt() == null) {
                unreadCount = messageRepository.countUnreadMessagesWhenNeverRead(
                        conversation.getId(),
                        userId
                );
            } else {
                unreadCount = messageRepository.countUnreadMessagesAfterLastRead(
                        conversation.getId(),
                        userId,
                        participant.getLastReadAt()
                );
            }
        }

        return OperationChatConversationResponseDto.builder()
                .id(conversation.getId())
                .contextType(conversation.getContextType())
                .referenceId(conversation.getReferenceId())
                .title(conversation.getTitle())
                .lastMessage(conversation.getLastMessage())
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }

    private OperationChatMessageResponseDto toMessageDto(OperationChatMessage message) {

        List<ChatAttachmentResponseDto> attachments =
                attachmentRepository.findByMessage_Id(message.getId())
                        .stream()
                        .map(attachment -> ChatAttachmentResponseDto.builder()
                                .id(attachment.getId())
                                .fileUrl(attachment.getFileUrl())
                                .fileName(attachment.getFileName())
                                .fileType(attachment.getFileType())
                                .fileSize(attachment.getFileSize())
                                .build())
                        .toList();

        User sender = message.getSender();

        return OperationChatMessageResponseDto.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(sender.getId())
                .senderName(sender.getFullName())
                .messageType(message.getMessageType())
                .message(message.getMessage())
                .replyToMessageId(message.getReplyToMessageId())
                .attachments(attachments)
                .createdAt(message.getCreatedAt())
                .build();
    }

    @Transactional
    public OperationChatConversationResponseDto closeChat(Long conversationId, Long userId) {
        validateParticipant(conversationId, userId);

        OperationChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Chat conversation not found"));

        if (conversation.isDeleted()) {
            throw new RuntimeException("Chat conversation is deleted");
        }

        if (conversation.getStatus() == OperationChatConversationStatus.CLOSED) {
            throw new RuntimeException("Chat is already closed");
        }

        conversation.setStatus(OperationChatConversationStatus.CLOSED);
        conversation.setLastMessage("Chat closed");
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Optional: Notify participants
        messagingTemplate.convertAndSend(
                "/topic/operation-chat/conversation/" + conversationId,
                "Chat has been closed"
        );

        return toConversationDto(conversation, userId);
    }
    @Transactional
    public OperationChatConversationResponseDto reopenChat(Long conversationId, Long userId) {
        validateParticipant(conversationId, userId);

        OperationChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Chat conversation not found"));

        if (conversation.isDeleted()) {
            throw new RuntimeException("Chat conversation is deleted");
        }

        if (conversation.getStatus() == OperationChatConversationStatus.OPEN) {
            throw new RuntimeException("Chat is already open");
        }

        conversation.setStatus(OperationChatConversationStatus.OPEN);
        conversation.setLastMessage("Chat reopened");
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Optional: Notify participants
        messagingTemplate.convertAndSend(
                "/topic/operation-chat/conversation/" + conversationId,
                "Chat has been reopened"
        );

        return toConversationDto(conversation, userId);
    }



}