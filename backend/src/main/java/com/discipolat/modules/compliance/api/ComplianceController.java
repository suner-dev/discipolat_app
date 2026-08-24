package com.discipolat.modules.compliance.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.compliance.domain.ComplianceService;
import com.discipolat.modules.gdpr.domain.GdprRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {

    private final ComplianceService service;

    public ComplianceController(ComplianceService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.getStats());
    }

    @GetMapping("/gdpr")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<PageResponse<GdprRequest>> listRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<GdprRequest> result = service.listRequests(PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/gdpr/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<GdprRequest> getRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getRequest(id));
    }

    @PostMapping("/gdpr")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GdprRequest> createRequest(@RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GdprRequest req = service.createRequest(body.get("typeDemande"), userId,
                body.get("concerneId") != null ? UUID.fromString(body.get("concerneId")) : null,
                body.get("motif"));
        return ResponseEntity.status(HttpStatus.CREATED).body(req);
    }

    @PatchMapping("/gdpr/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<GdprRequest> processRequest(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.processRequest(id, body.get("statut"), body.get("resultat"), userId));
    }

    @PostMapping("/consent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logConsent(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        service.logConsent(userId, (String) body.get("typeConsentement"),
                (Boolean) body.get("accorde"), (String) body.get("details"));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
