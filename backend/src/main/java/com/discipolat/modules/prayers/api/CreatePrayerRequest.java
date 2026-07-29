package com.discipolat.modules.prayers.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreatePrayerRequest(
        @NotBlank String titre,
        String description,
        UUID familleId,
        UUID ameId,
        @NotBlank @Pattern(regexp = "SANTE|FAMILLE|TRAVAIL|SPIRITUEL|AUTRE") String categorie,
        @Pattern(regexp = "BASSE|MOYENNE|HAUTE") String priorite,
        @Pattern(regexp = "PRIVEE|PARTAGEE") String visibilite
) {}
