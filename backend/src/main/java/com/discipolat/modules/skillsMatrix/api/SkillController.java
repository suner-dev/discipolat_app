package com.discipolat.modules.skillsMatrix.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.skillsMatrix.domain.SkillEvaluation;
import com.discipolat.modules.skillsMatrix.domain.SkillService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService service;

    public SkillController(SkillService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<SkillEvaluation>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/matrix")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> matrix() {
        return ResponseEntity.ok(service.getMatrix());
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<SkillEvaluation>> byMember(@PathVariable UUID memberId) {
        return ResponseEntity.ok(service.getByMembre(memberId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<SkillEvaluation> create(@RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        SkillEvaluation eval = service.create(
                UUID.fromString(body.get("membreId")),
                body.get("competence"),
                body.get("niveau"),
                body.get("commentaire"),
                userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(eval);
    }
}
