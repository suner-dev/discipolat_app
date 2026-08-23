package com.discipolat.modules.broadcast.api;

import com.discipolat.modules.broadcast.domain.BroadcastMessage;
import com.discipolat.modules.broadcast.domain.BroadcastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/broadcasts")
public class BroadcastController {

    private final BroadcastService service;

    public BroadcastController(BroadcastService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<BroadcastMessage>> list(@RequestParam Long tenantId) {
        return ResponseEntity.ok(service.list(tenantId));
    }

    @PostMapping
    public ResponseEntity<BroadcastMessage> send(@RequestBody BroadcastMessage message) {
        return ResponseEntity.ok(service.send(message));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<BroadcastMessage> markRead(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int totalRecipients) {
        return ResponseEntity.ok(service.markRead(id, totalRecipients));
    }
}
