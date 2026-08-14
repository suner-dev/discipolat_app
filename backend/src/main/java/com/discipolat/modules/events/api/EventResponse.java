package com.discipolat.modules.events.api;

import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.files.domain.EntityAttachmentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID organisateurId,
        UUID familleId,
        UUID departmentId,
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
        LocalDateTime createdAt,
        List<EntityAttachmentService.AttachmentItem> piecesJointes
) {
    public static EventResponse from(Event event, List<EntityAttachmentService.AttachmentItem> piecesJointes) {
        return new EventResponse(
                event.getId(), event.getOrganisateurId(), event.getFamilleId(), event.getDepartmentId(),
                event.getTypeEvenement(), event.getTitre(), event.getDescription(),
                event.getLieu(), event.getDateDebut(), event.getDateFin(),
                event.getLimitePlaces(), event.getNbInscrits(), event.getStatut(),
                event.getCompteRendu(), event.getCreatedAt(),
                piecesJointes != null ? piecesJointes : List.of());
    }
}
