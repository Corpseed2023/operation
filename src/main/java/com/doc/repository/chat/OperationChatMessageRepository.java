package com.doc.repository.chat;

import com.doc.entity.chat.OperationChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OperationChatMessageRepository extends JpaRepository<OperationChatMessage, Long> {

    /**
     * Fetch chat messages by conversation id.
     * Sender is fetched because response DTO needs sender id and sender fullName.
     */
    @Query(
            value = """
                    SELECT m
                    FROM OperationChatMessage m
                    JOIN FETCH m.sender s
                    WHERE m.conversation.id = :conversationId
                      AND m.deleted = false
                    ORDER BY m.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(m)
                    FROM OperationChatMessage m
                    WHERE m.conversation.id = :conversationId
                      AND m.deleted = false
                    """
    )
    Page<OperationChatMessage> findMessagesByConversationId(
            @Param("conversationId") Long conversationId,
            Pageable pageable
    );

    /**
     * Unread count when user has never read the chat.
     * Count all messages except messages sent by current user.
     */
    @Query("""
            SELECT COUNT(m)
            FROM OperationChatMessage m
            WHERE m.conversation.id = :conversationId
              AND m.deleted = false
              AND m.sender.id <> :userId
            """)
    long countUnreadMessagesWhenNeverRead(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId
    );

    /**
     * Unread count when lastReadAt exists.
     * Count messages after lastReadAt except current user's own messages.
     */
    @Query("""
            SELECT COUNT(m)
            FROM OperationChatMessage m
            WHERE m.conversation.id = :conversationId
              AND m.deleted = false
              AND m.sender.id <> :userId
              AND m.createdAt > :lastReadAt
            """)
    long countUnreadMessagesAfterLastRead(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("lastReadAt") LocalDateTime lastReadAt
    );

    /**
     * Optional: fetch latest message of a conversation.
     * Useful for chat list screen if needed.
     */
    @Query("""
            SELECT m
            FROM OperationChatMessage m
            JOIN FETCH m.sender s
            WHERE m.conversation.id = :conversationId
              AND m.deleted = false
            ORDER BY m.createdAt DESC
            LIMIT 1
            """)
    OperationChatMessage findLatestMessageByConversationId(
            @Param("conversationId") Long conversationId
    );
}