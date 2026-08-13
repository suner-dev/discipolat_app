package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Création / modification d'un poste du département.
 */
public record DepartmentPositionRequest(
        @NotBlank String nom,
        String description,
        String competencesRequises
) {
}
