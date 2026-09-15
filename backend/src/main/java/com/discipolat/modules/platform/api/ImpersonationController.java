package com.discipolat.modules.platform.api;

import com.discipolat.common.exception.BusinessRuleException;
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
    public ResponseEntity<Map<String, Object>> startImpersonation(@RequestBody Map<String, Object> requestBody, jakarta.servlet.http.HttpServletRequest httpRequest) {
        UUID tenantId = UUID.fromString((String) requestBody.get("tenantId"));
        String reason = (String) requestBody.get("reason");
        String targetUserEmail = (String) requestBody.get("targetUserEmail");

        // §G1.9 — Anti-élévation : on ne peut pas impersoner un autre super admin plateforme.
        if (targetUserEmail != null && !targetUserEmail.isBlank()) {
            if ("super@discipolat.com".equalsIgnoreCase(targetUserEmail.trim())) {
                throw new BusinessRuleException("Impossible d'impersoner un super admin plateforme", "SUPER_ADMIN_IMPERSONATION_FORBIDDEN");
            }
        }

        tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        Instant startTime = Instant.now();
        auditService.log(UUID.randomUUID(), tenantId, "IMPERSONATION_START", "TENANT",
            tenantId, "SUCCESS", Map.of("reason", reason != null ? reason : "", "targetUserEmail", targetUserEmail != null ? targetUserEmail : ""),
            httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), httpRequest);

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
    public ResponseEntity<Void> stopImpersonation(jakarta.servlet.http.HttpServletRequest httpRequest) {
        auditService.log(UUID.randomUUID(), null, "IMPERSONATION_END",
            "PLATFORM", null, "SUCCESS", Map.of(),
            httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), httpRequest);
        return ResponseEntity.noContent().build();
    }
}
