package com.discipolat.modules.tenants.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT t FROM Tenant t WHERE t.status = 'ACTIVE'")
    java.util.List<Tenant> findAllActive();

    long countByStatus(TenantStatus status);

    @Query("SELECT t FROM Tenant t WHERE t.createdAt > :date")
    long countByCreatedAtAfter(Instant date);

    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :term, '%')) " +
           "OR LOWER(t.slug) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<Tenant> findBySearchTerm(String term, Pageable pageable);

    long count();
}
