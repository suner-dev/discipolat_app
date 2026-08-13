package com.discipolat.modules.departments.api;

import com.discipolat.modules.departments.domain.DepartmentTeam;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Création / modification d'une équipe ou sous-département.
 * {@code parentId} permet la hiérarchie récursive (profondeur illimitée).
 */
public record DepartmentTeamRequest(
        @NotBlank String nom,
        UUID parentId,
        DepartmentTeam.TeamType type,
        UUID chefId,
        UUID adjointId,
        String objectif,
        String description,
        LocalDate dateDebut,
        LocalDate dateFin
) {
}
