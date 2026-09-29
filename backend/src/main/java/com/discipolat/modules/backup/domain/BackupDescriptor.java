package com.discipolat.modules.backup.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Métadonnées d'une archive de sauvegarde, sans le contenu de l'archive.
 *
 * <p>Le descripteur est la seule chose qui survit au redémarrage : l'archive,
 * elle, vit sur le disque. Sans descripteur persistant, une sauvegarde-devient
 * orpheline au premier redémarrage de l'application — d'où l'implémentation
 * JDBC de {@code BackupDescriptorRepository} plutôt qu'un store en mémoire.
 *
 * @param id        identifiant de la sauvegarde (UUID)
 * @param tenantId  tenant propriétaire, jamais déduit d'une requête HTTP
 * @param fileName  nom du fichier dans le dossier de sauvegarde du tenant
 * @param sizeBytes taille de l'archive en octets
 * @param sha256    empreinte SHA-256 hexadécimale de l'archive telle qu'écrite sur le disque
 * @param createdAt instant de création
 * @param createdBy auteur de la demande (utilisateur authentifié)
 * @param status    état courant dans le cycle de vie
 */
public record BackupDescriptor(
        UUID id,
        UUID tenantId,
        String fileName,
        long sizeBytes,
        String sha256,
        Instant createdAt,
        UUID createdBy,
        BackupStatus status
) {
}
