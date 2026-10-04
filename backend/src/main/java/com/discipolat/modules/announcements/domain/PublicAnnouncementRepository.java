package com.discipolat.modules.announcements.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS — la lecture publique et la modération traversent les
 * tenants : les requêtes « cross » sont exécutées sous filtre suspendu
 * (CrossTenantScopeAccess) côté service, jamais ici par magic.
 */
public interface PublicAnnouncementRepository extends JpaRepository<PublicAnnouncement, UUID> {

    List<PublicAnnouncement> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Query("SELECT a FROM PublicAnnouncement a WHERE a.status = :status "
            + "AND (a.expiresAt IS NULL OR a.expiresAt > :now) "
            + "AND (a.eventAt IS NULL OR a.eventAt > :now) "
            + "ORDER BY COALESCE(a.eventAt, a.publishedAt) DESC")
    List<PublicAnnouncement> findVisible(@Param("status") AnnouncementStatus status,
                                         @Param("now") Instant now,
                                         Pageable pageable);

    Page<PublicAnnouncement> findByStatusOrderByCreatedAtDesc(AnnouncementStatus status, Pageable pageable);

    List<PublicAnnouncement> findByStatusOrderByCreatedAtDesc(AnnouncementStatus status);

    @Query("SELECT a FROM PublicAnnouncement a WHERE a.status = :status "
            + "AND ((a.expiresAt IS NOT NULL AND a.expiresAt <= :now) "
            + "  OR (a.eventAt IS NOT NULL AND a.eventAt <= :now))")
    List<PublicAnnouncement> findStale(@Param("status") AnnouncementStatus status,
                                       @Param("now") Instant now);
}
