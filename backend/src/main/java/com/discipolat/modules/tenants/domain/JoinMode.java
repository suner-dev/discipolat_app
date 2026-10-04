package com.discipolat.modules.tenants.domain;

/**
 * SPEC_ONBOARDING_FLOWS (D2) — mode de rejointure d'un code.
 * OPEN : l'adhérent rejoint immédiatement ; APPROVAL : demande tracée
 * dans {@code tenant_join_requests}, à valider par un admin du tenant.
 */
public enum JoinMode {
    OPEN,
    APPROVAL
}
