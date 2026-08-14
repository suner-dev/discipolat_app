package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Création / mise à jour d'un objectif de progression pour un membre
 * du département.
 */
public record DepartmentObjectiveRequest(
        @NotBlank String titre,
        String description,
        LocalDate echeance,
        @Min(0) @Max(100) Integer avancement,
        String statut
) {}
