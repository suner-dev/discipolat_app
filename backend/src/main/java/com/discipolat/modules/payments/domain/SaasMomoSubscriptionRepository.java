package com.discipolat.modules.payments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour les abonnements SaaS via Mobile Money.
 * TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T6.3.
 */
@Repository
public interface SaasMomoSubscriptionRepository extends JpaRepository<SaasMomoSubscription, UUID> {

    Optional<SaasMomoSubscription> findByTenantId(UUID tenantId);

    Optional<SaasMomoSubscription> findByTenantIdAndStatusIn(UUID tenantId, List<MomoSubscriptionStatus> statuses);

    List<SaasMomoSubscription> findByStatus(MomoSubscriptionStatus status);

    /**
     * Abonnements actifs dont la prochaine échéance est due.
     * Utilisé par le scheduler de renouvellement.
     */
    List<SaasMomoSubscription> findByStatusAndNextChargeAtBefore(MomoSubscriptionStatus status, java.time.Instant before);

    Optional<SaasMomoSubscription> findByProviderReference(String providerReference);
}