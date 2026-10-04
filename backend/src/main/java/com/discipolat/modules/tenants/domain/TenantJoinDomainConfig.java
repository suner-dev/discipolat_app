package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.authentication.domain.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SPEC_ONBOARDING_FLOWS — câble le pont fonctionnel entre le domaine tenants
 * et le domaine authentication : {@code TenantJoinService} doit pouvoir créer
 * un compte MEMBRE en attente d'activation sans dépendre de la classe complète
 * {@code AuthService} (lecture du graphe + testabilité au mock).
 */
@Configuration
public class TenantJoinDomainConfig {

    @Bean
    TenantJoinService.AuthServiceBridge authServiceBridge(AuthService authService) {
        return authService::registerInChurch;
    }
}
