package com.discipolat.modules.familyMeeting.api;

import com.discipolat.modules.familyMeeting.domain.FamilyMeeting;
import com.discipolat.modules.familyMeeting.domain.FamilyMeetingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/family-meetings")
@PreAuthorize("isAuthenticated()")
public class FamilyMeetingController {

    private final FamilyMeetingService service;
    public FamilyMeetingController(FamilyMeetingService service) { this.service = service; }

    @PostMapping("/generate/{familyId}")
    public ResponseEntity<?> generate(@PathVariable UUID familyId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generateAgenda(familyId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FamilyMeeting meeting) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(meeting));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.complete(id, body.get("minutes")));
    }

    @GetMapping("/family/{familyId}")
    public ResponseEntity<?> byFamily(@PathVariable UUID familyId) { return ResponseEntity.ok(service.listByFamily(familyId)); }

    @GetMapping
    public ResponseEntity<?> list() { return ResponseEntity.ok(service.listAll()); }
}
