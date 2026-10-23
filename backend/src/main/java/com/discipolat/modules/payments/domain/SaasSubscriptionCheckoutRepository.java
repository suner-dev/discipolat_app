package com.discipolat.modules.payments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux passages en caisse d'abonnement SaaS par redirection externe.
 * TODO_TARIFS_PAIEMENTS_INTERNATIONAL — continuité T6.
 */
@Repository
public interface SaasSubscriptionCheckoutRepository extends JpaRepository<SaasSubscriptionCheckout, UUID> {

    /** Rapprochement webhook : la transaction CPS impose sa référence locale. */
    Optional<SaasSubscriptionCheckout> findByProviderReference(String providerReference);

    Optional<SaasSubscriptionCheckout> findByPaymentIntentId(UUID paymentIntentId);

    List<SaasSubscriptionCheckout> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<SaasSubscriptionCheckout> findFirstByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, SubscriptionCheckoutStatus status);
}
