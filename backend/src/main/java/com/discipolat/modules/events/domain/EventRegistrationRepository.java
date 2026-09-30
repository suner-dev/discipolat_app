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
    long countByEventId(UUID eventId);
    long countByEventIdAndStatutInscription(UUID eventId, String statut);
    long countByUtilisateurIdAndStatutInscription(UUID userId, String statut);

    /**
     * nbInscrits du contrat §3 = compteur des inscriptions actives
     * (décision V202/D1 : la colonne nb_inscrits n'existe pas sur la table
     * vivante ; c'est event_registrations qui fait foi). Sémantique de
     * l'ancien compteur : INSCRIT/PRESENT compte, EN_ATTENTE (liste
     * d'attente) et ABSENT ne comptent pas.
     */
    long countByEventIdAndStatutInscriptionIn(UUID eventId, java.util.Collection<String> statuts);

    /** Inscriptions d'un utilisateur (événements auxquels il participe). */
    List<EventRegistration> findByUtilisateurId(UUID userId);
}
