package com.discipolat.modules.backup.api;

import com.discipolat.common.exception.ResourceNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.backup.domain.BackupArchive;
import com.discipolat.modules.backup.domain.BackupResult;
import com.discipolat.modules.backup.domain.BackupService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * API de sauvegarde et de vérification d'intégrité.
 *
 * <p><b>Base : {@code /api/v1/backups}</b> (convention du dépôt).
 *
 * <p><b>Autorité : {@code @authz.isPlatformSuperAdmin()}.</b> Un dump complet
 * d'un tenant est une opération de PLATEFORME, pas d'église : même traitement
 * que {@code TenantController}, {@code ImpersonationController} et
 * {@code PlatformProvisioningController} dans ce codebase. Un administrateur
 * d'église n'a pas à pouvoir extraire l'intégralité de la base.
 *
 * <p><b>Isolation tenant.</b> Le tenant vient TOUJOURS de
 * {@link TenantContext#requireTenantId()}, jamais d'un corps de requête ni
 * d'un paramètre d'URL. Le super admin plateforme travaille donc dans le
 * contexte de tenant de sa requête : il ne voit que les sauvegardes de ce
 * tenant, comme n'importe quel appelant authentifié.
 */
@RestController
@RequestMapping("/api/v1/backups")
@PreAuthorize("@authz.isPlatformSuperAdmin()")
public class BackupController {

    private static final MediaType APPLICATION_GZIP = MediaType.parseMediaType("application/gzip");

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * Crée une sauvegarde du tenant courant.
     *
     * @return la sauvegarde créée avec son rapport d'export
     */
    @PostMapping
    public ResponseEntity<BackupResponse> create() {
        UUID tenantId = TenantContext.requireTenantId();
        BackupResult result = backupService.createWithReport(tenantId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(BackupResponse.from(result));
    }

    /**
     * Liste les sauvegardes du tenant courant, de la plus récente à la plus
     * ancienne.
     */
    @GetMapping
    public ResponseEntity<List<BackupResponse>> list() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(backupService.list(tenantId).stream().map(BackupResponse::from).toList());
    }

    /**
     * Détail d'une sauvegarde du tenant courant.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BackupResponse> get(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return backupService.find(id, tenantId)
                .map(BackupResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Vérifie l'intégrité d'une sauvegarde : relit l'archive et recalcule son
     * SHA-256. Annonce par le cahier de charges (CU-26) et longtemps
     * inexistante.
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<BackupVerificationResponse> verify(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(BackupVerificationResponse.from(backupService.verify(id, tenantId)));
    }

    /**
     * Supprime une sauvegarde du tenant courant (archive et descripteur).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        backupService.delete(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Télécharge l'archive d'une sauvegarde du tenant courant.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        BackupArchive archive = backupService.openArchive(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Archive de sauvegarde", "id", id));

        Resource resource = new FileSystemResource(archive.path());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archive.fileName() + "\"")
                .contentType(APPLICATION_GZIP)
                .contentLength(archive.sizeBytes())
                .body(resource);
    }
}
