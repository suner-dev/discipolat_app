package com.discipolat.modules.groupMessages.api;

import com.discipolat.modules.groupMessages.domain.GroupMessage;
import com.discipolat.modules.groupMessages.domain.GroupMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping({"/api/group-messages", "/api/v1/group-messages"})
@PreAuthorize("isAuthenticated()")
public class GroupMessageController {

    private final GroupMessageService groupMessageService;

    public GroupMessageController(GroupMessageService groupMessageService) {
        this.groupMessageService = groupMessageService;
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getMessages(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupMessageService.getGroupMessages(groupId));
    }

    @PostMapping
    public ResponseEntity<?> send(@RequestBody GroupMessage message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupMessageService.sendMessage(message));
    }

    @PostMapping("/{id}/reaction")
    public ResponseEntity<?> react(@PathVariable UUID id) {
        return ResponseEntity.ok(groupMessageService.addReaction(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        groupMessageService.deleteMessage(id);
        return ResponseEntity.ok(Map.of("message", "Message supprimé"));
    }

    /** P10 — Recherche plein-texte simple dans les messages d'un groupe. */
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam UUID groupId, @RequestParam String q) {
        return ResponseEntity.ok(groupMessageService.search(groupId, q));
    }

    @GetMapping("/group/{groupId}/stats")
    public ResponseEntity<?> stats(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupMessageService.getGroupStats(groupId));
    }
}
