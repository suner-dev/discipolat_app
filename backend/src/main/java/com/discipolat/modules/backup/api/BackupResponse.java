package com.discipolat.modules.backup.api;

import com.discipolat.modules.backup.domain.BackupDescriptor;
import com.discipolat.modules.backup.domain.BackupResult;
import com.discipolat.modules.backup.domain.BackupStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Représentation HTTP d'une sauvegarde.
 *
 * <p>Le chemin absolu de l'archive n'est JAMAIS exposé : seul le nom de
 * fichier, à titre d'information, et l'empreinte SHA-256, qui est déjà une
 * information publique de l'archive.
 */
public record BackupResponse(
        UUID id,
        UUID tenantId,
        String fileName,
        long sizeBytes,
        String sha256,
        Instant createdAt,
        UUID createdBy,
        BackupStatus status,
        Integer tablesExported,
        Long rowsExported,
        Long durationMs
) {

    /**
     * Vue sans les compteurs d'exécution (listes et lecture unitaire).
     */
    public static BackupResponse from(BackupDescriptor descriptor) {
        return new BackupResponse(
                descriptor.id(),
                descriptor.tenantId(),
                descriptor.fileName(),
                descriptor.sizeBytes(),
                descriptor.sha256(),
                descriptor.createdAt(),
                descriptor.createdBy(),
                descriptor.status(),
                null,
                null,
                null);
    }

    /**
     * Vue complète renvoyée par la création, avec le rapport d'export.
     */
    public static BackupResponse from(BackupResult result) {
        BackupDescriptor descriptor = result.descriptor();
        return new BackupResponse(
                descriptor.id(),
                descriptor.tenantId(),
                descriptor.fileName(),
                descriptor.sizeBytes(),
                descriptor.sha256(),
                descriptor.createdAt(),
                descriptor.createdBy(),
                descriptor.status(),
                result.tablesExported(),
                result.rowsExported(),
                result.durationMs());
    }
}
