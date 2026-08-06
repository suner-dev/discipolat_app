package com.discipolat.modules.families.api;

import com.discipolat.common.enums.StatutEntite;
import com.discipolat.modules.families.domain.Family;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record FamilyResponse(
        UUID id,
        String nom,
        UUID userId,
        UUID chefFamilleId,
        String chefFamilleNom,
        UUID chefAdjointId,
        String chefAdjointNom,
        LocalDate dateCreation,
        StatutEntite statut,
        Double latitude,
        Double longitude,
        String zone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FamilyResponse from(Family family) {
        return new FamilyResponse(
                family.getId(),
                family.getNom(),
                family.getUserId(),
                family.getChefFamilleId(),
                null, // chefFamilleNom will be resolved by service
                family.getChefAdjointId(),
                null, // chefAdjointNom will be resolved by service
                family.getDateCreation(),
                family.getStatut(),
                family.getLatitude(),
                family.getLongitude(),
                family.getZone(),
                family.getCreatedAt(),
                family.getUpdatedAt());
    }

    public static FamilyResponse from(Family family, String chefFamilleNom, String chefAdjointNom) {
        return new FamilyResponse(
                family.getId(),
                family.getNom(),
                family.getUserId(),
                family.getChefFamilleId(),
                chefFamilleNom,
                family.getChefAdjointId(),
                chefAdjointNom,
                family.getDateCreation(),
                family.getStatut(),
                family.getLatitude(),
                family.getLongitude(),
                family.getZone(),
                family.getCreatedAt(),
                family.getUpdatedAt());
    }
}
