package com.discipolat.modules.backup.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Résultat d'une vérification d'intégrité d'archive.
 *
 * <p>La vérification RELIT réellement le fichier et recalcule son SHA-256 : une
 * sauvegarde annoncée « vérifiée » sans lecture du fichier n'est qu'un
 * mensonge, c'est précisément le défaut que ce module supprime.
 *
 * @param backupId sauvegarde vérifiée
 * @param tenantId tenant propriétaire
 * @param valid {@code true} si l'archive existe et que son empreinte correspond
 * @param expectedSha256 empreinte enregistrée à la création (peut être nulle si l'archive n'existe plus)
 * @param actualSha256 empreinte recalculée sur le fichier, {@code null} si l'archive est absente
 * @param sizeBytes taille relue sur le disque, {@code -1} si l'archive est absente
 * @param checkedAt instant du calcul
 * @param message diagnostic lisible en français
 */
public record VerificationResult(
        UUID backupId,
        UUID tenantId,
        boolean valid,
        String expectedSha256,
        String actualSha256,
        long sizeBytes,
        Instant checkedAt,
        String message
) {

    /**
     * Archive conforme : relue, empreinte recalculée et égale à l'enregistrement.
     */
    public static VerificationResult ok(UUID backupId, UUID tenantId, String sha256, long sizeBytes, Instant checkedAt) {
        return new VerificationResult(backupId, tenantId, true, sha256, sha256, sizeBytes, checkedAt,
                "Archive intacte : empreinte SHA-256 conforme à l'enregistrement.");
    }

    /**
     * Archive corrompue : le fichier existe mais son contenu ne correspond plus.
     */
    public static VerificationResult corrupted(UUID backupId, UUID tenantId, String expectedSha256, String actualSha256,
                                              long sizeBytes, Instant checkedAt) {
        return new VerificationResult(backupId, tenantId, false, expectedSha256, actualSha256, sizeBytes, checkedAt,
                "Archive corrompue : l'empreinte SHA-256 recalculée ne correspond pas à celle enregistrée.");
    }

    /**
     * Archive absente du disque alors que son descripteur existe.
     */
    public static VerificationResult missing(UUID backupId, UUID tenantId, String expectedSha256, Instant checkedAt) {
        return new VerificationResult(backupId, tenantId, false, expectedSha256, null, -1L, checkedAt,
                "Archive introuvable sur le disque alors que son descripteur existe.");
    }
}
