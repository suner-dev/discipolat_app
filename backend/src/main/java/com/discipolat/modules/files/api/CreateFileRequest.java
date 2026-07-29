package com.discipolat.modules.files.api;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateFileRequest(
        @NotBlank String nom,
        @NotBlank String typeFichier,
        Long taille,
        @NotBlank String chemin,
        String description,
        UUID familleId,
        UUID evenementId,
        String categorie
) {}
