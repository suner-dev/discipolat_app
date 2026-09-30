package com.discipolat.modules.events.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {
    List<EventRegistration> findByEventId(UUID eventId);
    Optional<EventRegistration> findByEventIdAndUtilisateurId(UUID eventId, UUID utilisateurId);
    long countByEventIdAndStatutInscription(UUID eventId, String statut);
    long countByUtilisateurIdAndStatutInscription(UUID userId, String statut);

    /**
     * Nombre d'inscriptions portees par un evenement, tous statuts confondus.
     *
     * <p>C'est la definition de {@code nbInscrits} : une inscription est une
     * ligne de {@code event_registrations}, que la personne soit venue ou non.
     * Le tableau {@code event_registrations} ne porte pas de suppression
     * logique : une inscription disparue est une ligne effacee, donc le compte
     * se recompose seul.
     */
    long countByEventId(UUID eventId);

    /** Inscriptions d'un utilisateur (événements auxquels il participe). */
    List<EventRegistration> findByUtilisateurId(UUID userId);

    /**
     * Une ligne de {@link #countByEventIds(Collection)} : evenement + nombre
     * d'inscriptions.
     */
    record EventCount(UUID eventId, long total) {
    }

    /**
     * Nombre d'inscriptions par evenement, en UNE requete.
     *
     * <p>La source de verite de {@code nbInscrits} : la table vivante {@code event}
     * ne porte pas de colonne de compteur (V203), parce qu'une colonne
     * desynchronisee vaut moins qu'un calcul. Compter depuis le service
     * declencherait une requete par ligne d'ecran.
     *
     * <p>Ne renvoie que les evenements ayant au moins une inscription : l'appelant
     * traite l'absence comme un zero.
     */
    @Query("""
            SELECT r.eventId AS eventId, COUNT(r) AS total
            FROM EventRegistration r
            WHERE r.eventId IN :eventIds
            GROUP BY r.eventId
            """)
    List<EventCount> countByEventIds(@Param("eventIds") Collection<UUID> eventIds);
}
