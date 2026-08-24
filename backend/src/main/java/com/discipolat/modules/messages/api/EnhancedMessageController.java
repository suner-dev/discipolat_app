package com.discipolat.modules.messages.api;

import com.discipolat.modules.messages.domain.EnhancedMessageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@PreAuthorize("isAuthenticated()")
public class EnhancedMessageController {

    private final EnhancedMessageService enhancedMessageService;

    public EnhancedMessageController(EnhancedMessageService enhancedMessageService) {
        this.enhancedMessageService = enhancedMessageService;
    }

    // ============================================================
    // GROUP CONVERSATIONS
    // ============================================================

    @PostMapping("/groups")
    public ResponseEntity<GroupConversationResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.ok(enhancedMessageService.createGroup(
                request.name(), request.description(), request.groupType(), request.memberIds()));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupConversationResponse>> listMyGroups() {
        return ResponseEntity.ok(enhancedMessageService.listMyGroups());
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<GroupConversationResponse> getGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(enhancedMessageService.getGroup(groupId));
    }

    @PostMapping("/groups/{groupId}/members")
    public ResponseEntity<Void> addMembers(
            @PathVariable UUID groupId,
            @RequestBody Map<String, List<UUID>> body) {
        enhancedMessageService.addMembers(groupId, body.get("userIds"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID userId) {
        enhancedMessageService.removeMember(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/groups/{groupId}/messages")
    public ResponseEntity<List<MessageResponse>> getGroupMessages(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(enhancedMessageService.getGroupMessages(
                groupId, PageRequest.of(page, size)));
    }

    @PostMapping("/groups/{groupId}/messages")
    public ResponseEntity<MessageResponse> sendGroupMessage(
            @PathVariable UUID groupId,
            @Valid @RequestBody SendEnhancedMessageRequest request) {
        return ResponseEntity.ok(enhancedMessageService.sendGroupMessage(
                groupId, request.content(), request.messageType(),
                request.mediaUrl(), request.replyToId()));
    }

    @PostMapping("/groups/{groupId}/voice")
    public ResponseEntity<MessageResponse> sendGroupVoiceMessage(
            @PathVariable UUID groupId,
            @RequestBody Map<String, Object> body) {
        String audioUrl = (String) body.get("audioUrl");
        Integer duration = body.get("duration") != null ? ((Number) body.get("duration")).intValue() : null;
        return ResponseEntity.ok(enhancedMessageService.sendGroupVoiceMessage(groupId, audioUrl, duration));
    }

    // ============================================================
    // VOICE MESSAGES (1:1)
    // ============================================================

    @PostMapping("/conversations/{id}/voice")
    public ResponseEntity<MessageResponse> sendVoiceMessage(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        String audioUrl = (String) body.get("audioUrl");
        Integer duration = body.get("duration") != null ? ((Number) body.get("duration")).intValue() : null;
        return ResponseEntity.ok(enhancedMessageService.sendVoiceMessage(id, audioUrl, duration));
    }

    // ============================================================
    // REACTIONS
    // ============================================================

    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<Map<String, Object>> toggleReaction(
            @PathVariable UUID messageId,
            @Valid @RequestBody ToggleReactionRequest request) {
        return ResponseEntity.ok(enhancedMessageService.toggleReaction(messageId, request.emoji()));
    }

    @GetMapping("/messages/{messageId}/reactions")
    public ResponseEntity<Map<String, Object>> getReactions(@PathVariable UUID messageId) {
        return ResponseEntity.ok(enhancedMessageService.getReactions(messageId));
    }

    // ============================================================
    // THREADS (REPLIES)
    // ============================================================

    @GetMapping("/messages/{messageId}/replies")
    public ResponseEntity<List<MessageResponse>> getReplies(@PathVariable UUID messageId) {
        return ResponseEntity.ok(enhancedMessageService.getReplies(messageId));
    }

    @PostMapping("/conversations/{id}/reply")
    public ResponseEntity<MessageResponse> sendReply(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UUID replyToId = UUID.fromString(body.get("replyToId"));
        String content = body.get("content");
        return ResponseEntity.ok(enhancedMessageService.sendReply(id, replyToId, content));
    }

    // ============================================================
    // ENHANCED 1:1 MESSAGES
    // ============================================================

    @GetMapping("/conversations/{id}/messages/enhanced")
    public ResponseEntity<List<MessageResponse>> getEnhancedMessages(@PathVariable UUID id) {
        return ResponseEntity.ok(enhancedMessageService.getEnhancedMessages(id));
    }

    @PostMapping("/conversations/{id}/messages/enhanced")
    public ResponseEntity<MessageResponse> sendEnhancedMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendEnhancedMessageRequest request) {
        return ResponseEntity.ok(enhancedMessageService.sendEnhancedMessage(
                id, request.content(), request.messageType(),
                request.mediaUrl(), request.replyToId()));
    }

    // ============================================================
    // SEARCH
    // ============================================================

    @GetMapping("/search")
    public ResponseEntity<Page<MessageResponse>> searchMessages(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(enhancedMessageService.searchMessages(q, PageRequest.of(page, size)));
    }
}
