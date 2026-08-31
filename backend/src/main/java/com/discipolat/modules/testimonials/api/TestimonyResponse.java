package com.discipolat.modules.testimonials.api;

import com.discipolat.modules.testimonials.domain.Testimony;

import java.time.LocalDateTime;
import java.util.UUID;

public record TestimonyResponse(
        UUID id,
        String titre,
        String contenu,
        String categorie,
        String statut,
        UUID auteurId,
        String auteurNom,
        int likes,
        int commentaires,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TestimonyResponse from(Testimony t, String auteurNom) {
        return new TestimonyResponse(
                t.getId(),
                t.getTitre(),
                t.getContenu(),
                t.getCategorie().name(),
                t.getStatut().name(),
                t.getAuteurId(),
                auteurNom,
                t.getLikes(),
                t.getCommentaires(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
