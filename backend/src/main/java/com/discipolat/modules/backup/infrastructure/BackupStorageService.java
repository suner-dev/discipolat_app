package com.discipolat.modules.backup.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * Stockage des archives de sauvegarde sur le système de fichiers.
 *
 * <p>Même approche et mêmes garde-fous que
 * {@code com.discipolat.modules.tenants.service.FileStorageService} : racine
 * normalisée créée au démarrage, un sous-dossier par tenant, et vérification
 * systématique que le chemin résolu reste SOUS la racine. On y ajoute un
 * contrôle explicite du nom de fichier, parce qu'ici le nom est stocké en base
 * puis relu : une seule valeur corrompue suffirait sinon à lire ou écraser un
 * fichier hors du dossier de sauvegarde.
 *
 * <p>Racine : {@code app.backup.root} (défaut {@code ./backups}), un dossier à
 * ajouter à {@code .gitignore} — ne jamais commiter d'archives de production.
 */
@Service
public class BackupStorageService {

    private static final Logger log = LoggerFactory.getLogger(BackupStorageService.class);

    /** Taille du tampon de lecture pour le calcul d'empreinte. */
    private static final int DIGEST_BUFFER_SIZE = 8192;

    /**
     * Longueur maximale acceptée pour un nom d'archive. Les noms produits par
     * le module font ~60 caractères ; la borne protège le système de fichiers
     * (limite 255 octets sur la plupart des systèmes Unix).
     */
    static final int MAX_FILE_NAME_LENGTH = 200;

    private static final String SHA256 = "SHA-256";

    private final Path rootLocation;

    /**
     * @param rootPath racine de stockage, configurée par {@code app.backup.root}
     */
    public BackupStorageService(@Value("${app.backup.root:./backups}") String rootPath) {
        this.rootLocation = Paths.get(rootPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier de sauvegarde: " + rootPath, e);
        }
        log.info("Dossier de sauvegarde initialisé: {}", this.rootLocation);
    }

    /**
     * Racine absolue de tous les dossiers de sauvegarde.
     */
    public Path rootLocation() {
        return this.rootLocation;
    }

    /**
     * Valide un nom de fichier d'archive et rejette toute tentative de sortie
     * du dossier de sauvegarde (traversée de chemin).
     *
     * <p>Motifs refusés : séparateur {@code /}, séparateur Windows {@code \},
     * fragment {@code ..}, caractère nul, chemin absolu, nom vide ou trop long.
     * Le refus est fail-closed et remonte en {@link SecurityException}, que
     * {@code GlobalExceptionHandler} traduit en 403 sans révéler le chemin.
     *
     * @param fileName nom à valider
     * @throws SecurityException si le nom est absent ou contient un chemin
     */
    public static void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new SecurityException("Nom de fichier d'archive invalide: absent");
        }
        if (fileName.length() > MAX_FILE_NAME_LENGTH) {
            throw new SecurityException("Nom de fichier d'archive invalide: trop long");
        }
        if (fileName.indexOf('\0') >= 0) {
            throw new SecurityException("Nom de fichier d'archive invalide: caractère nul");
        }
        if (fileName.contains("/") || fileName.contains("\\")) {
            throw new SecurityException("Nom de fichier d'archive invalide: séparateur de chemin interdit");
        }
        if (fileName.contains("..")) {
            throw new SecurityException("Nom de fichier d'archive invalide: traversée de chemin interdite");
        }
        if (Paths.get(fileName).isAbsolute() || Paths.get(fileName).getNameCount() != 1) {
            throw new SecurityException("Nom de fichier d'archive invalide: chemin non autorisé");
        }
    }

    /**
     * Dossier d'un tenant, créé à la demande, vérifié comme étant sous la racine.
     *
     * @param tenantId tenant concerné
     * @return dossier du tenant
     * @throws IllegalArgumentException si {@code tenantId} est nul
     * @throws SecurityException        si le dossier sort de la racine
     */
    public Path tenantRoot(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        Path tenantRoot = this.rootLocation.resolve(tenantId.toString()).normalize().toAbsolutePath();
        if (!tenantRoot.startsWith(this.rootLocation)) {
            throw new SecurityException("Tentative d'accès hors du dossier de sauvegarde");
        }
        try {
            Files.createDirectories(tenantRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier de sauvegarde du tenant: " + tenantId, e);
        }
        return tenantRoot;
    }

    /**
     * Résout le chemin d'une archive pour un tenant, après validation du nom.
     *
     * @param tenantId tenant concerné
     * @param fileName nom de l'archive
     * @return chemin absolu de l'archive
     * @throws SecurityException si le nom est invalide ou si le chemin sort du dossier du tenant
     */
    public Path resolveArchive(UUID tenantId, String fileName) {
        validateFileName(fileName);
        Path tenantRoot = tenantRoot(tenantId);
        Path archive = tenantRoot.resolve(fileName).normalize().toAbsolutePath();
        if (!archive.startsWith(tenantRoot)) {
            throw new SecurityException("Tentative d'accès hors du dossier de sauvegarde du tenant");
        }
        return archive;
    }

    /**
     * Écrit une archive compressée et calcule son empreinte en une seule passe.
     *
     * <p>Le digest enveloppe le flux GZIP : l'empreinte retournée est celle des
     * octets RÉELLEMENT écrits sur le disque, donc exactement celle que
     * {@link #sha256(Path)} recalculera plus tard. La calculer sur le texte non
     * compressé aurait produit une empreinte que la vérification ne pourrait
     * jamais retrouver.
     *
     * @param tenantId      tenant concerné
     * @param fileName      nom de l'archive (validé)
     * @param contentWriter producteur du contenu texte de l'archive
     * @return chemin, taille et empreinte de l'archive écrite
     * @throws IOException si l'écriture échoue
     */
    public WrittenArchive writeArchive(UUID tenantId, String fileName, ArchiveContentWriter contentWriter)
            throws IOException {
        Path target = resolveArchive(tenantId, fileName);
        MessageDigest digest = newSha256Digest();
        // Seul `raw` est fermé par le try-with-resources : la fermeture de
        // l'OutputStreamWriter termine proprement le flux GZIP avant que le
        // digest ne soit lu, et `raw` est alors déjà vidé sur le disque.
        try (OutputStream raw = new BufferedOutputStream(Files.newOutputStream(target))) {
            DigestOutputStream digestOut = new DigestOutputStream(raw, digest);
            GZIPOutputStream gzip = new GZIPOutputStream(digestOut);
            Writer writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8);
            try {
                contentWriter.write(writer);
            } finally {
                // Ferme puis VidE l'encodeur, ce qui termine le flux GZIP
                // (en-tête, corps, CRC) AVANT que le digest ne soit lu.
                writer.close();
            }
        }
        return new WrittenArchive(target, Files.size(target), hex(digest.digest()));
    }

    /**
     * Recalcule l'empreinte SHA-256 d'un fichier EN LE LISANT RÉELLEMENT.
     *
     * @param file fichier à relire
     * @return empreinte hexadécimale
     * @throws IOException si le fichier est illisible
     */
    public String sha256(Path file) throws IOException {
        MessageDigest digest = newSha256Digest();
        byte[] buffer = new byte[DIGEST_BUFFER_SIZE];
        try (DigestInputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            while (in.read(buffer) != -1) {
                // DigestInputStream met à jour le digest à chaque lecture
                continue;
            }
        }
        return hex(digest.digest());
    }

    /**
     * Supprime une archive. Silencieux si le fichier est déjà absent, comme le
     * fait {@code FileStorageService.delete}.
     *
     * @param tenantId tenant concerné
     * @param fileName nom de l'archive
     */
    public void deleteArchive(UUID tenantId, String fileName) {
        try {
            Files.deleteIfExists(resolveArchive(tenantId, fileName));
        } catch (IOException e) {
            log.warn("Impossible de supprimer l'archive {} du tenant {}: {}", fileName, tenantId, e.getMessage());
        } catch (SecurityException e) {
            log.error("Suppression d'une archive refusée (nom invalide) pour le tenant {}: {}", tenantId, fileName);
        }
    }

    private static MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance(SHA256);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithme SHA-256 indisponible", e);
        }
    }

    private static String hex(byte[] digest) {
        return HexFormat.of().formatHex(digest);
    }

    /**
     * Archive fraichement écrite sur le disque.
     *
     * @param path      chemin absolu
     * @param sizeBytes taille en octets
     * @param sha256    empreinte SHA-256 hexadécimale des octets écrits
     */
    public record WrittenArchive(Path path, long sizeBytes, String sha256) {
    }

    /**
     * Producteur du contenu texte d'une archive.
     *
     * <p>Le {@link Writer} fourni est managed par
     * {@link #writeArchive(UUID, String, ArchiveContentWriter)} : ne pas le
     * fermer, écrire dedans puis laisser faire.
     */
    @FunctionalInterface
    public interface ArchiveContentWriter {

        /**
         * Écrit le contenu de l'archive.
         *
         * @param writer writer UTF-8 à alimenter (ne pas le fermer)
         * @throws IOException si l'écriture échoue
         */
        void write(Writer writer) throws IOException;
    }
}
