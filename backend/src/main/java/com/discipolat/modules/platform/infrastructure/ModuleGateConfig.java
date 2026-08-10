package com.discipolat.modules.platform.infrastructure;

import com.discipolat.modules.platform.domain.PlatformModuleRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Enregistre le garde-fou des modules désactivés AVANT la chaîne de sécurité
 * (précédence maximale), afin de rejeter au plus tôt les appels vers les API
 * de modules désactivés — y compris sans jeton valide.
 */
@Configuration
public class ModuleGateConfig {

    @Bean
    public FilterRegistrationBean<ModuleGateFilter> moduleGateFilter(PlatformModuleRepository moduleRepository) {
        FilterRegistrationBean<ModuleGateFilter> registration =
                new FilterRegistrationBean<>(new ModuleGateFilter(moduleRepository));
        registration.addUrlPatterns("/api/v1/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
