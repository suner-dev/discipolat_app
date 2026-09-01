package com.discipolat.modules.payments.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, UUID> {

    Page<WebhookLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<WebhookLog> findByTenantIdAndProviderOrderByCreatedAtDesc(UUID tenantId, String provider, Pageable pageable);

    Page<WebhookLog> findByTenantIdAndStatusLabelOrderByCreatedAtDesc(UUID tenantId, String statusLabel, Pageable pageable);

    Page<WebhookLog> findByTenantIdAndProviderAndStatusLabelOrderByCreatedAtDesc(
            UUID tenantId, String provider, String statusLabel, Pageable pageable);

    @Query("SELECT w FROM WebhookLog w WHERE w.tenantId = :tenantId AND w.createdAt >= :since ORDER BY w.createdAt DESC")
    Page<WebhookLog> findByTenantIdAndSince(@Param("tenantId") UUID tenantId,
                                             @Param("since") LocalDateTime since,
                                             Pageable pageable);

    @Query("SELECT w FROM WebhookLog w WHERE w.tenantId = :tenantId " +
           "AND (:provider IS NULL OR w.provider = :provider) " +
           "AND (:status IS NULL OR w.statusLabel = :status) " +
           "AND w.createdAt >= :since ORDER BY w.createdAt DESC")
    Page<WebhookLog> search(@Param("tenantId") UUID tenantId,
                             @Param("provider") String provider,
                             @Param("status") String status,
                             @Param("since") LocalDateTime since,
                             Pageable pageable);

    long countByTenantIdAndStatusLabel(UUID tenantId, String statusLabel);

    @Query("SELECT w.provider, COUNT(w), " +
           "SUM(CASE WHEN w.statusLabel = 'PROCESSED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN w.statusLabel = 'REJECTED' THEN 1 ELSE 0 END) " +
           "FROM WebhookLog w WHERE w.tenantId = :tenantId AND w.createdAt >= :since " +
           "GROUP BY w.provider ORDER BY COUNT(w) DESC")
    Object[] statsByProvider(@Param("tenantId") UUID tenantId, @Param("since") LocalDateTime since);
}
