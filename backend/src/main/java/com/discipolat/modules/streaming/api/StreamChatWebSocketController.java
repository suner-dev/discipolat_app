package com.discipolat.modules.streaming.api;

import com.discipolat.modules.streaming.domain.StreamChatMessage;
import com.discipolat.modules.streaming.domain.StreamChatMessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket controller for real-time streaming chat.
 *
 * Endpoints:
 * - SEND /app/streams/{streamId}/chat → broadcasts to /topic/streams/{streamId}/chat
 * - SEND /app/streams/{streamId}/join → broadcasts to /topic/streams/{streamId}/presence
 * - SEND /app/streams/{streamId}/leave → broadcasts to /topic/streams/{streamId}/presence
 */
@Controller
@PreAuthorize("isAuthenticated()")
public class StreamChatWebSocketController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final StreamChatMessageService chatService;

    public StreamChatWebSocketController(SimpMessageSendingOperations messagingTemplate,
                                        StreamChatMessageService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    @MessageMapping("/streams/{streamId}/chat")
    @SendTo("/topic/streams/{streamId}/chat")
    public StreamChatMessage sendChat(
            @DestinationVariable Long streamId,
            @Payload Map<String, String> payload,
            Principal principal) {

        UUID senderId = principal != null ? UUID.fromString(principal.getName()) : UUID.randomUUID();
        String senderName = payload.getOrDefault("senderName", "Utilisateur");
        String content = payload.getOrDefault("content", "");
        String emoji = payload.get("emoji");

        return chatService.send(streamId, senderId, senderName, content, emoji);
    }

    @MessageMapping("/streams/{streamId}/join")
    public void handleJoin(
            @DestinationVariable Long streamId,
            @Payload Map<String, String> payload,
            Principal principal) {

        UUID userId = principal != null ? UUID.fromString(principal.getName()) : UUID.randomUUID();
        String displayName = payload.getOrDefault("senderName", "Utilisateur");

        messagingTemplate.convertAndSend(
                "/topic/streams/" + streamId + "/presence",
                Map.of(
                        "type", "JOIN",
                        "userId", userId.toString(),
                        "displayName", displayName,
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @MessageMapping("/streams/{streamId}/leave")
    public void handleLeave(
            @DestinationVariable Long streamId,
            Principal principal) {

        UUID userId = principal != null ? UUID.fromString(principal.getName()) : UUID.randomUUID();

        messagingTemplate.convertAndSend(
                "/topic/streams/" + streamId + "/presence",
                Map.of(
                        "type", "LEAVE",
                        "userId", userId.toString(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}