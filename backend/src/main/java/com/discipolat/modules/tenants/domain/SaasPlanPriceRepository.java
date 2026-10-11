package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.StripePriceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Catalogue des prix (TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T1.6).
 *
 * <p>Pas de filtre multi-tenant : les prix sont une donnée de PLATEFORME, comme
 * {@code saas_plans}. C'est le tenant qui s'abonne à un prix, pas l'inverse.</p>
 */
@Repository
public interface SaasPlanPriceRepository extends JpaRepository<SaasPlanPrice, UUID> {

    List<SaasPlanPrice> findByPlanKeyOrderBySortOrderAscCurrencyAsc(String planKey);

    List<SaasPlanPrice> findByPlanKeyIn(Collection<String> planKeys);

    Optional<SaasPlanPrice> findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(
            String planKey, String currency, BillingCycle cycle, String regionCode);

    /** Prix visibles d'une devise pour un cycle — base du catalogue public. */
    List<SaasPlanPrice> findByCurrencyAndBillingCycleAndIsEnabledTrueAndIsVisibleTrue(
            String currency, BillingCycle cycle);

    List<SaasPlanPrice> findByCurrencyAndBillingCycleAndIsEnabledTrueAndIsVisibleTrueAndRegionCode(
            String currency, BillingCycle cycle, String regionCode);

    /** Lignes dont le Price Stripe doit être créé ou rejoué. */
    List<SaasPlanPrice> findByStripePriceStatusIn(Collection<StripePriceStatus> statuses);

    boolean existsByPlanKey(String planKey);

    long countByPlanKey(String planKey);

    void deleteByPlanKey(String planKey);
}