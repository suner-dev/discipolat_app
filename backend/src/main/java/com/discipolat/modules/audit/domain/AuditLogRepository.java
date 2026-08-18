package com.discipolat.modules.audit.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
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
              AND (:debut IS NULL OR a.createdAt >= cast(:debut as timestamp))
              AND (:fin IS NULL OR a.createdAt <= cast(:fin as timestamp))
            """)
    Page<AuditLog> findFiltered(@Param("utilisateurId") UUID utilisateurId,
                                @Param("entiteType") String entiteType,
                                @Param("action") String action,
                                @Param("debut") LocalDateTime debut,
                                @Param("fin") LocalDateTime fin,
                                Pageable pageable);
}
