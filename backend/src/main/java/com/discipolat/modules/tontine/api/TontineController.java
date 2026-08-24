package com.discipolat.modules.tontine.api;

import com.discipolat.modules.tontine.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tontines")
public class TontineController {

    private final TontineService service;

    public TontineController(TontineService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TontineGroup>> list() {
        return ResponseEntity.ok(service.listGroups());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<TontineGroup> create(@RequestBody TontineGroup group) {
        return ResponseEntity.ok(service.createGroup(group));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<TontineMember> addMember(@PathVariable UUID id, @RequestBody TontineMember member) {
        return ResponseEntity.ok(service.addMember(id, member));
    }

    @PostMapping("/{id}/contributions/{memberId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<TontineContribution> markPaid(@PathVariable UUID id,
                                                        @PathVariable UUID memberId,
                                                        @RequestParam(defaultValue = "1") int tour,
                                                        @RequestParam(required = false) String note) {
        return ResponseEntity.ok(service.markPaid(id, memberId, tour, note));
    }

    @PostMapping("/{id}/next-round")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> nextRound(@PathVariable UUID id) {
        return ResponseEntity.ok(service.nextRound(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(service.groupDetail(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.stats());
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getDashboard(id));
    }
}
