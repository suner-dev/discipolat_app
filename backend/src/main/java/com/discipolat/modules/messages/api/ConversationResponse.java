package com.discipolat.modules.messages.api;

import com.discipolat.modules.messages.domain.Conversation;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID otherUserId,
        String otherUserName,
        String otherUserRole,
        String lastMessage,
        UUID lastMessageSenderId,
        LocalDateTime lastMessageAt,
        long unreadCount,
        LocalDateTime createdAt
) {
    public static ConversationResponse from(Conversation c, UUID currentUserId,
                                            String otherUserName, String otherUserRole,
                                            String lastMessage, UUID lastMessageSenderId,
                                            long unreadCount) {
        return new ConversationResponse(
                c.getId(),
                c.getUserAId().equals(currentUserId) ? c.getUserBId() : c.getUserAId(),
                otherUserName,
                otherUserRole,
                lastMessage,
                lastMessageSenderId,
                c.getLastMessageAt(),
                unreadCount,
                c.getCreatedAt()
        );
    }
}
