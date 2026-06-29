package com.doc.entity.chat;

import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "operation_chat_participants",
        indexes = {
                @Index(name = "idx_chat_participant_conversation", columnList = "conversation_id"),
                @Index(name = "idx_chat_participant_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_conversation_user",
                        columnNames = {"conversation_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Chat Conversation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private OperationChatConversation conversation;

    /**
     * Existing ERP User
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Last time this user opened/read this chat
     */
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    /**
     * Active participant
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Joined chat time
     */
    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
}