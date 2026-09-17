package com.discipolat.modules.health.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PharmacyItemRepository extends JpaRepository<PharmacyItem, UUID> {
    List<PharmacyItem> findByTenantIdAndDeletedFalse(UUID tenantId);
    Page<PharmacyItem> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    @Query("SELECT pi FROM PharmacyItem pi WHERE pi.tenantId = :tenantId AND LOWER(pi.nom) LIKE LOWER(CONCAT('%',:q,'%')) AND pi.deleted = false")
    Page<PharmacyItem> searchByNom(UUID tenantId, String q, Pageable pageable);
    List<PharmacyItem> findByCategorieAndTenantIdAndDeletedFalse(String categorie, UUID tenantId);
    Page<PharmacyItem> findByCategorieAndTenantIdAndDeletedFalse(String categorie, UUID tenantId, Pageable pageable);
}
