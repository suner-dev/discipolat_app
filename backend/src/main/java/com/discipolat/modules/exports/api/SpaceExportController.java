package com.discipolat.modules.exports.api;

import com.discipolat.common.exception.ForbiddenException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.exports.domain.SpaceExportBundle;
import com.discipolat.modules.exports.domain.SpaceExportService;
import com.discipolat.modules.spaces.domain.Space;
import com.discipolat.modules.spaces.domain.SpaceService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * G4.5 — Export canonique {@code space_export_v1}.
 *
 * <p>Deux points d'entrée :
 * <ul>
 *   <li>{@code GET /api/v1/spaces/{spaceId}/export} : configuration d'un espace,
 *       soumise à {@link SpaceService#canCustomize} (un chef de famille n'exporte que sa famille) ;</li>
 *   <li>{@code GET /api/v1/tenant/export} : export complet du tenant courant
 *       (super admin plateforme, ADMIN ou PASTEUR).</li>
 * </ul>
 */
@RestController
public class SpaceExportController {

    private final SpaceExportService exportService;
    private final SpaceService spaceService;

    public SpaceExportController(SpaceExportService exportService, SpaceService spaceService) {
        this.exportService = exportService;
        this.spaceService = spaceService;
    }

    private UUID tenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Aucun tenant dans le contexte");
        }
        return tenantId;
    }

    @GetMapping("/api/v1/spaces/{spaceId}/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> exportSpace(@PathVariable UUID spaceId) {
        UUID tenant = tenantId();
        Space space = spaceService.getSpace(tenant, spaceId);
        if (!spaceService.canCustomize(SecurityUtils.getCurrentUserId(), space)) {
            throw new ForbiddenException("Export de cet espace non autorisé");
        }
        SpaceExportBundle bundle = exportService.exportSpace(tenant, spaceId);
        return download(bundle, "space_" + (space.getCode() == null ? spaceId : space.getCode()));
    }

    @GetMapping("/api/v1/tenant/export")
    @PreAuthorize("@authz.isPlatformSuperAdmin() or hasAnyRole('ADMIN','PASTEUR')")
    public ResponseEntity<ByteArrayResource> exportTenant() {
        UUID tenant = tenantId();
        SpaceExportBundle bundle = exportService.exportTenant(tenant);
        return download(bundle, "tenant_" + tenant);
    }

    /** Décrit le format canonique attendu (utile aux clients web/mobile avant import). */
    @GetMapping("/api/v1/spaces/import-format")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> describeFormat() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("format", SpaceExportBundle.FORMAT);
        info.put("version", SpaceExportBundle.VERSION);
        info.put("scopes", java.util.List.of(SpaceExportBundle.SCOPE_SPACE, SpaceExportBundle.SCOPE_TENANT));
        info.put("sections", java.util.List.of(
                "space", "spaces", "modules", "customFields", "statuses", "workflows", "data"));
        info.put("idempotent", true);
        info.put("dryRunSupported", true);
        return ResponseEntity.ok(info);
    }

    private ResponseEntity<ByteArrayResource> download(SpaceExportBundle bundle, String prefix) {
        byte[] payload = exportService.toJson(bundle);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", exportService.fileName(prefix));
        headers.setContentLength(payload.length);
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(payload));
    }
}