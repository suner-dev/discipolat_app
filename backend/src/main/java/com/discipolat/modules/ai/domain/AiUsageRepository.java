package com.discipolat.modules.ai.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

    List<AiUsage> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Page<AiUsage> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    List<AiUsage> findByTenantIdAndUsageDateBetween(UUID tenantId, LocalDate from, LocalDate to);

    List<AiUsage> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);

    @Query("SELECT new Map(u.requestType as type, COUNT(u) as count, SUM(u.creditsConsumed) as credits) " +
           "FROM AiUsage u WHERE u.tenantId = :tenantId AND u.usageDate BETWEEN :from AND :to " +
           "GROUP BY u.requestType")
    List<Map<String, Object>> getUsageByType(@Param("tenantId") UUID tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT new Map(u.modelUsed as model, COUNT(u) as count, SUM(u.creditsConsumed) as credits) " +
           "FROM AiUsage u WHERE u.tenantId = :tenantId AND u.usageDate BETWEEN :from AND :to " +
           "GROUP BY u.modelUsed")
    List<Map<String, Object>> getUsageByModel(@Param("tenantId") UUID tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT SUM(u.creditsConsumed) FROM AiUsage u WHERE u.tenantId = :tenantId AND u.usageDate BETWEEN :from AND :to")
    Integer getTotalCreditsConsumed(@Param("tenantId") UUID tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT COALESCE(SUM(credits_consumed), 0) FROM ai_usage " +
            "WHERE tenant_id = :tenantId AND usage_date >= :from AND usage_date < :to", nativeQuery = true)
    long sumCreditsConsumedByTenantIdAndUsageDateGreaterThanEqualAndUsageDateLessThan(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT COUNT(u) FROM AiUsage u WHERE u.tenantId = :tenantId AND u.usageDate = :date")
    Long getDailyRequestCount(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    @Query("SELECT SUM(u.creditsConsumed) FROM AiUsage u WHERE u.tenantId = :tenantId AND u.usageDate = :date")
    Integer getDailyCreditsConsumed(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);
}