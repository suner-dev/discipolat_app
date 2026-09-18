package com.discipolat.modules.imports.api;

import com.discipolat.common.exception.ForbiddenException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.exports.domain.SpaceExportBundle;
import com.discipolat.modules.imports.domain.SpaceImportReport;
import com.discipolat.modules.imports.domain.SpaceImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * G4.5 — Import d'un document canonique {@code space_export_v1}.
 *
 * <p>{@code POST /api/v1/spaces/import} accepte soit un fichier
 * ({@code multipart/form-data}) soit le document JSON brut ({@code application/json}).
 * Le paramètre {@code dryRun} (défaut {@code false}) exécute une validation
 * sans aucune écriture.
 *
 * <p>Autorisation : super admin plateforme, ADMIN ou PASTEUR du tenant courant.
 * Aucun import inter-tenant n'est possible en v1.0 (le tenant cible est toujours
 * celui du contexte).
 */
@RestController
public class SpaceImportController {

    private final SpaceImportService importService;

    public SpaceImportController(SpaceImportService importService) {
        this.importService = importService;
    }

    private UUID tenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Aucun tenant dans le contexte");
        }
        return tenantId;
    }

    @PostMapping(value = "/api/v1/spaces/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.isPlatformSuperAdmin() or hasAnyRole('ADMIN','PASTEUR')")
    public ResponseEntity<SpaceImportReport> importFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun) throws IOException {
        SpaceExportBundle bundle = importService.parse(file.getBytes());
        UUID tenant = tenantId();
        return ResponseEntity.ok(dryRun
                ? importService.validate(bundle, tenant)
                : importService.apply(bundle, tenant));
    }

    @PostMapping(value = "/api/v1/spaces/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@authz.isPlatformSuperAdmin() or hasAnyRole('ADMIN','PASTEUR')")
    public ResponseEntity<SpaceImportReport> importJson(
            @RequestBody SpaceExportBundle bundle,
            @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun) {
        UUID tenant = tenantId();
        return ResponseEntity.ok(dryRun
                ? importService.validate(bundle, tenant)
                : importService.apply(bundle, tenant));
    }
}