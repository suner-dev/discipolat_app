package com.discipolat.modules.inventory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Page<InventoryItem> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<InventoryItem> findByTenantIdAndCategorie(UUID tenantId, String categorie, Pageable pageable);

    Page<InventoryItem> findByTenantIdAndStatut(UUID tenantId, String statut, Pageable pageable);

    @Query("SELECT i FROM InventoryItem i WHERE i.tenantId = :tenantId AND (LOWER(i.nom) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<InventoryItem> search(UUID tenantId, String q, Pageable pageable);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatut(UUID tenantId, String statut);

    long countByTenantIdAndDepartementId(UUID tenantId, UUID departementId);
}
