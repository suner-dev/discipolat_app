package com.discipolat.modules.tenants.domain;

import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accès à la ligne unique de réglages de tarification
 * (TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T1.6).
 */
@Repository
public interface SaasPlanPricingSettingsRepository
        extends org.springframework.data.jpa.repository.JpaRepository<SaasPlanPricingSettings, Short> {

    /** La table est un singleton : la ligne 1 est la seule lisible. */
    default Optional<SaasPlanPricingSettings> findSingleton() {
        return findById(SaasPlanPricingSettings.SINGLETON_ID);
    }
}