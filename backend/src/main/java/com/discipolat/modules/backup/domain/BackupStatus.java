package com.discipolat.modules.backup.domain;

/**
 * Cycle de vie d'une archive de sauvegarde.
 *
 * <p>Transitions normales :
 * <pre>
 *   PENDING -&gt; RUNNING -&gt; COMPLETED -&gt; VERIFIED
 *                          -&gt; FAILED
 * </pre>
 *
 * <p>{@link #FAILED} est également posée par {@code BackupService.verify()} lorsque
 * l'archive est introuvable ou que son empreinte SHA-256 ne correspond plus à
 * celle enregistrée au moment de la création : une archive corrompue n'est plus
 * une sauvegarde exploitable, la signaler dans {@code GET /api/v1/backups} vaut
 * mieux que de la laisser passer pour une sauvegarde réussie.
 */
public enum BackupStatus {

    /** Archivage programmé, pas encore démarré. */
    PENDING,

    /** Archivage en cours d'écriture sur le disque. */
    RUNNING,

    /** Archive écrite et empreinte SHA-256 enregistrée. */
    COMPLETED,

    /** Échec de l'archivage, ou archive ensuite corrompue / manquante. */
    FAILED,

    /** Archive relue et empreinte SHA-256 recalculée : conforme à l'enregistrement. */
    VERIFIED
}
