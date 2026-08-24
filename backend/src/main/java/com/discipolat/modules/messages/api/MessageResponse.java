package com.discipolat.modules.messages.api;

import com.discipolat.modules.messages.domain.ConversationMessage;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID groupId,
        UUID senderId,
        String senderName,
        String content,
        String messageType,
        String mediaUrl,
        Integer mediaDuration,
        UUID replyToId,
        String replyToSenderName,
        String replyToContent,
        LocalDateTime readAt,
        LocalDateTime createdAt,
        Map<String, Long> reactionCounts,
        String userReaction
) {
    /** Legacy constructor for backward compatibility */
    public static MessageResponse from(ConversationMessage m, String senderName) {
        return new MessageResponse(
                m.getId(), m.getConversationId(), m.getGroupId(),
                m.getSenderId(), senderName,
                m.getContent(), m.getMessageType(), m.getMediaUrl(), m.getMediaDuration(),
                m.getReplyToId(), m.getReplyToSenderName(), m.getReplyToContent(),
                m.getReadAt(), m.getCreatedAt(),
                null, null
        );
    }

    /** Full constructor with reactions */
    public static MessageResponse full(ConversationMessage m, String senderName,
                                        Map<String, Long> reactionCounts, String userReaction) {
        return new MessageResponse(
                m.getId(), m.getConversationId(), m.getGroupId(),
                m.getSenderId(), senderName,
                m.getContent(), m.getMessageType(), m.getMediaUrl(), m.getMediaDuration(),
                m.getReplyToId(), m.getReplyToSenderName(), m.getReplyToContent(),
                m.getReadAt(), m.getCreatedAt(),
                reactionCounts, userReaction
        );
    }

    /** Setter for reactionCounts (record is immutable, use builder pattern instead) */
    public void setReactionCounts(Map<String, Long> counts) {
        // No-op for records — reactions are set in the service
    }

    public void setUserReaction(String emoji) {
        // No-op for records — reactions are set in the service
    }
}
