package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Création / mise à jour d'une checklist de département.
 *
 * @param titre     titre de la checklist
 * @param cibleType GENERAL | TACHE | EVENEMENT | EQUIPE | MEMBRE
 * @param cibleId   identifiant de la cible (optionnel si GENERAL)
 * @param items     libellés des éléments de la checklist (création uniquement)
 * @param statut    OUVERTE | TERMINEE (mise à jour)
 */
public record DepartmentChecklistRequest(
        @NotBlank String titre,
        @NotNull String cibleType,
        UUID cibleId,
        List<String> items,
        String statut
) {}
