package com.doc.repository.chat;

import com.doc.em.chat.OperationChatContextType;
import com.doc.entity.chat.OperationChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperationChatConversationRepository extends JpaRepository<OperationChatConversation, Long> {

    Optional<OperationChatConversation> findByContextTypeAndReferenceIdAndDeletedFalse(
            OperationChatContextType contextType,
            Long referenceId
    );
}