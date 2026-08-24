package com.discipolat.modules.visits.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.visits.domain.PastoralVisit;
import com.discipolat.modules.visits.domain.PastoralVisitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pastoral-visits")
public class PastoralVisitController {

    private final PastoralVisitService service;

    public PastoralVisitController(PastoralVisitService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PastoralVisit>> list(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String statut) {
        if (start != null && end != null) {
            return ResponseEntity.ok(service.listByRange(LocalDateTime.parse(start), LocalDateTime.parse(end)));
        }
        return ResponseEntity.ok(service.listByVisitor(SecurityUtils.getCurrentUserId(), statut));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PastoralVisit> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PastoralVisit> create(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PastoralVisit visit = service.create(
                userId,
                UUID.fromString((String) body.get("membreId")),
                (String) body.get("motif"),
                LocalDateTime.parse((String) body.get("prévuLe")),
                (String) body.get("notes")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(visit);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PastoralVisit> complete(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.complete(id, body.get("notes")));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PastoralVisit> reschedule(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.reschedule(id, LocalDateTime.parse(body.get("newDate"))));
    }

    @PostMapping("/auto-generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<PastoralVisit>> autoGenerate(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> membreIdsStr = (List<String>) body.get("membreIds");
        List<UUID> membreIds = membreIdsStr.stream().map(UUID::fromString).toList();
        int jours = body.get("joursAVenir") != null ? (int) body.get("joursAVenir") : 14;
        return ResponseEntity.ok(service.generateAutoVisits(membreIds, SecurityUtils.getCurrentUserId(), jours));
    }
}
