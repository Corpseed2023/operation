package com.doc.repository.chat;

import com.doc.entity.chat.OperationChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationChatAttachmentRepository extends JpaRepository<OperationChatAttachment, Long> {

    List<OperationChatAttachment> findByMessage_Id(Long messageId);
}