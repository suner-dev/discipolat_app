package com.discipolat.modules.families.api;

import com.discipolat.common.enums.StatutEntite;
import com.discipolat.modules.families.domain.Family;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record FamilyResponse(
        UUID id,
        String nom,
        UUID departementId,
        UUID chefFamilleId,
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
                family.getId(), family.getNom(), family.getDepartementId(),
                family.getChefFamilleId(), family.getDateCreation(),
                family.getStatut(),
                family.getLatitude(), family.getLongitude(), family.getZone(),
                family.getCreatedAt(), family.getUpdatedAt());
    }
}
