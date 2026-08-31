package com.discipolat.modules.network.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NetworkEventRepository extends JpaRepository<NetworkEvent, UUID> {

    /** Événements publics à venir. */
    List<NetworkEvent> findBySharedWithPublicTrueAndIsActiveTrueAndStartsAtAfterOrderByStartsAtAsc(LocalDateTime now);

    /** Tous les événements publics (y passés pour l'historique). */
    List<NetworkEvent> findBySharedWithPublicTrueAndIsActiveTrueOrderByStartsAtDesc();

    /** Événements d'une église spécifique. */
    List<NetworkEvent> findByTenantIdAndIsActiveTrueOrderByStartsAtDesc(UUID tenantId);

    /** Événements par type. */
    List<NetworkEvent> findBySharedWithPublicTrueAndEventTypeAndIsActiveTrueOrderByStartsAtAsc(String eventType);

    /** Événements par pays. */
    List<NetworkEvent> findBySharedWithPublicTrueAndCountryAndIsActiveTrueOrderByStartsAtAsc(String country);

    long countByTenantIdAndIsActiveTrue(UUID tenantId);
}
