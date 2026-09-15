package com.discipolat.modules.health.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PharmacyStockRepository extends JpaRepository<PharmacyStock, UUID> {
    List<PharmacyStock> findByPharmacyItemIdAndTenantId(UUID pharmacyItemId, UUID tenantId);
    Page<PharmacyStock> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    @Query("SELECT ps FROM PharmacyStock ps WHERE ps.tenantId = :tenantId AND ps.deleted = false AND ps.quantite <= ps.seuilAlerte")
    List<PharmacyStock> findLowStock(UUID tenantId);
    @Query("SELECT ps FROM PharmacyStock ps WHERE ps.tenantId = :tenantId AND ps.deleted = false AND ps.dateExpiration <= :threshold AND ps.status <> 'EXPIR E'")
    List<PharmacyStock> findExpiringBefore(UUID tenantId, LocalDate threshold);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
