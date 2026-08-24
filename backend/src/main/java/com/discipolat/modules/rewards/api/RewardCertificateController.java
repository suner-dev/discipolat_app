package com.discipolat.modules.rewards.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.rewards.domain.Certificate;
import com.discipolat.modules.rewards.domain.CertificateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * P3 #108 — Récompenses avancées : certificats et mentions.
 */
@RestController
@RequestMapping("/api/v1/reward-certificates")
public class RewardCertificateController {

    private final CertificateService service;

    public RewardCertificateController(CertificateService service) {
        this.service = service;
    }

    /** Mes certificats. */
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Certificate>> mine() {
        return ResponseEntity.ok(service.mine(SecurityUtils.getCurrentUserId()));
    }

    /** Certificats que je mérite (non encore émis). */
    @GetMapping("/eligible")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> eligible() {
        return ResponseEntity.ok(service.eligible(SecurityUtils.getCurrentUserId()));
    }

    /** Émettre un certificat (admin/pasteur). */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")
    public ResponseEntity<Certificate> issue(@RequestBody Certificate certificate) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.issue(certificate));
    }

    /** Liste des certificats de l'église. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")
    public ResponseEntity<List<Certificate>> list() {
        return ResponseEntity.ok(service.list());
    }
}
