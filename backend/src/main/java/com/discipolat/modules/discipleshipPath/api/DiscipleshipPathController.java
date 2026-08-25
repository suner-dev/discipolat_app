package com.discipolat.modules.discipleshipPath.api;

import com.discipolat.modules.discipleshipPath.domain.DiscipleshipPath;
import com.discipolat.modules.discipleshipPath.domain.DiscipleshipPathService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/discipleship-paths")
@PreAuthorize("isAuthenticated()")
public class DiscipleshipPathController {

    private final DiscipleshipPathService service;
    public DiscipleshipPathController(DiscipleshipPathService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> list() { return ResponseEntity.ok(service.listAll()); }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> getForMember(@PathVariable UUID memberId) { return ResponseEntity.ok(service.getOrCreate(memberId)); }

    @PostMapping("/member/{memberId}")
    public ResponseEntity<?> createForMember(@PathVariable UUID memberId) { return ResponseEntity.ok(service.getOrCreate(memberId)); }

    @PostMapping("/{id}/advance")
    public ResponseEntity<?> advance(@PathVariable UUID id) { return ResponseEntity.ok(service.advanceStage(id)); }

    @GetMapping("/stage/{stage}")
    public ResponseEntity<?> byStage(@PathVariable DiscipleshipPath.Stage stage) { return ResponseEntity.ok(service.listByStage(stage)); }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() { return ResponseEntity.ok(service.getStats()); }
}
