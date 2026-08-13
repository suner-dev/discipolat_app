package com.discipolat.modules.departments.api;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Création d'un nouveau membre directement depuis l'espace de gestion
 * du département (identité + informations ecclésiales).
 */
public record DepartmentCreateMemberRequest(
        @NotBlank String nom,
        String prenom,
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
        Integer niveauCroissance
) {}
