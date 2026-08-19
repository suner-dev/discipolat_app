package com.discipolat.modules.families.api;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Mise à jour d'une famille.
 * Seuls les champs modifiables sont acceptés (pas de création de chef).
 */
public record UpdateFamilyRequest(
        @NotBlank String nom,
        UUID chefFamilleId,
        UUID chefAdjointId
) {}
