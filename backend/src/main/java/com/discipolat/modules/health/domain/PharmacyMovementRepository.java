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
public interface PharmacyMovementRepository extends JpaRepository<PharmacyMovement, UUID> {
    List<PharmacyMovement> findByPharmacyStockIdAndTenantIdOrderByCreatedAtDesc(UUID pharmacyStockId, UUID tenantId);
    Page<PharmacyMovement> findByPharmacyStockIdAndTenantId(UUID pharmacyStockId, UUID tenantId, Pageable pageable);
    List<PharmacyMovement> findByPatientIdAndTenantIdOrderByCreatedAtDesc(UUID patientId, UUID tenantId);
    @Query("SELECT pm FROM PharmacyMovement pm WHERE pm.tenantId = :tenantId AND pm.deleted = false AND pm.createdAt BETWEEN :start AND :end")
    List<PharmacyMovement> findByDateRange(UUID tenantId, LocalDate start, LocalDate end);
    long countByPharmacyStockIdAndTenantId(UUID stockId, UUID tenantId);
}
