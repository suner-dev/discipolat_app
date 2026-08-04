package com.discipolat.modules.messages.api;

import com.discipolat.modules.messages.domain.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> conversations() {
        return ResponseEntity.ok(messageService.listConversations());
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> startConversation(
            @Valid @RequestBody StartConversationRequest request) {
        return ResponseEntity.ok(messageService.startConversation(request.otherUserId()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageResponse>> messages(@PathVariable UUID id) {
        return ResponseEntity.ok(messageService.getMessages(id));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(messageService.sendMessage(id, request));
    }

    @PatchMapping("/conversations/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        messageService.markConversationAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conversations/unread-total")
    public ResponseEntity<Map<String, Long>> unreadTotal() {
        long total = messageService.listConversations().stream()
                .mapToLong(ConversationResponse::unreadCount)
                .sum();
        return ResponseEntity.ok(Map.of("total", total));
    }
}
