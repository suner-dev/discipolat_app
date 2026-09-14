package com.discipolat.modules.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssetMaintenanceRepository extends JpaRepository<AssetMaintenance, UUID> {

    List<AssetMaintenance> findByTenantIdAndItemId(UUID tenantId, UUID itemId);

    List<AssetMaintenance> findByTenantIdAndStatus(UUID tenantId, String status);

    List<AssetMaintenance> findByTenantIdAndScheduledForBetween(UUID tenantId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(am.cost) FROM AssetMaintenance am WHERE am.tenantId = :tenantId AND am.itemId = :itemId AND am.status = 'COMPLETED'")
    Double getTotalMaintenanceCost(@Param("tenantId") UUID tenantId, @Param("itemId") UUID itemId);

    @Query("SELECT SUM(am.cost) FROM AssetMaintenance am WHERE am.tenantId = :tenantId AND am.status = 'COMPLETED'")
    Double getTotalMaintenanceCostByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT am FROM AssetMaintenance am WHERE am.tenantId = :tenantId " +
           "AND am.status = 'SCHEDULED' AND am.scheduledFor < :before")
    List<AssetMaintenance> findUpcomingMaintenance(@Param("tenantId") UUID tenantId, @Param("before") LocalDateTime before);

    long countByTenantIdAndStatus(UUID tenantId, String status);
}
