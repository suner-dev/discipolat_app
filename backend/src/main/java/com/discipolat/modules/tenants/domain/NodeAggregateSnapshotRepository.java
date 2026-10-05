package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §E — repository des snapshots agrégés.
 * Lecture de la progression : le dernier snapshot + la série ordonnée.
 */
public interface NodeAggregateSnapshotRepository extends JpaRepository<NodeAggregateSnapshot, UUID> {

    Optional<NodeAggregateSnapshot> findFirstByTenantIdAndNodeIdOrderBySnapshotAtDesc(UUID tenantId, UUID nodeId);

    List<NodeAggregateSnapshot> findByTenantIdAndNodeIdOrderBySnapshotAtAsc(UUID tenantId, UUID nodeId);

    /** Tous les snapshots d'un tenant, du plus ancien au plus récent (parité de lecture de l'arbre). */
    List<NodeAggregateSnapshot> findByTenantIdOrderBySnapshotAtAsc(UUID tenantId);

    List<NodeAggregateSnapshot> findByTenantIdAndNodeIdInOrderBySnapshotAtDesc(UUID tenantId, List<UUID> nodeIds);

    void deleteByTenantIdAndNodeId(UUID tenantId, UUID nodeId);
}
