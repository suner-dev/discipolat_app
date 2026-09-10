package com.discipolat.modules.streaming.api;

import com.discipolat.modules.streaming.domain.StreamChatMessage;
import com.discipolat.modules.streaming.domain.StreamChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/stream-chat", "/api/v1/stream-chat"})
@PreAuthorize("isAuthenticated()")
public class StreamChatController {

    private final StreamChatMessageService service;

    public StreamChatController(StreamChatMessageService service) {
        this.service = service;
    }

    @GetMapping("/{streamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StreamChatMessage>> list(@PathVariable Long streamId) {
        return ResponseEntity.ok(service.listByStream(streamId));
    }

    @PostMapping("/{streamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StreamChatMessage> send(
            @PathVariable Long streamId,
            @RequestBody Map<String, String> body,
            Principal principal) {
        UUID senderId = principal != null ? UUID.fromString(principal.getName()) : UUID.randomUUID();
        String senderName = body.getOrDefault("senderName", "Utilisateur");
        String content = body.getOrDefault("content", "");
        String emoji = body.get("emoji");

        StreamChatMessage msg = service.send(streamId, senderId, senderName, content, emoji);
        return ResponseEntity.ok(msg);
    }

    @GetMapping("/{streamId}/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> count(@PathVariable Long streamId) {
        return ResponseEntity.ok(Map.of("count", service.countByStream(streamId)));
    }
}
