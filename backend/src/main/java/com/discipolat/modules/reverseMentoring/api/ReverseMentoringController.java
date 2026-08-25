package com.discipolat.modules.reverseMentoring.api;

import com.discipolat.modules.reverseMentoring.domain.ReverseMentoringRequest;
import com.discipolat.modules.reverseMentoring.domain.ReverseMentoringService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/reverse-mentoring")
@PreAuthorize("isAuthenticated()")
public class ReverseMentoringController {

    private final ReverseMentoringService service;
    public ReverseMentoringController(ReverseMentoringService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ReverseMentoringRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public ResponseEntity<?> list() { return ResponseEntity.ok(service.listAll()); }

    @GetMapping("/pending")
    public ResponseEntity<?> pending() { return ResponseEntity.ok(service.listPending()); }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.accept(id, UUID.fromString(body.get("mentorId"))));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.resolve(id, body.get("outcome")));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() { return ResponseEntity.ok(service.getStats()); }
}
