package com.discipolat.modules.messages.api;

import java.util.UUID;

public record SendEnhancedMessageRequest(
        String content,
        String messageType, // TEXT, VOICE, IMAGE, FILE
        String mediaUrl,
        Integer mediaDuration,
        UUID replyToId
) {}
