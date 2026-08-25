package com.discipolat.modules.skillMatching.api;

import com.discipolat.modules.skillMatching.domain.SkillMatch;
import com.discipolat.modules.skillMatching.domain.SkillMatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/skill-matching")
@PreAuthorize("isAuthenticated()")
public class SkillMatchController {

    private final SkillMatchService service;
    public SkillMatchController(SkillMatchService service) { this.service = service; }

    @GetMapping
    public List<SkillMatch> list() { return service.listAll(); }

    @GetMapping("/{id}")
    public SkillMatch get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<SkillMatch> create(@RequestBody SkillMatch m) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(m));
    }

    @PostMapping("/{id}/respond")
    public SkillMatch respond(@PathVariable UUID id, @RequestParam SkillMatch.Statut decision) {
        return service.respond(id, decision);
    }

    @PostMapping("/run")
    public List<SkillMatch> runMatching() { return service.runMatching(); }

    @GetMapping("/stats")
    public Map<String, Object> stats() { return service.getStats(); }
}
