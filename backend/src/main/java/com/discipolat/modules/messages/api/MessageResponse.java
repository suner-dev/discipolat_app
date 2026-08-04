package com.discipolat.modules.messages.api;

import com.discipolat.modules.messages.domain.ConversationMessage;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String content,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static MessageResponse from(ConversationMessage m, String senderName) {
        return new MessageResponse(
                m.getId(), m.getConversationId(), m.getSenderId(), senderName,
                m.getContent(), m.getReadAt(), m.getCreatedAt()
        );
    }
}
