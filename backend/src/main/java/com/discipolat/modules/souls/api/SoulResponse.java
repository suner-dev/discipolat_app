package com.discipolat.modules.souls.api;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.modules.souls.domain.Soul;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SoulResponse(
        UUID id,
        String nom,
        String prenom,
        String nomComplet,
        String email,
        String telephone,
        String adresse,
        LocalDate dateNaissance,
        String profession,
        TypeDisciple typeDisciple,
        LocalDate dateIntegration,
        LocalDate dateConversion,
        StatutAme statut,
        UUID faiseurId,
        UUID familleId,
        String situationFamiliale,
        String etatSpirituel,
        Integer niveauCroissance,
        String notesPasteur,
        LocalDateTime dateDernierContact,
        Double latitude,
        Double longitude,
        String zone,
        LocalDateTime createdAt
) {
    public static SoulResponse from(Soul soul) {
        return new SoulResponse(
                soul.getId(), soul.getNom(), soul.getPrenom(), soul.getNomComplet(),
                soul.getEmail(), soul.getTelephone(), soul.getAdresse(),
                soul.getDateNaissance(), soul.getProfession(),
                soul.getTypeDisciple(), soul.getDateIntegration(), soul.getDateConversion(),
                soul.getStatut(), soul.getFaiseurId(), soul.getFamilleId(),
                soul.getSituationFamiliale(), soul.getEtatSpirituel(), soul.getNiveauCroissance(),
                soul.getNotesPasteur(), soul.getDateDernierContact(),
                soul.getLatitude(), soul.getLongitude(), soul.getZone(),
                soul.getCreatedAt());
    }
}
