package com.discipolat.modules.onboarding.api;

import java.time.Instant;

/**
 * Contrat figé §3.1 — état d'achèvement de l'onboarding au niveau du TENANT.
 *
 * <p>Alimenté par les deux colonnes additives {@code tenants.onboarding_completed_at}
 * et {@code tenants.onboarding_completed_by} (migration V183, décision D2) et non
 * par un nouvel état de {@code TenantStatus} : ajouter un état
 * {@code ONBOARDING} aurait cassé {@code TenantStatus}, les seeds, les tableaux de
 * bord et une dizaine de tests existants.
 *
 * @param completed      l'onboarding du tenant est-il terminé ?
 * @param completedAt    fin de l'onboarding (ISO-8601 UTC), null sinon
 * @param completedBy    acteur ayant terminé l'onboarding, null sinon
 * @param totalSteps     nombre total d'étapes (7)
 * @param completedSteps étapes {@code COMPLETED}
 * @param skippedSteps   étapes {@code SKIPPED}
 * @param percentage     progression en pourcentage entier (0-100)
 */
public record OnboardingStatusResponse(
        boolean completed,
        Instant completedAt,
        String completedBy,
        int totalSteps,
        long completedSteps,
        long skippedSteps,
        int percentage) {
}
