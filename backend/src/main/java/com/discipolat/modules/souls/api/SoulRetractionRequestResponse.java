package com.discipolat.modules.souls.api;

import com.discipolat.modules.souls.domain.SoulRetractionRequest;

import java.time.LocalDateTime;
import java.util.UUID;

public record SoulRetractionRequestResponse(
        UUID id,
        UUID ameId,
        UUID demandeurId,
        String justification,
        String statut,
        UUID traitePar,
        LocalDateTime dateTraitement,
        String commentaireReponse,
        LocalDateTime createdAt
) {
    public static SoulRetractionRequestResponse from(SoulRetractionRequest request) {
        return new SoulRetractionRequestResponse(
                request.getId(), request.getAmeId(), request.getDemandeurId(),
                request.getJustification(), request.getStatut(), request.getTraitePar(),
                request.getDateTraitement(), request.getCommentaireReponse(), request.getCreatedAt());
    }
}
