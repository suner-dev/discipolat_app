package com.discipolat.modules.onboarding.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Point d'entrée en lecture de l'achèvement de l'onboarding au niveau du tenant.
 *
 * <p>Le module {@code onboarding} doit pouvoir répondre à {@code GET /status}
 * ({@code completed}, {@code completedAt}, {@code completedBy}) sans créer de
 * dépendance de bean circulaire avec le module {@code tenants} : le portail
 * implémente ce port, le wizard le consomme via un {@code ObjectProvider}
 * (absent avant la migration V183, ce qui est le comportement fail-closed
 * correct : onboarding non déclaré terminé).
 *
 * <p>Décision D2 : l'information provient de deux colonnes <b>additives</b>
 * ({@code tenants.onboarding_completed_at} / {@code tenants.onboarding_completed_by}),
 * jamais d'un nouvel état de {@code TenantStatus} — ajouter {@code ONBOARDING}
 * aurait cassé l'enum, les seeds, les tableaux de bord et une dizaine de tests.
 */
public interface TenantOnboardingStatusPort {

    /** Fin de l'onboarding du tenant, ou {@code null} s'il n'est pas terminé. */
    Instant completedAtOf(UUID tenantId);

    /** Acteur ayant terminé l'onboarding, ou {@code null}. */
    UUID completedByOf(UUID tenantId);
}
