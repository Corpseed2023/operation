package com.doc.entity.chat;

import com.doc.em.chat.OperationChatContextType;
import com.doc.em.chat.OperationChatConversationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "operation_chat_conversations",
        indexes = {
                @Index(name = "idx_chat_context", columnList = "context_type, reference_id"),
                @Index(name = "idx_chat_status", columnList = "status"),
                @Index(name = "idx_chat_last_message_at", columnList = "last_message_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Example:
     * LEGAL_REQUEST
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 50)
    private OperationChatContextType contextType;

    /**
     * Example:
     * legalRequestId
     */
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    /**
     * Example:
     * Legal Review - Vendor Agreement
     */
    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OperationChatConversationStatus status;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "last_message", length = 1000)
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}