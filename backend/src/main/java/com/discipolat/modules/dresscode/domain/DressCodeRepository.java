package com.discipolat.modules.dresscode.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DressCodeRepository extends JpaRepository<DressCode, UUID> {

    List<DressCode> findByTenantIdAndArchivedFalse(UUID tenantId);

    List<DressCode> findByTenantIdAndSpaceIdAndArchivedFalse(UUID tenantId, UUID spaceId);

    List<DressCode> findByTenantIdAndEventIdAndArchivedFalse(UUID tenantId, UUID eventId);

    /**
     * PORT Develop1 (§G3.4 archives) : l'instantané d'événement est un objet
     * HISTORIQUE — il doit inclure les tenues déjà archivées pour cet événement,
     * sous peine d'archive trouée. Complémentaire de la méthode ArchivedFalse
     * ci-dessus, qui reste la vue courante du produit.
     */
    List<DressCode> findByTenantIdAndEventId(UUID tenantId, UUID eventId);

    /**
     * PORT Develop1 (§G5.9 portail basse connexion) : prochaines tenues à venir,
     * triées chronologiquement, pour le SMS/WhatsApp récapitulatif.
     */
    List<DressCode> findByTenantIdAndArchivedFalseAndBeginsAtAfterOrderByBeginsAtAsc(
            UUID tenantId, Instant after);

    @Query("SELECT dc FROM DressCode dc WHERE dc.tenantId = :tenantId " +
           "AND dc.archived = false " +
           "AND (:spaceId IS NULL OR dc.spaceId = :spaceId) " +
           "AND (:eventId IS NULL OR dc.eventId = :eventId)")
    List<DressCode> findFiltered(@Param("tenantId") UUID tenantId,
                                 @Param("spaceId") UUID spaceId,
                                 @Param("eventId") UUID eventId);

    long countByTenantIdAndArchivedFalse(UUID tenantId);
}
