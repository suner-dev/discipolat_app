package com.discipolat.modules.audit.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository("auditLogRepository")
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Recherche combinée : utilisateur, type d'entité, action et plage de dates.
     * Chaque critère est optionnel (null = pas de filtre). Le filtre par action
     * permet d'exploiter l'audit par type d'opération (création, modification,
     * suppression, transfert... → voir les actions « CREER_* », « MODIFIER_* »,
     * « SUPPRIMER_* », « TRANSFERT_* », etc.).
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:utilisateurId IS NULL OR a.utilisateurId = :utilisateurId)
              AND (:entiteType IS NULL OR a.entiteType = :entiteType)
              AND (:action IS NULL OR a.action = :action)
              AND (:debut IS NULL OR a.createdAt >= :debut)
              AND (:fin IS NULL OR a.createdAt <= :fin)
            """)
    Page<AuditLog> findFiltered(@Param("utilisateurId") UUID utilisateurId,
                                @Param("entiteType") String entiteType,
                                @Param("action") String action,
                                @Param("debut") LocalDateTime debut,
                                @Param("fin") LocalDateTime fin,
                                Pageable pageable);

    /**
     * Requête simplifiée pour les tendances d'audit : tous les logs depuis une date.
     * Pas de Pageable pour éviter les erreurs de cast sur PostgreSQL.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.createdAt >= :debut
            ORDER BY a.createdAt DESC
            """)
    List<AuditLog> findSince(@Param("debut") LocalDateTime debut);

    @Query(value = "SELECT * FROM audit_logs ORDER BY created_at DESC", nativeQuery = true)
    Page<AuditLog> findPlatformAll(Pageable pageable);

    long countByTenantIdAndCreatedAtGreaterThan(UUID tenantId, LocalDateTime debut);

    long countByCreatedAtGreaterThan(LocalDateTime debut);

    @Query(value = """
        SELECT
            COUNT(*) as total,
            COUNT(*) FILTER (WHERE created_at > NOW() - INTERVAL '7 days') as last_7_days,
            COUNT(*) FILTER (WHERE created_at > NOW() - INTERVAL '24 hours') as last_24_hours,
            COUNT(*) FILTER (WHERE created_at > NOW() - INTERVAL '1 hour') as last_hour
        FROM audit_logs
        """, nativeQuery = true)
    Map<String, Object> getPlatformActivityStats();

    @Query(value = """
        SELECT
            action,
            COUNT(*) as count
        FROM audit_logs
        WHERE created_at > NOW() - INTERVAL '7 days'
        GROUP BY action
        ORDER BY count DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Map<String, Object>> getTopActionsLast7Days();

    @Query(value = """
        SELECT
            entite_type,
            COUNT(*) as count
        FROM audit_logs
        WHERE created_at > NOW() - INTERVAL '7 days'
        GROUP BY entite_type
        ORDER BY count DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Map<String, Object>> getTopEntityTypesLast7Days();
}
