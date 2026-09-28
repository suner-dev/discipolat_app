package com.discipolat.modules.tenants.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT t FROM Tenant t WHERE t.status = 'ACTIVE'")
    java.util.List<Tenant> findAllActive();

    long countByStatus(TenantStatus status);

    Optional<Tenant> findFirstByStatusOrderByCreatedAtAsc(TenantStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tenant t WHERE t.id = :id")
    Optional<Tenant> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.createdAt > :date")
    long countByCreatedAtAfter(Instant date);

    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :term, '%')) " +
           "OR LOWER(t.slug) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<Tenant> findBySearchTerm(String term, Pageable pageable);

    long count();

    @Query(value = """
        SELECT
            COUNT(*) as total,
            COUNT(*) FILTER (WHERE status = 'ACTIVE') as active,
            COUNT(*) FILTER (WHERE status = 'SUSPENDED') as suspended,
            COUNT(*) FILTER (WHERE status = 'CANCELLED') as cancelled,
            COUNT(*) FILTER (WHERE status = 'PENDING_SETUP') as pending_setup,
            COUNT(*) FILTER (WHERE created_at > NOW() - INTERVAL '30 days') as recent_30_days
        FROM tenants
        """, nativeQuery = true)
    Map<String, Object> getDashboardStats();

    @Query(value = """
        SELECT
            normalize_plan_key(plan) as plan,
            COUNT(*) as count
        FROM tenants
        GROUP BY normalize_plan_key(plan)
        """, nativeQuery = true)
    List<Map<String, Object>> getTenantsByPlan();

    @Query(value = """
        SELECT
            status,
            COUNT(*) as count
        FROM tenants
        GROUP BY status
        """, nativeQuery = true)
    List<Map<String, Object>> getTenantsByStatus();

    @Query(value = """
        SELECT t FROM Tenant t
        WHERE t.createdAt < :cursor
        ORDER BY t.createdAt DESC
        """)
    Page<Tenant> findByCursor(@Param("cursor") Instant cursor, Pageable pageable);
}
