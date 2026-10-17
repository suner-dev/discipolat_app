package com.discipolat.modules.objectives.api;

import com.discipolat.modules.objectives.domain.Objective;
import com.discipolat.modules.objectives.domain.ObjectiveType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateObjectiveRequest(
        // V263 — clé de rôle en texte : nom du rôle socle (UserRole) OU clé
        // custom du catalogue servi du tenant ; validée côté service (P7a).
        @NotBlank @Size(max = 50) String role,
        @NotNull ObjectiveType type,
        @Min(1) int cible,
        @NotNull Objective.Periode periode
) {
}
