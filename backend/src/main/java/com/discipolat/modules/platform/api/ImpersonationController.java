package com.discipolat.modules.platform.api;

import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/platform/admin/impersonation")
public class ImpersonationController {

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public ImpersonationController(TenantRepository tenantRepository, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> startImpersonation(@RequestBody Map<String, Object> request) {
        UUID tenantId = UUID.fromString((String) request.get("tenantId"));
        String reason = (String) request.get("reason");
        
        tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        
        Instant startTime = Instant.now();
        auditService.log(UUID.randomUUID(), tenantId, "IMPERSONATION_START",
            "TENANT", tenantId, "SUCCESS", Map.of("reason", reason));
        
        String token = UUID.randomUUID().toString();
        
        return ResponseEntity.ok(Map.of(
            "token", token,
            "tenantId", tenantId.toString(),
            "tenantName", tenantRepository.findById(tenantId).get().getName(),
            "startTime", startTime.toString(),
            "expiresAt", Instant.now().plusSeconds(1800).toString()
        ));
    }

    @PostMapping("/stop")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Void> stopImpersonation() {
        auditService.log(UUID.randomUUID(), null, "IMPERSONATION_END",
            "PLATFORM", null, "SUCCESS", Map.of());
        return ResponseEntity.noContent().build();
    }
}
