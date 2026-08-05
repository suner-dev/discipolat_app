package com.discipolat.modules.interactions.api;

import com.discipolat.modules.interactions.domain.Interaction;
import com.discipolat.modules.interactions.domain.InteractionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InteractionResponse(
        UUID id,
        UUID soulId,
        UUID auteurId,
        String auteurNom,
        InteractionType type,
        String canal,
        String objet,
        String contenu,
        LocalDateTime dateInteraction,
        UUID aFairePar,
        String aFaireParNom,
        LocalDateTime rappelLe,
        LocalDateTime createdAt
) {
    public static InteractionResponse from(Interaction i, String auteurNom, String aFaireParNom) {
        return new InteractionResponse(
                i.getId(), i.getSoulId(), i.getAuteurId(), auteurNom,
                i.getType(), i.getCanal(), i.getObjet(), i.getContenu(),
                i.getDateInteraction(), i.getAFairePar(), aFaireParNom, i.getRappelLe(),
                i.getCreatedAt()
        );
    }
}
