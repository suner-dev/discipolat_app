package com.discipolat.modules.map.api;

import java.util.UUID;

/**
 * Point affiché sur la carte : disciple (soul) ou famille (family).
 */
public record MapPointResponse(
        UUID id,
        String type,          // SOUL | FAMILY
        String nom,
        Double latitude,
        Double longitude,
        String zone,
        String statut,
        String familleNom,
        String departementNom,
        Integer niveauCroissance
) {
}
