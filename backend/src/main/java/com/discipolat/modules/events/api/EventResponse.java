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
        String imageUrl,
        List<String> tags,
        Boolean publicEvent,
        Boolean requiresRegistration,
        Boolean checkinEnabled,
        java.util.UUID streamId,
        java.math.BigDecimal latitude,
        java.math.BigDecimal longitude,
        Integer geofenceRadiusMeters,
        LocalDateTime createdAt,
        List<EntityAttachmentService.AttachmentItem> piecesJointes
) {
    public static EventResponse from(Event event, List<EntityAttachmentService.AttachmentItem> piecesJointes) {
        // L'ordre suit EXACTEMENT la declaration du record : c'est le seul moyen
        // que le compilateur_signale une divergence de contrat.
        return new EventResponse(
                event.getId(),
                event.getOrganisateurId(),
                event.getFamilleId(),
                event.getDepartmentId(),
                event.getTypeEvenement(),
                event.getTitre(),
                event.getDescription(),
                event.getLieu(),
                event.getDateDebut(),
                event.getDateFin(),
                event.getLimitePlaces(),
                event.getNbInscrits(),
                event.getStatut(),
                event.getCompteRendu(),
                event.getImageUrl(),
                event.getTags() == null ? List.of() : List.of(event.getTags()),
                event.isPublicEvent(),
                Boolean.TRUE.equals(event.getRequiresRegistration()),
                Boolean.TRUE.equals(event.getCheckinEnabled()),
                event.getStreamId(),
                event.getLatitude() == null ? null : java.math.BigDecimal.valueOf(event.getLatitude()),
                event.getLongitude() == null ? null : java.math.BigDecimal.valueOf(event.getLongitude()),
                event.getGeofenceRadiusMeters(),
                event.getCreatedAt(),
                piecesJointes != null ? piecesJointes : List.of());
    }
}
