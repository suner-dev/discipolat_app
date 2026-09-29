package com.discipolat.modules.backup.domain;

import com.discipolat.common.exception.ResourceNotFoundException;
import com.discipolat.modules.backup.infrastructure.BackupDescriptorRepository;
import com.discipolat.modules.backup.infrastructure.BackupStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Sauvegarde logique par tenant, réalisée en JDBC depuis la {@code DataSource}
 * existante.
 *
 * <p><b>Choix d'implémentation.</b> {@code scripts/backup.sh} appelle
 * {@code pg_dump}, mais le code Java n'a AUCUN antécédent de
 * {@code ProcessBuilder}, {@code pg_dump} ou {@code psql} (vérifié sur
 * {@code src/main} et {@code src/test}). Tirer un processus externe depuis une
 * API HTTP demanderait un binaire {@code pg_dump} dans l'image, un mot de passe
 * en variable d'environnement, et ouvrirait une surface d'injection si le nom
 * de fichier entrait dans une ligne de commande. On fait donc l'équivalent
 * applicatif : un export SQL logique, table par table, filtré sur
 * {@code tenant_id}.
 *
 * <p><b>Découverte des tables par métadonnées, pas de SQL catalogue.</b> On
 * utilise {@link DatabaseMetaData} plutôt qu'une requête sur
 * {@code information_schema} : le code reste standard JDBC, et l'export
 * fonctionne à l'identique sur PostgreSQL et sur la base H2 des tests.
 *
 * <p>Un tenant sans aucune donnée ne provoque aucune erreur : la liste des
 * tables est simplement vide et une archive valide (vide) est produite.
 */
@Service
public class BackupServiceImpl implements BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);

    /**
     * Préfixe des archives, aligné sur {@code scripts/backup.sh} qui produit
     * {@code discipolat_<horodatage>.sql.gz}.
     */
    private static final String FILE_NAME_PREFIX = "discipolat_tenant-";

    private static final String FILE_NAME_SUFFIX = ".sql.gz";

    private static final DateTimeFormatter FILE_NAME_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** Colonne portée par les tables multi-tenant (cf. migrations {@code V135}…). */
    private static final String TENANT_COLUMN = "tenant_id";

    private final DataSource dataSource;
    private final BackupStorageService storage;
    private final BackupDescriptorRepository repository;
    private final int retentionDays;

    /**
     * Injection par constructeur, comme le reste du codebase.
     *
     * @param dataSource     source de données applicative
     * @param storage        stockage disque des archives
     * @param repository     persistance des descripteurs
     * @param retentionDays  rétention en jours ({@code app.backup.retention-days}), 0 désactive
     */
    public BackupServiceImpl(
            DataSource dataSource,
            BackupStorageService storage,
            BackupDescriptorRepository repository,
            @Value("${app.backup.retention-days:30}") int retentionDays) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource is required");
        this.storage = Objects.requireNonNull(storage, "storage is required");
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.retentionDays = retentionDays;
    }

    @Override
    public BackupDescriptor create(UUID tenantId, UUID actorId) {
        return createWithReport(tenantId, actorId).descriptor();
    }

    @Override
    public BackupResult createWithReport(UUID tenantId, UUID actorId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }

        long startedAtNanos = System.nanoTime();
        UUID backupId = UUID.randomUUID();
        String fileName = buildFileName(tenantId, backupId);
        Instant createdAt = Instant.now();

        // Valide le nom et crée le dossier du tenant AVANT d'ouvrir un flux.
        this.storage.resolveArchive(tenantId, fileName);
        this.repository.save(new BackupDescriptor(backupId, tenantId, fileName, 0L, "", createdAt, actorId,
                BackupStatus.RUNNING));

        DumpStats stats = new DumpStats();
        try {
            BackupStorageService.WrittenArchive written = this.storage.writeArchive(tenantId, fileName,
                    writer -> writeLogicalDump(writer, tenantId, stats));

            BackupDescriptor completed = new BackupDescriptor(
                    backupId, tenantId, fileName, written.sizeBytes(), written.sha256(), createdAt, actorId,
                    BackupStatus.COMPLETED);
            this.repository.save(completed);

            long durationMs = elapsedMs(startedAtNanos);
            log.info("Sauvegarde {} créée pour le tenant {} : {} octets, {} tables, {} lignes en {} ms",
                    backupId, tenantId, written.sizeBytes(), stats.tables, stats.rows, durationMs);

            int purged = purgeExpired(tenantId);
            if (purged > 0) {
                log.info("Rétention : {} sauvegarde(s) de plus de {} jours supprimées pour le tenant {}",
                        purged, this.retentionDays, tenantId);
            }

            return BackupResult.of(completed, stats.tables, stats.rows, durationMs);
        } catch (IOException | RuntimeException e) {
            this.repository.save(new BackupDescriptor(backupId, tenantId, fileName, 0L, "", createdAt, actorId,
                    BackupStatus.FAILED));
            this.storage.deleteArchive(tenantId, fileName);
            log.error("Échec de la sauvegarde {} pour le tenant {}: {}", backupId, tenantId, e.getMessage(), e);
            throw new IllegalStateException("Échec de la sauvegarde du tenant " + tenantId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<BackupDescriptor> find(UUID id, UUID tenantId) {
        if (id == null || tenantId == null) {
            return Optional.empty();
        }
        return this.repository.findById(id).filter(descriptor -> tenantId.equals(descriptor.tenantId()));
    }

    @Override
    public List<BackupDescriptor> list(UUID tenantId) {
        if (tenantId == null) {
            return List.of();
        }
        return this.repository.findAllByTenant(tenantId).stream()
                .filter(descriptor -> tenantId.equals(descriptor.tenantId()))
                .sorted(Comparator.comparing(BackupDescriptor::createdAt).reversed())
                .toList();
    }

    @Override
    public VerificationResult verify(UUID id, UUID tenantId) {
        BackupDescriptor descriptor = requireOwnedDescriptor(id, tenantId);
        Path archive = this.storage.resolveArchive(tenantId, descriptor.fileName());
        Instant checkedAt = Instant.now();

        if (!Files.isRegularFile(archive)) {
            this.repository.updateStatus(descriptor.id(), BackupStatus.FAILED);
            log.error("Archive de sauvegarde {} introuvable sur le disque ({})", descriptor.id(), archive);
            return VerificationResult.missing(descriptor.id(), tenantId, descriptor.sha256(), checkedAt);
        }

        // Lecture réelle du fichier : c'est le cœur de la vérification.
        String actualSha256;
        long sizeBytes;
        try {
            actualSha256 = this.storage.sha256(archive);
            sizeBytes = Files.size(archive);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de relire l'archive de sauvegarde " + descriptor.fileName(), e);
        }

        boolean valid = actualSha256.equalsIgnoreCase(descriptor.sha256());
        this.repository.updateStatus(descriptor.id(), valid ? BackupStatus.VERIFIED : BackupStatus.FAILED);

        if (valid) {
            log.info("Archive de sauvegarde {} vérifiée: empreinte conforme", descriptor.id());
            return VerificationResult.ok(descriptor.id(), tenantId, actualSha256, sizeBytes, checkedAt);
        }

        log.error("Archive de sauvegarde {} CORROMPUE: attendu {}, obtenu {}",
                descriptor.id(), descriptor.sha256(), actualSha256);
        return VerificationResult.corrupted(descriptor.id(), tenantId, descriptor.sha256(), actualSha256,
                sizeBytes, checkedAt);
    }

    @Override
    public void delete(UUID id, UUID tenantId) {
        BackupDescriptor descriptor = requireOwnedDescriptor(id, tenantId);
        this.storage.deleteArchive(tenantId, descriptor.fileName());
        this.repository.deleteById(descriptor.id());
        log.info("Sauvegarde {} supprimée pour le tenant {}", descriptor.id(), tenantId);
    }

    @Override
    public Optional<BackupArchive> openArchive(UUID id, UUID tenantId) {
        if (id == null || tenantId == null) {
            return Optional.empty();
        }
        return this.repository.findById(id)
                .filter(descriptor -> tenantId.equals(descriptor.tenantId()))
                .map(descriptor -> {
                    Path archive = this.storage.resolveArchive(tenantId, descriptor.fileName());
                    if (!Files.isRegularFile(archive)) {
                        throw new ResourceNotFoundException("Archive de sauvegarde", "fileName", descriptor.fileName());
                    }
                    return new BackupArchive(descriptor.id(), descriptor.fileName(), archive, descriptor.sizeBytes());
                });
    }

    @Override
    public int purgeExpired(UUID tenantId) {
        // 0 ou négatif = rétention désactivée. Sans cette garde, une rétention à
        // 0 supprimerait la sauvegarde que l'appelant vient de créer.
        if (this.retentionDays <= 0) {
            return 0;
        }
        if (tenantId == null) {
            return 0;
        }
        Instant cutoff = Instant.now().minus(Duration.ofDays(this.retentionDays));
        int purged = 0;
        for (BackupDescriptor descriptor : this.repository.findAllByTenant(tenantId)) {
            if (descriptor.createdAt() == null || !descriptor.createdAt().isBefore(cutoff)) {
                continue;
            }
            this.storage.deleteArchive(tenantId, descriptor.fileName());
            this.repository.deleteById(descriptor.id());
            purged++;
        }
        return purged;
    }

    // ==================== Export logique ====================

    /**
     * Écrit l'export SQL logique du tenant dans l'archive.
     *
     * <p>Le {@link Writer} n'est volontairement pas fermé : il est géré par
     * {@link BackupStorageService#writeArchive}, qui doit terminer le flux GZIP
     * avant de lire l'empreinte.
     */
    private void writeLogicalDump(Writer out, UUID tenantId, DumpStats stats) throws IOException {
        out.write("-- Discipolat - sauvegarde logique du tenant " + tenantId + System.lineSeparator());
        out.write("-- Générée le " + Instant.now() + System.lineSeparator());
        out.write("-- Périmètre : lignes des tables multi-tenant filtrées sur " + TENANT_COLUMN
                + System.lineSeparator());
        out.write(System.lineSeparator());

        try (Connection connection = this.dataSource.getConnection()) {
            List<String> tenantTables = discoverTenantTables(connection);
            for (String table : tenantTables) {
                exportTable(connection, tenantId, table, out, stats);
            }
        } catch (SQLException e) {
            throw new IOException("Échec de l'export SQL du tenant " + tenantId, e);
        }

        out.write("-- Fin de la sauvegarde : " + stats.tables + " table(s), " + stats.rows + " ligne(s)"
                + System.lineSeparator());
    }

    /**
     * Liste les tables de la base possédant une colonne {@code tenant_id}.
     *
     * <p>Passer par {@link DatabaseMetaData} plutôt que par une requête sur
     * {@code pg_catalog} garde ce code en SQL standard, donc exécutable aussi
     * sur la base H2 des tests.
     */
    private List<String> discoverTenantTables(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();

        List<String> candidates = new ArrayList<>();
        try (ResultSet tables = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (tableName != null && !tableName.isBlank()) {
                    candidates.add(tableName);
                }
            }
        }

        List<String> tenantTables = new ArrayList<>();
        for (String tableName : candidates) {
            try (ResultSet columns = metaData.getColumns(catalog, null, tableName, TENANT_COLUMN)) {
                if (columns.next()) {
                    tenantTables.add(tableName);
                }
            }
        }
        // Ordre stable : deux sauvegardes du même jeu de données produisent
        // alors des archives comparables octet pour octet.
        tenantTables.sort(String::compareTo);
        return tenantTables;
    }

    /**
     * Émet les {@code INSERT} d'une table, restreints au tenant courant.
     */
    private void exportTable(Connection connection, UUID tenantId, String table, Writer out, DumpStats stats)
            throws SQLException, IOException {
        String quotedTable = quoteIdentifier(table);
        // Le nom de table vient de DatabaseMetaData et n'est jamais fourni par
        // l'utilisateur ; il est systématiquement quoté.
        String sql = "SELECT * FROM " + quotedTable + " WHERE " + quoteIdentifier(TENANT_COLUMN) + " = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, tenantId);
            try (ResultSet rows = statement.executeQuery()) {
                ResultSetMetaData rowMetaData = rows.getMetaData();
                int columnCount = rowMetaData.getColumnCount();

                out.write("-- Table: " + table + System.lineSeparator());
                out.write("INSERT INTO " + quotedTable + " (" + columnList(rowMetaData, columnCount) + ") VALUES"
                        + System.lineSeparator());

                boolean first = true;
                long rowCount = 0;
                while (rows.next()) {
                    if (!first) {
                        out.write("," + System.lineSeparator());
                    }
                    out.write("(");
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) {
                            out.write(", ");
                        }
                        out.write(toSqlLiteral(rows.getObject(i)));
                    }
                    out.write(")");
                    first = false;
                    rowCount++;
                }

                if (first) {
                    out.write("-- (aucune ligne pour ce tenant)" + System.lineSeparator());
                } else {
                    out.write(";" + System.lineSeparator());
                }
                out.write(System.lineSeparator());

                stats.tables++;
                stats.rows += rowCount;
            }
        }
    }

    private static String columnList(ResultSetMetaData rowMetaData, int columnCount) throws SQLException {
        StringBuilder columns = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                columns.append(", ");
            }
            columns.append(quoteIdentifier(rowMetaData.getColumnLabel(i)));
        }
        return columns.toString();
    }

    /**
     * Convertit une valeur JDBC en littéral SQL.
     *
     * <p>Les chaînes sont échappées en doublant les apostrophes. Les valeurs
     * UUID, temporelles et numériques sont écrites sans guillemets, ce que
     * PostgreSQL accepte pour une colonne du type correspondant.
     */
    private static String toSqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof byte[] bytes) {
            return "'\\x" + HexFormat.of().formatHex(bytes) + "'::bytea";
        }
        if (value instanceof Boolean flag) {
            return flag ? "TRUE" : "FALSE";
        }
        if (value instanceof Number
                || value instanceof UUID
                || value instanceof java.time.temporal.Temporal
                || value instanceof java.util.Date) {
            return value.toString();
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }

    private static String quoteIdentifier(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    /**
     * Nom d'archive tenant-scoped, aligné sur {@code scripts/backup.sh}.
     * Construit à partir d'UUID et d'un horodatage : aucune donnée utilisateur
     * n'y entre, et il est de toute façon revalidé par
     * {@link BackupStorageService#validateFileName(String)}.
     */
    private static String buildFileName(UUID tenantId, UUID backupId) {
        String timestamp = FILE_NAME_TIMESTAMP.format(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        return FILE_NAME_PREFIX
                + tenantId.toString().substring(0, 8)
                + "_" + timestamp
                + "_" + backupId.toString().substring(0, 8)
                + FILE_NAME_SUFFIX;
    }

    /**
     * Charge un descripteur en vérifiant qu'il appartient bien au tenant.
     *
     * <p>Une sauvegarde existante mais appartenant à un AUTRE tenant est traitée
     * comme inexistante (404) et non comme interdite (403) : répondre « 403 »
     * confirmerait à l'appelant que cet identifiant existe.
     */
    private BackupDescriptor requireOwnedDescriptor(UUID id, UUID tenantId) {
        if (id == null || tenantId == null) {
            throw new IllegalArgumentException("backupId and tenantId are required");
        }
        return this.repository.findById(id)
                .filter(descriptor -> tenantId.equals(descriptor.tenantId()))
                .orElseThrow(() -> new ResourceNotFoundException("Backup", "id", id));
    }

    private static long elapsedMs(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }

    /**
     * Compteurs de l'export, partagés entre l'opération d'écriture et
     * l'appelant.
     */
    private static final class DumpStats {

        private int tables;
        private long rows;
    }
}
