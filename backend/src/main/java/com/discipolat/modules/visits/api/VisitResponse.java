package com.discipolat.modules.visits.api;

import com.discipolat.modules.visits.domain.Visit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        UUID soulId,
        String soulNom,
        UUID visiteurId,
        String visiteurNom,
        LocalDate datePrevue,
        LocalDate dateRealisee,
        Visit.StatutVisite statut,
        String motif,
        String objectif,
        String compteRendu,
        String photoUrl,
        Boolean present,
        LocalDateTime createdAt
) {
    public static VisitResponse from(Visit v, String soulNom, String visiteurNom) {
        return new VisitResponse(
                v.getId(), v.getSoulId(), soulNom, v.getVisiteurId(), visiteurNom,
                v.getDatePrevue(), v.getDateRealisee(), v.getStatut(), v.getMotif(),
                v.getObjectif(), v.getCompteRendu(), v.getPhotoUrl(), v.getPresent(),
                v.getCreatedAt());
    }
}
