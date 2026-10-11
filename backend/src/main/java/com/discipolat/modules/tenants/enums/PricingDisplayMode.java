package com.discipolat.modules.tenants.enums;

/**
 * Mode d'affichage du catalogue de tarifs, piloté par le super-admin.
 *
 * <p>Le mode {@link #LIST_ALL} conserve le comportement historique de la page
 * publique, qui empilait tous les prix d'un plan. Les autres modes posent un seul
 * prix et laissent l'utilisateur Basculer (dans la limite de ce que le
 * super-admin a autorisé).</p>
 */
public enum PricingDisplayMode {

    /** Un prix affiché ; sélecteur de devise proposé si autorisé. */
    SINGLE,
    /** Sélecteur de devise toujours visible. */
    SWITCHER,
    /** Tous les prix du plan empilés (rendu historique de {@code PricingPage}). */
    LIST_ALL,
    /** Prix de la région détectée, repli sur le prix GLOBAL. */
    AUTO_REGION
}