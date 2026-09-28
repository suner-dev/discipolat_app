package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.onboarding.domain.TenantOnboardingStatusPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Implémentation du port de lecture de l'achèvement de l'onboarding (V183).
 *
 * <p>La table {@code tenants} est GLOBALE et l'entité {@code Tenant} ne porte pas
 * le filtre Hibernate {@code tenantFilter} : la lecture n'est donc jamais
 * restreinte au tenant courant — ce qui est nécessaire pour que le super admin
 * plateforme puisse lire l'état d'onboarding d'un tenant arbitraire.
 */
@Component
public class TenantOnboardingStatusAdapter implements TenantOnboardingStatusPort {

    private final TenantRepository tenantRepository;

    public TenantOnboardingStatusAdapter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Instant completedAtOf(UUID tenantId) {
        if (tenantId == null) {
            return null;
        }
        return tenantRepository.findById(tenantId)
                .map(Tenant::getOnboardingCompletedAt)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID completedByOf(UUID tenantId) {
        if (tenantId == null) {
            return null;
        }
        return tenantRepository.findById(tenantId)
                .map(Tenant::getOnboardingCompletedBy)
                .orElse(null);
    }
}
