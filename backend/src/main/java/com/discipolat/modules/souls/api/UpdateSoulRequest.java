package com.discipolat.modules.souls.api;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateSoulRequest(
        String nom,
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
        Integer niveauCroissance,
        String notesPasteur
) {}
