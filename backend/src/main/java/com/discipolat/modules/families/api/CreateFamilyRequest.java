package com.discipolat.modules.families.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateFamilyRequest(
        @NotBlank String nom,
        @NotNull UUID departementId,
        @NotNull UUID chefFamilleId
) {}
