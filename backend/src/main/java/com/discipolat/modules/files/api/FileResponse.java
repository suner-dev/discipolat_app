package com.discipolat.modules.files.api;

import com.discipolat.modules.files.domain.FileEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileResponse(
        UUID id,
        String nom,
        String typeFichier,
        Long taille,
        String chemin,
        String description,
        UUID familleId,
        UUID evenementId,
        UUID auteurId,
        String categorie,
        LocalDateTime createdAt
) {
    public static FileResponse from(FileEntity file) {
        return new FileResponse(
                file.getId(), file.getNom(), file.getTypeFichier(),
                file.getTaille(), file.getChemin(), file.getDescription(),
                file.getFamilleId(), file.getEvenementId(), file.getAuteurId(),
                file.getCategorie(), file.getCreatedAt());
    }
}
