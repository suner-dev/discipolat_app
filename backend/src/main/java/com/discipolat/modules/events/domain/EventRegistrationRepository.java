package com.discipolat.modules.events.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {
    List<EventRegistration> findByEventId(UUID eventId);
    Optional<EventRegistration> findByEventIdAndUtilisateurId(UUID eventId, UUID utilisateurId);
    long countByEventIdAndStatutInscription(UUID eventId, String statut);
    long countByUtilisateurIdAndStatutInscription(UUID userId, String statut);
}
