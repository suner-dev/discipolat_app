package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Génération d'un rapport de département (synthèse calculée sur les
 * données réelles) ou sauvegarde manuelle d'un bilan rédigé.
 *
 * @param type        HEBDOMADAIRE | MENSUEL | TRIMESTRIEL | ANNUEL | EVENEMENT |
 *                    INCIDENT | DISCIPLINE | ACTIVITE | EFFECTIF | ASSIDUITE | PERFORMANCE | SYNTHESE
 * @param titre       titre du rapport (optionnel — auto si absent)
 * @param periodeDebut début de période (optionnel — auto selon le type)
 * @param periodeFin  fin de période (optionnel — auto selon le type)
 * @param contenu     contenu pour une sauvegarde manuelle (ignoré en génération)
 * @param statut      BROUILLON | SOUMIS (défaut BROUILLON)
 */
public record DepartmentReportRequest(
        @NotBlank String type,
        String titre,
        LocalDate periodeDebut,
        LocalDate periodeFin,
        String contenu,
        String statut
) {}
