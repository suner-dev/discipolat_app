package com.discipolat.modules.members.api;

import com.discipolat.modules.members.domain.CompetenceMatchingService;
import com.discipolat.modules.members.domain.MemberCompetence;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members/competences")
public class CompetenceMatchingController {

    private final CompetenceMatchingService service;

    public CompetenceMatchingController(CompetenceMatchingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MemberCompetence> addCompetence(@RequestBody MemberCompetence competence) {
        return ResponseEntity.ok(service.addCompetence(competence));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemberCompetence> updateCompetence(@PathVariable UUID id,
                                                              @RequestBody MemberCompetence competence) {
        return ResponseEntity.ok(service.updateCompetence(id, competence));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompetence(@PathVariable UUID id) {
        service.deleteCompetence(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine")
    public ResponseEntity<List<MemberCompetence>> myCompetences() {
        return ResponseEntity.ok(service.getMyCompetences());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MemberCompetence>> userCompetences(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.getUserCompetences(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestParam String competenceName) {
        return ResponseEntity.ok(service.findMembersWithCompetence(competenceName));
    }

    @PostMapping("/match")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<List<Map<String, Object>>> match(
            @RequestBody List<String> requiredCompetences,
            @RequestParam(defaultValue = "2") int minLevel) {
        return ResponseEntity.ok(service.findBestMatches(requiredCompetences, minLevel));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.getStats());
    }
}
