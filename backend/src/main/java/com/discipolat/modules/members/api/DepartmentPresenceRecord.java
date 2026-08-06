package com.discipolat.modules.members.api;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Fiche de présence d'un membre d'un département pour une semaine donnée.
 * Utilisée par le responsable pour la saisie groupée des présences.
 */
public record DepartmentPresenceRecord(
        UUID soulId,
        UUID userId,
        String nom,
        String telephone,
        String statut,
        String familleNom,
        UUID familleId,
        LocalDate dateIntegration,
        boolean presenceSaisie,
        Boolean present,
        Map<String, Boolean> presences,
        String notes,
        String typeProgramme,
        String sousProgramme
) {}
