package com.discipolat.modules.backup.domain;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Archive de sauvegarde résolue sur le disque, prête à être servie en
 * téléchargement.
 *
 * <p>Le {@link Path} n'est renvoyé qu'après les contrôles d'isolation de tenant
 * et de nom de fichier : l'appelant reçoit un chemin déjà validé, jamais un
 * nom de fichier brut à résoudre lui-même.
 *
 * @param backupId identifiant de la sauvegarde
 * @param fileName nom du fichier (validé)
 * @param path chemin absolu sur le disque
 * @param sizeBytes taille de l'archive
 */
public record BackupArchive(
        UUID backupId,
        String fileName,
        Path path,
        long sizeBytes
) {
}
