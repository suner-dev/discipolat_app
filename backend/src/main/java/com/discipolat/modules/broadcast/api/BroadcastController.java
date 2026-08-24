package com.discipolat.modules.broadcast.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.broadcast.domain.BroadcastMessage;
import com.discipolat.modules.broadcast.domain.BroadcastService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/broadcast")
public class BroadcastController {

    private final BroadcastService service;

    public BroadcastController(BroadcastService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PageResponse<BroadcastMessage>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statut) {
        Page<BroadcastMessage> messages = service.list(PageRequest.of(page, size), statut);
        return ResponseEntity.ok(PageResponse.of(messages.getContent(), page, size,
                messages.getTotalElements(), messages.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BroadcastMessage> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BroadcastMessage> create(@RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        BroadcastMessage message = service.create(body.get("titre"), body.get("contenu"),
                body.get("cible"), body.get("cibleIds"), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @PatchMapping("/{id}/schedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BroadcastMessage> schedule(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.schedule(id, LocalDateTime.parse(body.get("programméAt"))));
    }

    @PatchMapping("/{id}/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BroadcastMessage> send(@PathVariable UUID id) {
        return ResponseEntity.ok(service.send(id));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        service.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/receipts")
    public ResponseEntity<Map<String, Object>> getReceipts(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getReceiptStats(id));
    }
}
