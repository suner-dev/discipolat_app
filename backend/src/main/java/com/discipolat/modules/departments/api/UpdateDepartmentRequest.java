package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Mise à jour d'un département.
 * Seuls les champs modifiables sont acceptés (pas de création de responsable).
 */
public record UpdateDepartmentRequest(
        @NotBlank String nom,
        String description,
        UUID responsableId
) {}
