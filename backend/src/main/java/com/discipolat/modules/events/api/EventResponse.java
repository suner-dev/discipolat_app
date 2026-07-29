package com.discipolat.modules.events.api;

import com.discipolat.modules.events.domain.Event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID organisateurId,
        UUID familleId,
        String typeEvenement,
        String titre,
        String description,
        String lieu,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        Integer limitePlaces,
        Integer nbInscrits,
        String statut,
        String compteRendu,
        LocalDateTime createdAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(), event.getOrganisateurId(), event.getFamilleId(),
                event.getTypeEvenement(), event.getTitre(), event.getDescription(),
                event.getLieu(), event.getDateDebut(), event.getDateFin(),
                event.getLimitePlaces(), event.getNbInscrits(), event.getStatut(),
                event.getCompteRendu(), event.getCreatedAt());
    }
}
