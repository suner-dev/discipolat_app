package com.discipolat.modules.souls.api;

import com.discipolat.common.enums.TypeDisciple;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateSoulRequest(
        @NotBlank String nom,
        String prenom,
        String email,
        String telephone,
        String adresse,
        LocalDate dateNaissance,
        String profession,
        @NotNull TypeDisciple typeDisciple,
        LocalDate dateIntegration,
        LocalDate dateConversion,
        // Optionnel : si absent, le faiseur courant est utilisé (SoulService.create).
        // Gardé optionnel pour que les rôles terrain (FAISEUR, CHEF_DE_FAMILLE…)
        // créent une âme sans connaître d'UUID par cœur.
        UUID faiseurId,
        UUID familleId,
        String situationFamiliale,
        String etatSpirituel,
        Integer niveauCroissance
) {}
