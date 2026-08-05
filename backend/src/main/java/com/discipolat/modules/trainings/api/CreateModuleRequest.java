package com.discipolat.modules.trainings.api;

import jakarta.validation.constraints.NotBlank;

public record CreateModuleRequest(
        @NotBlank String titre,
        String contenu,
        String videoUrl,
        int ordre
) {
}
