package com.discipolat.modules.dataMigration.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.dataMigration.domain.LegacyMigrationService;
import com.discipolat.modules.dataMigration.domain.MigrationAudit;
import com.discipolat.modules.dataMigration.domain.MigrationJob;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * G4.6 — API du moteur de migration legacy.
 *
 * <p>Sécurité : rôle admin tenant uniquement (jamais un membre), tenant déduit du
 * contexte authentifié (§0.3 n°2), et double verrou applicatif : le toggle
 * {@code legacy_migration_enabled} (§G1.2) est vérifié côté service pour toute écriture.</p>
 */
@RestController
@RequestMapping("/api/v1/legacy-migration")
public class LegacyMigrationController {

    private final LegacyMigrationService service;

    public LegacyMigrationController(LegacyMigrationService service) {
        this.service = service;
    }

    private UUID tenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new SecurityException("Aucun tenant dans le contexte");
        return tenantId;
    }

    /** Catalogue des maps source → cible (règles + champs non mappables), sources réellement présentes. */
    @GetMapping("/maps")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<LegacyMigrationService.LegacyMap>> maps() {
        return ResponseEntity.ok(service.listMaps());
    }

    /** Simulation complète : rapport par table, aucune écriture cible. */
    @PostMapping("/modules/{moduleCode}/dry-run")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<MigrationJob> dryRun(@PathVariable String moduleCode) {
        return ResponseEntity.ok(service.dryRun(tenantId(), SecurityUtils.getCurrentUserId(), moduleCode));
    }

    /** Migration réelle (idempotente) — exige le toggle tenant `legacy_migration_enabled`. */
    @PostMapping("/modules/{moduleCode}/migrate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<MigrationJob> migrate(@PathVariable String moduleCode) {
        return ResponseEntity.ok(service.migrate(tenantId(), SecurityUtils.getCurrentUserId(), moduleCode));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<MigrationJob>> jobs() {
        return ResponseEntity.ok(service.listJobs(tenantId()));
    }

    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<MigrationJob> job(@PathVariable UUID jobId) {
        return ResponseEntity.ok(service.getJob(tenantId(), jobId));
    }

    /** Traçabilité ligne à ligne du job (source_id → target_id, statut, message). */
    @GetMapping("/jobs/{jobId}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<MigrationAudit>> audit(@PathVariable UUID jobId) {
        return ResponseEntity.ok(service.listAudit(tenantId(), jobId));
    }

    /** Annulation d'un job MIGRATE ≤ 30 jours : retire uniquement les lignes créées par ce job. */
    @PostMapping("/jobs/{jobId}/rollback")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<MigrationJob> rollback(@PathVariable UUID jobId) {
        return ResponseEntity.ok(service.rollback(tenantId(), jobId, SecurityUtils.getCurrentUserId()));
    }

    /** État du toggle tenant §G1.2 qui pilote le moteur. */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> status() {
        UUID tenant = tenantId();
        return ResponseEntity.ok(Map.of(
                "tenantId", tenant.toString(),
                "enabled", service.isEnabled(tenant),
                "modules", service.listMaps().stream()
                        .map(LegacyMigrationService.LegacyMap::moduleCode).toList()
        ));
    }
}
