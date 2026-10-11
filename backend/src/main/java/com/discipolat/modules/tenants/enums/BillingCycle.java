package com.discipolat.modules.tenants.enums;

import java.util.Locale;

/**
 * Cycle de facturation d'un prix de catalogue.
 *
 * <p>Introduit avec le catalogue multi-devises (V245) pour lever l'ambiguïté de
 * {@code saas_plans.billing_period}, qui était consommé à la fois comme cycle de
 * référence du plan et comme période d'affichage côté client
 * (cf. {@code PricingPage.tsx}). Ici le cycle est porté par le PRIX, donc un
 * même plan peut avoir un mensuel et un annuel dans des devises différentes.</p>
 */
public enum BillingCycle {

    MONTHLY,
    YEARLY;

    /** Tolérant : "monthly", "MONTHLY", "month", "mensuel" comme "YEARLY", "annual". */
    public static BillingCycle fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return MONTHLY;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "YEARLY", "YEAR", "ANNUAL", "ANNUEL", "AN" -> YEARLY;
            default -> MONTHLY;
        };
    }

    public boolean isYearly() {
        return this == YEARLY;
    }
}