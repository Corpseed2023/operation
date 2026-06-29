package com.doc.repository.chat;

import com.doc.entity.chat.OperationChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperationChatParticipantRepository extends JpaRepository<OperationChatParticipant, Long> {

    /**
     * Check whether user is already active participant in conversation.
     */
    boolean existsByConversation_IdAndUser_IdAndActiveTrue(Long conversationId, Long userId);

    /**
     * Find active participant by conversation id and user id.
     * Used in markAsRead and validation.
     */
    Optional<OperationChatParticipant> findByConversation_IdAndUser_IdAndActiveTrue(
            Long conversationId,
            Long userId
    );

    /**
     * Find all active participants of a conversation.
     * Used when you want to show users in chat.
     */
    List<OperationChatParticipant> findByConversation_IdAndActiveTrue(Long conversationId);

    /**
     * Find all active participants of a conversation ordered by joined time.
     */
    List<OperationChatParticipant> findByConversation_IdAndActiveTrueOrderByJoinedAtAsc(
            Long conversationId
    );

    /**
     * Find all active conversations of a user.
     * Useful for chat list screen.
     */
    List<OperationChatParticipant> findByUser_IdAndActiveTrueOrderByConversation_LastMessageAtDesc(
            Long userId
    );

    /**
     * Find participant even if inactive.
     * Useful if later you want to reactivate participant.
     */
    Optional<OperationChatParticipant> findByConversation_IdAndUser_Id(
            Long conversationId,
            Long userId
    );
}