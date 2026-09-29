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
 * {@code spring.jpa.hibernate.ddl-auto=none} et laisse Flyway maître du schéma
 * (155 migrations dans {@code db/migration}) : une nouvelle entité exigerait une
 * migration. L'implémentation ci-dessous est en JDBC pur sur la {@code DataSource}
 * existante et crée sa propre table en {@code CREATE TABLE IF NOT EXISTS}, ce
 * qui rend le module autonome.
 *
 * <p><b>TODO(intégrateur)</b> : reprendre ce {@code CREATE TABLE IF NOT EXISTS}
 * dans une migration Flyway dès qu'une migration peut être ajoutée, pour aligner
 * le module sur la convention du dépôt.
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
