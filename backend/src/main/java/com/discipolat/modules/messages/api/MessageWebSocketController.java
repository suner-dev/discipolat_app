package com.discipolat.modules.messages.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.messages.domain.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket controller for real-time messaging.
 *
 * Endpoints:
 * - POST /app/conversations/{id}/send → broadcasts to /topic/conversations/{id}
 * - POST /app/conversations/{id}/typing → broadcasts to /topic/conversations/{id}/typing
 * - POST /app/conversations/{id}/read → broadcasts to /topic/conversations/{id}/read
 */
@Controller
public class MessageWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    public MessageWebSocketController(SimpMessagingTemplate messagingTemplate, MessageService messageService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
    }

    @MessageMapping("/conversations/{conversationId}/send")
    @SendTo("/topic/conversations/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public MessageResponse handleMessage(
            @DestinationVariable UUID conversationId,
            @Payload SendMessageRequest request,
            Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        MessageResponse response = messageService.sendMessage(conversationId, request);

        // Also send notification to conversation participants
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId + "/notification",
                Map.of(
                        "type", "NEW_MESSAGE",
                        "conversationId", conversationId.toString(),
                        "messageId", response.id().toString(),
                        "senderId", userId.toString(),
                        "timestamp", LocalDateTime.now().toString()
                ));

        return response;
    }

    @MessageMapping("/conversations/{conversationId}/typing")
    public void handleTyping(
            @DestinationVariable UUID conversationId,
            @Payload Map<String, Boolean> payload,
            Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        boolean isTyping = payload.getOrDefault("typing", false);

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId + "/typing",
                Map.of(
                        "userId", userId.toString(),
                        "typing", isTyping,
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @MessageMapping("/conversations/{conversationId}/read")
    public void handleRead(
            @DestinationVariable UUID conversationId,
            @Payload Map<String, String> payload,
            Principal principal) {

        UUID userId = UUID.fromString(principal.getName());

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId + "/read",
                Map.of(
                        "userId", userId.toString(),
                        "lastReadMessageId", payload.getOrDefault("lastReadMessageId", ""),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}
