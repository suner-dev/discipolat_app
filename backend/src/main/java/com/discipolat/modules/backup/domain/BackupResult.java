package com.discipolat.modules.backup.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Résultat détaillé d'une opération de sauvegarde.
 *
 * <p>Complète le {@link BackupDescriptor} par les compteurs utiles à un
 * exploitant : combien de tables du tenant ont été exportées et combien de
 * lignes au total. Un backup « réussi » qui n'exporte que 3 lignes sur un
 * tenant de 12 000 âmes est un incident ; sans ces compteurs, rien ne le
 * signale.
 *
 * @param descriptor descripteur persisté de l'archive (statut attendu : {@code COMPLETED})
 * @param tablesExported nombre de tables du tenant réellement exportées
 * @param rowsExported nombre total de lignes exportées (0 pour un tenant vide)
 * @param durationMs durée de l'opération en millisecondes
 */
public record BackupResult(
        BackupDescriptor descriptor,
        int tablesExported,
        long rowsExported,
        long durationMs
) {

    /**
     * Constructeur de commodité.
     */
    public BackupResult {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor is required");
        }
    }

    /**
     * Fabrique statique alignée sur l'ordre des paramètres de l'appelant.
     */
    public static BackupResult of(BackupDescriptor descriptor, int tablesExported, long rowsExported, long durationMs) {
        return new BackupResult(descriptor, tablesExported, rowsExported, durationMs);
    }
}
