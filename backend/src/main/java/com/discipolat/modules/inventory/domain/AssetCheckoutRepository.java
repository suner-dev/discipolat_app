package com.discipolat.modules.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetCheckoutRepository extends JpaRepository<AssetCheckout, UUID> {

    List<AssetCheckout> findByTenantIdAndItemId(UUID tenantId, UUID itemId);

    List<AssetCheckout> findByTenantIdAndMemberId(UUID tenantId, UUID memberId);

    List<AssetCheckout> findByTenantIdAndStatus(UUID tenantId, String status);

    Optional<AssetCheckout> findFirstByTenantIdAndItemIdAndStatus(UUID tenantId, UUID itemId, String status);

    @Query("SELECT ac FROM AssetCheckout ac WHERE ac.tenantId = :tenantId " +
           "AND ac.status = 'CHECKED_OUT' AND ac.dueBackAt < CURRENT_TIMESTAMP")
    List<AssetCheckout> findOverdueCheckouts(@Param("tenantId") UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, String status);
}
