package com.discipolat.modules.aiVisitNotes.api;

import com.discipolat.modules.aiVisitNotes.domain.AiVisitNote;
import com.discipolat.modules.aiVisitNotes.domain.AiVisitNoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping({"/api/ai-visit-notes", "/api/v1/ai-visit-notes"})
@PreAuthorize("isAuthenticated()")
public class AiVisitNoteController {

    private final AiVisitNoteService service;
    public AiVisitNoteController(AiVisitNoteService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AiVisitNote note) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(note));
    }

    @GetMapping
    public ResponseEntity<?> list() { return ResponseEntity.ok(service.listAll()); }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> byMember(@PathVariable UUID memberId) { return ResponseEntity.ok(service.listByMember(memberId)); }

    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verify(@PathVariable UUID id) { return ResponseEntity.ok(service.verify(id)); }
}
