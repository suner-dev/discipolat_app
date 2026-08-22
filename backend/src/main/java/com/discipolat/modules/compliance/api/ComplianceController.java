package com.discipolat.modules.compliance.api;

import com.discipolat.modules.compliance.domain.ComplianceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/compliance")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
public class ComplianceController {

    private final ComplianceService service;

    public ComplianceController(ComplianceService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(service.getComplianceDashboard());
    }

    @GetMapping("/export/{userId}")
    public ResponseEntity<Map<String, Object>> exportUserData(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.exportUserData(userId));
    }

    @DeleteMapping("/user-data/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUserData(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.deleteUserData(userId));
    }

    @GetMapping("/retention")
    public ResponseEntity<Map<String, Object>> retentionPolicy() {
        return ResponseEntity.ok(service.getRetentionPolicy());
    }
}
