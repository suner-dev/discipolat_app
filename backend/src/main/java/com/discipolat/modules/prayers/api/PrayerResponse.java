package com.discipolat.modules.prayers.api;

import com.discipolat.modules.prayers.domain.Prayer;

import java.time.LocalDateTime;
import java.util.UUID;

public record PrayerResponse(
        UUID id,
        UUID auteurId,
        UUID familleId,
        UUID ameId,
        String titre,
        String description,
        String categorie,
        String priorite,
        String statut,
        String temoignage,
        LocalDateTime dateExaucee,
        String visibilite,
        LocalDateTime createdAt
) {
    public static PrayerResponse from(Prayer prayer) {
        return new PrayerResponse(
                prayer.getId(), prayer.getAuteurId(), prayer.getFamilleId(),
                prayer.getAmeId(), prayer.getTitre(), prayer.getDescription(),
                prayer.getCategorie(), prayer.getPriorite(), prayer.getStatut(),
                prayer.getTemoignage(), prayer.getDateExaucee(),
                prayer.getVisibilite(), prayer.getCreatedAt());
    }
}
