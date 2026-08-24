package com.discipolat.modules.skillsMatrix.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.skillsMatrix.domain.SkillEvaluation;
import com.discipolat.modules.skillsMatrix.domain.SkillsMatrixService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/skills-matrix")
public class SkillsMatrixController {

    private final SkillsMatrixService service;

    public SkillsMatrixController(SkillsMatrixService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SkillEvaluation>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/member/{membreId}")
    public ResponseEntity<List<SkillEvaluation>> listByMember(@PathVariable UUID membreId) {
        return ResponseEntity.ok(service.listByMember(membreId));
    }

    @GetMapping("/member/{membreId}/matrix")
    public ResponseEntity<Map<String, Object>> getMatrix(@PathVariable UUID membreId) {
        return ResponseEntity.ok(service.getMatrix(membreId));
    }

    @GetMapping("/department/{departmentId}/matrix")
    public ResponseEntity<Map<String, Object>> getDepartmentMatrix(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(service.getDepartmentMatrix(departmentId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SkillEvaluation> evaluate(@RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        SkillEvaluation eval = service.evaluate(
                UUID.fromString(body.get("membreId")),
                body.get("compétence"),
                body.get("niveau"),
                userId,
                body.get("commentaire")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(eval);
    }
}
