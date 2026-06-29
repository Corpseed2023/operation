package com.doc.entity.chat;

import com.doc.em.chat.OperationChatMessageType;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "operation_chat_messages",
        indexes = {
                @Index(name = "idx_chat_msg_conversation", columnList = "conversation_id"),
                @Index(name = "idx_chat_msg_sender", columnList = "sender_id"),
                @Index(name = "idx_chat_msg_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Chat conversation where this message belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private OperationChatConversation conversation;

    /**
     * Existing ERP user who sent the message.
     *
     * Do not store senderName separately.
     * Sender name will come from User.fullName.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * TEXT, ATTACHMENT, TEXT_WITH_ATTACHMENT
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 50)
    private OperationChatMessageType messageType;

    /**
     * Text message body.
     * Can be null when message type is ATTACHMENT only.
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Optional reply message id.
     * For now only stores parent message id.
     */
    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    @Column(name = "is_edited", nullable = false)
    private boolean edited = false;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}