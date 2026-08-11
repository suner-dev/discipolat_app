package com.discipolat.modules.platform.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint public des méta-données de la plateforme : nom, version,
 * environnement (dev/docker/beta/prod), mode bêta et visibilité des
 * comptes de démonstration.
 */
@RestController
public class PlatformMetaController {

    private final String appName;
    private final String version;
    private final String environment;
    private final boolean demoAccountsEnabled;

    public PlatformMetaController(
            @Value("${spring.application.name:discipolat-api}") String appName,
            @Value("${app.version:1.0.0}") String version,
            @Value("${app.environment:dev}") String environment,
            @Value("${app.beta-testing.demo-accounts-enabled:false}") boolean demoAccountsEnabled) {
        this.appName = appName;
        this.version = version;
        this.environment = environment;
        this.demoAccountsEnabled = demoAccountsEnabled;
    }

    @GetMapping("/api/v1/public/meta")
    public PlatformMetaResponse meta() {
        boolean betaMode = "beta".equalsIgnoreCase(environment);
        return new PlatformMetaResponse(appName, version, environment, betaMode, demoAccountsEnabled);
    }
}
