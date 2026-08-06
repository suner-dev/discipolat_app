package com.discipolat.modules.alerts.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Création manuelle d'une alerte par le pasteur, le responsable,
 * le chef de famille ou le faiseur.
 *
 * Cible : PERSONNE (ameId), DEPARTEMENT (departmentId),
 *         FAMILLE (familleId), GROUPE, EGLISE.
 */
public record CreateAlertRequest(
        @NotBlank String typeAlerteManuel,
        @NotBlank String titre,
        @NotBlank String message,
        @NotNull String cible,
        String priorite,
        UUID ameId,
        UUID faiseurId,
        UUID familleId,
        UUID departmentId
) {}
