package com.discipolat.modules.backup.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sécurité du nom de fichier d'archive — exigence de non-traversée de chemin.
 *
 * <p>Le nom d'archive est écrit en base puis relu à la lecture, à la
 * vérification et au téléchargement. Une seule valeur corrompue
 * ({@code ../../../etc/passwd}) suffirait à lire ou écraser un fichier hors du
 * dossier de sauvegarde : le garde-fou est donc testé sur chaque forme
 * d'entrée, y compris via la résolution complète.
 */
class BackupFileNameTest {

    // ==================== Noms rejetés ====================

    @ParameterizedTest(name = "rejeté: \"{0}\"")
    @DisplayName("Un nom contenant une traversée de chemin est rejeté")
    @ValueSource(strings = {
            "..",
            "../",
            "../secrets.sql.gz",
            "../../etc/passwd",
            "..\\..\\windows\\system32\\config",
            "backups/../../../etc/passwd",
            "a/b",
            "a/b/c.sql.gz",
            "discipolat_tenant-1234_20260101_000000_abcd.sql.gz/../../x"
    })
    void traverseeDeCheminRejetee(String fileName) {
        assertThrows(SecurityException.class, () -> BackupStorageService.validateFileName(fileName),
                "Le nom \"" + fileName + "\" aurait dû être rejeté");
    }

    @ParameterizedTest(name = "rejeté: \"{0}\"")
    @DisplayName("Un chemin absolu ou un séparateur Windows est rejeté")
    @ValueSource(strings = {
            "/etc/passwd",
            "/tmp/archive.sql.gz",
            "C:\\Windows\\System32\\config",
            "C:/Windows/System32/config",
            "\\\\serveur\\partage\\archive.sql.gz"
    })
    void cheminAbsoluRejete(String fileName) {
        assertThrows(SecurityException.class, () -> BackupStorageService.validateFileName(fileName),
                "Le chemin \"" + fileName + "\" aurait dû être rejeté");
    }

    @Test
    @DisplayName("Un nom vide, nul ou composé d'espaces est rejeté")
    void nomInvalideRejete() {
        assertThrows(SecurityException.class, () -> BackupStorageService.validateFileName(null));
        assertThrows(SecurityException.class, () -> BackupStorageService.validateFileName(""));
        assertThrows(SecurityException.class, () -> BackupStorageService.validateFileName("   "));
    }

    @Test
    @DisplayName("Un nom contenant un caractère nul est rejeté")
    void caractereNulRejete() {
        assertThrows(SecurityException.class, () -> BackupStorageService.validateFileName("archive\0.sql.gz"));
    }

    @Test
    @DisplayName("Un nom trop long est rejeté")
    void nomTropLongRejete() {
        String tooLong = "a".repeat(BackupStorageService.MAX_FILE_NAME_LENGTH + 1);
        assertThrows(SecurityException.class, () -> BackupStorageService.validateFileName(tooLong));
    }

    // ==================== Noms acceptés ====================

    @Test
    @DisplayName("Un nom d'archive bien formé est accepté")
    void nomValideAccepte() {
        assertDoesNotThrow(() ->
                BackupStorageService.validateFileName("discipolat_tenant-1a2b3c4d_20260101_120000_abcd1234.sql.gz"));
    }

    // ==================== La résolution complète reste confinée ====================

    @Test
    @DisplayName("resolveArchive confine l'archive dans le dossier du tenant")
    void resolveArchiveConfineAuDossierDuTenant() throws IOException {
        Path root = Files.createTempDirectory("backup-root-test");
        BackupStorageService storage = new BackupStorageService(root.toString());
        UUID tenantId = UUID.randomUUID();

        Path resolved = storage.resolveArchive(tenantId, "discipolat_tenant-1a2b3c4d_20260101_120000_abcd1234.sql.gz");

        assertEquals(storage.tenantRoot(tenantId), resolved.getParent(),
                "L'archive doit être un fichier direct du dossier du tenant");
        assertTrue(resolved.startsWith(storage.rootLocation()),
                "L'archive doit rester sous la racine de sauvegarde");
    }

    @Test
    @DisplayName("resolveArchive refuse une traversée de chemin vers un fichier déjà existant hors racine")
    void resolveArchiveRefuseSortieDeLaRacine(@TempDir Path tempDir) throws IOException {
        // Cible réelle, hors du dossier de sauvegarde : si le garde-fou cédait,
        // ce fichier deviendrait lisible / écrasable via un nom d'archive piégé.
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "ne-pas-lire", StandardCharsets.UTF_8);

        Path root = Files.createTempDirectory("backup-root-test-2");
        BackupStorageService storage = new BackupStorageService(root.toString());
        UUID tenantId = UUID.randomUUID();

        // Forme relative avec remontée d'arborescence
        assertThrows(SecurityException.class, () -> storage.resolveArchive(tenantId, "../" + secret.getFileName()));
        assertThrows(SecurityException.class, () -> storage.resolveArchive(tenantId, "../../etc/passwd"));

        // Forme absolue, indépendante de la profondeur de dossier
        assertThrows(SecurityException.class, () -> storage.resolveArchive(tenantId, secret.toString()));

        // Le fichier visé est resté intact
        assertTrue(Files.exists(secret));
        assertEquals("ne-pas-lire", Files.readString(secret, StandardCharsets.UTF_8));
    }
}
