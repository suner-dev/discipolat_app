package com.discipolat.modules.tenants.domain;

import java.util.UUID;

/**
 * Événement applicatif publié à chaque changement de statut d'un tenant.
 *
 * <p>Écoute par {@link TenantStatusGuard} pour vider immédiatement son cache de
 * statut, ce qui rend une suspension/réactivation effective sans attendre
 * l'expiration du TTL de 30 secondes.
 *
 * <p>Ce n'est PAS un événement Spring {@code ApplicationEvent} : c'est un record
 * simple, transporté par {@code ApplicationEventPublisher}. Aucun listener
 * applicatif tiers n'est requis.
 *
 * @param tenantId      tenant concerné (jamais null)
 * @param previousStatus statut avant le changement
 * @param newStatus      statut après le changement
 */
public record TenantStatusChangedEvent(
        UUID tenantId,
        TenantStatus previousStatus,
        TenantStatus newStatus) {

    public TenantStatusChangedEvent {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId cannot be null");
        }
    }
}
