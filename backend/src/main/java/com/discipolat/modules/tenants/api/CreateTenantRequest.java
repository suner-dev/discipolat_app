package com.discipolat.modules.tenants.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens") String slug,
        String plan,
        String country,
        String currency,
        String timezone,
        String locale,
        String brandingJson,
        String featuresJson,
        String settingsJson,
        // SPEC_ORGANISATION_DENOMINATION_V2 (D2) — nature de l'organisation.
        // Aucune valeur = CHURCH par défaut (comportement existant préservé).
        // Permet de créer une dénomination, association, organisation ou méga-association
        // directement à la création du tenant.
        String kind
) {}
