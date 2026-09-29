package com.discipolat.modules.backup.infrastructure;

import com.discipolat.modules.backup.domain.BackupDescriptor;
import com.discipolat.modules.backup.domain.BackupStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistance des descripteurs de sauvegarde.
 *
 * <p>Pourquoi pas d'entité JPA ? Le profil par défaut impose
 * {@code spring.jpa.hibernate.ddl-auto=none} et laisse Flyway maître du schéma :
 * l'implémentation est en JDBC pur sur la {@code DataSource} existante. Le schéma
 * de la table {@code backup_archive} est la propriété de la migration
 * {@code V189__create_backup_archive.sql} (source de vérité unique, convention
 * du dépôt) — ce package ne crée aucune table au démarrage.
 */
public interface BackupDescriptorRepository {

    /**
     * Insère ou met à jour un descripteur (upsert sur l'identifiant).
     *
     * @param descriptor descripteur à persister
     */
    void save(BackupDescriptor descriptor);

    /**
     * Recherche par identifiant, sans filtre de tenant : le filtrage tenant est
     * fait par le service, qui traite une sauvegarde d'un autre tenant comme
     * inexistante.
     *
     * @param id identifiant de la sauvegarde
     * @return descripteur si présent
     */
    Optional<BackupDescriptor> findById(UUID id);

    /**
     * Liste les descripteurs d'un tenant, du plus récent au plus ancien.
     *
     * @param tenantId tenant concerné
     * @return descripteurs du tenant
     */
    List<BackupDescriptor> findAllByTenant(UUID tenantId);

    /**
     * Met à jour le statut d'une sauvegarde.
     *
     * @param id     identifiant de la sauvegarde
     * @param status nouveau statut
     */
    void updateStatus(UUID id, BackupStatus status);

    /**
     * Supprime un descripteur. Silencieux si absent.
     *
     * @param id identifiant de la sauvegarde
     */
    void deleteById(UUID id);
}
