package com.discipolat.modules.backup.infrastructure;

import com.discipolat.modules.backup.domain.BackupDescriptor;
import com.discipolat.modules.backup.domain.BackupStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistance JDBC des descripteurs de sauvegarde, dans la table
 * {@code backup_archive}.
 *
 * <p>Le schéma de la table appartient à Flyway
 * ({@code V189__create_backup_archive.sql}) : ce dépôt n'ouvre que des
 * connexions, il ne crée jamais de table au démarrage (voir le commentaire
 * inline ci-dessous pour les raisons).
 *
 * <p>Toutes les requêtes sont des PreparedStatement : aucune valeur n'est
 * concaténée dans le SQL.
 */
@Repository
public class JdbcBackupDescriptorRepository implements BackupDescriptorRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcBackupDescriptorRepository.class);

    private static final String UPSERT = """
            INSERT INTO backup_archive (id, tenant_id, file_name, size_bytes, sha256, created_at, created_by, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                file_name = EXCLUDED.file_name,
                size_bytes = EXCLUDED.size_bytes,
                sha256 = EXCLUDED.sha256,
                status = EXCLUDED.status""";

    private static final String SELECT_COLUMNS =
            "SELECT id, tenant_id, file_name, size_bytes, sha256, created_at, created_by, status FROM backup_archive";

    private static final String SELECT_BY_ID = SELECT_COLUMNS + " WHERE id = ?";

    private static final String SELECT_BY_TENANT = SELECT_COLUMNS + " WHERE tenant_id = ? ORDER BY created_at DESC";

    private static final String UPDATE_STATUS = "UPDATE backup_archive SET status = ? WHERE id = ?";

    private static final String DELETE_BY_ID = "DELETE FROM backup_archive WHERE id = ?";

    private final DataSource dataSource;

    public JdbcBackupDescriptorRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Le schéma appartient a FLYWAY (V189__create_backup_archive.sql), pas au
    // code. Une creation de table au demarrage :
    //  - exigeait un droit CREATE sur le schema en base, ce qui n'est pas acquis
    //    dans un environnement de production verrouille ;
    //  - rendait le demarrage dependent du SGBD (le DDL echouait sur H2 avec
    //    « Unknown data type: TIMESTAMPTZ »), donc la suite de tests ne pouvait
    //    meme plus charger le contexte Spring ;
    //  - rendait la migration invisible : aucun historique, aucune reversibilite.
    // La table est donc creee par la migration, et ce depot n'ouvre que des
    // connexions.

    @Override
    public void save(BackupDescriptor descriptor) {
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            statement.setObject(1, descriptor.id());
            statement.setObject(2, descriptor.tenantId());
            statement.setString(3, descriptor.fileName());
            statement.setLong(4, descriptor.sizeBytes());
            statement.setString(5, descriptor.sha256());
            statement.setObject(6, toOffsetDateTime(descriptor.createdAt()));
            statement.setObject(7, descriptor.createdBy());
            statement.setString(8, descriptor.status().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'enregistrer la sauvegarde " + descriptor.id(), e);
        }
    }

    @Override
    public Optional<BackupDescriptor> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
            statement.setObject(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de lire la sauvegarde " + id, e);
        }
    }

    @Override
    public List<BackupDescriptor> findAllByTenant(UUID tenantId) {
        if (tenantId == null) {
            return List.of();
        }
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_TENANT)) {
            statement.setObject(1, tenantId);
            try (ResultSet rs = statement.executeQuery()) {
                List<BackupDescriptor> descriptors = new ArrayList<>();
                while (rs.next()) {
                    descriptors.add(map(rs));
                }
                return descriptors;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de lister les sauvegardes du tenant " + tenantId, e);
        }
    }

    @Override
    public void updateStatus(UUID id, BackupStatus status) {
        if (id == null || status == null) {
            return;
        }
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS)) {
            statement.setString(1, status.name());
            statement.setObject(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre à jour le statut de la sauvegarde " + id, e);
        }
    }

    @Override
    public void deleteById(UUID id) {
        if (id == null) {
            return;
        }
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID)) {
            statement.setObject(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer le descripteur de la sauvegarde " + id, e);
        }
    }

    private static BackupDescriptor map(ResultSet rs) throws SQLException {
        return new BackupDescriptor(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("file_name"),
                rs.getLong("size_bytes"),
                rs.getString("sha256"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                rs.getObject("created_by", UUID.class),
                BackupStatus.valueOf(rs.getString("status")));
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant toInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }
}
