package com.discipolat.modules.discipleship.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscipleProgressRepository extends JpaRepository<DiscipleProgress, Long> {

    Page<DiscipleProgress> findByTenantId(UUID tenantId, Pageable pageable);

    Page<DiscipleProgress> findByTenantIdAndJourneyId(UUID tenantId, Long journeyId, Pageable pageable);

    Page<DiscipleProgress> findByTenantIdAndDiscipleId(UUID tenantId, UUID discipleId, Pageable pageable);

    Page<DiscipleProgress> findByTenantIdAndStatus(UUID tenantId, DiscipleProgress.ProgressStatus status, Pageable pageable);

    Optional<DiscipleProgress> findByTenantIdAndId(UUID tenantId, Long id);

    Optional<DiscipleProgress> findByTenantIdAndDiscipleIdAndJourneyId(UUID tenantId, UUID discipleId, Long journeyId);

    long countByTenantIdAndJourneyId(UUID tenantId, Long journeyId);

    long countByTenantIdAndJourneyIdAndStatus(UUID tenantId, Long journeyId, DiscipleProgress.ProgressStatus status);

    /**
     * Filtrage combiné (parcours ET/OU disciple ET/OU statut) en base.
     *
     * <p>Chaque critère est facultatif : {@code null} = non filtré. Cela évite
     * d'exploser en 2^n requêtes dérivées et garantit que la PAGINATION est
     * correcte (le filtrage se fait avant le découpage en pages, pas après).
     */
    @Query("""
            SELECT p FROM DiscipleProgress p
            WHERE p.tenantId = :tenantId
              AND (:journeyId IS NULL OR p.journeyId = :journeyId)
              AND (:discipleId IS NULL OR p.discipleId = :discipleId)
              AND (:status IS NULL OR p.status = :status)
            """)
    Page<DiscipleProgress> findAll(@Param("tenantId") UUID tenantId,
                                   @Param("journeyId") Long journeyId,
                                   @Param("discipleId") UUID discipleId,
                                   @Param("status") DiscipleProgress.ProgressStatus status,
                                   Pageable pageable);

    /**
     * Répartition d'un disciple par étape atteinte — alimente
     * {@code stageDistribution} du rapport de parcours (§2.1).
     */
    @Query("""
            SELECT p.currentStageId, COUNT(p)
            FROM DiscipleProgress p
            WHERE p.tenantId = :tenantId AND p.journeyId = :journeyId
            GROUP BY p.currentStageId
            """)
    List<Object[]> countByCurrentStage(@Param("tenantId") UUID tenantId,
                                       @Param("journeyId") Long journeyId);
}
