package com.discipolat.modules.backup.api;

import com.discipolat.modules.backup.domain.VerificationResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Représentation HTTP d'une vérification d'intégrité.
 *
 * <p>Les deux empreintes sont renvoyées quand elles diffèrent : sans
 * l'empreinte réellement recalculée, un exploitant ne peut pas distinguer une
 * archive tronquée d'une archive remplacée.
 */
public record BackupVerificationResponse(
        UUID backupId,
        UUID tenantId,
        boolean valid,
        String expectedSha256,
        String actualSha256,
        long sizeBytes,
        Instant checkedAt,
        String message
) {

    public static BackupVerificationResponse from(VerificationResult result) {
        return new BackupVerificationResponse(
                result.backupId(),
                result.tenantId(),
                result.valid(),
                result.expectedSha256(),
                result.actualSha256(),
                result.sizeBytes(),
                result.checkedAt(),
                result.message());
    }
}
