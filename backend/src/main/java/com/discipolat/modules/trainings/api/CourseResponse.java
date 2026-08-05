package com.discipolat.modules.trainings.api;

import com.discipolat.modules.trainings.domain.Course;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String titre,
        String description,
        String categorie,
        Course.Niveau niveau,
        Integer dureeMinutes,
        UUID formateurId,
        String formateurNom,
        String imageUrl,
        boolean actif,
        long nbModules,
        long nbInscrits,
        LocalDateTime createdAt
) {
    public static CourseResponse from(Course c, String formateurNom, long nbModules, long nbInscrits) {
        return new CourseResponse(
                c.getId(), c.getTitre(), c.getDescription(), c.getCategorie(), c.getNiveau(),
                c.getDureeMinutes(), c.getFormateurId(), formateurNom, c.getImageUrl(), c.isActif(),
                nbModules, nbInscrits, c.getCreatedAt());
    }
}
