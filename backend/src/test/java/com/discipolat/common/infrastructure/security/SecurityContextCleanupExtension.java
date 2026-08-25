package com.discipolat.common.infrastructure.security;

import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extension JUnit 5 auto-détectée (ServiceLoader + junit-platform.properties)
 * qui purge le SecurityContext et le TenantContext après CHAQUE test.
 *
 * Sans cela, un test appelant {@code SecurityTestHelper.loginAs(...)} pollue les
 * tests suivants s'exécutant sur le même thread (ex : un test « sans token »
 * héritait de l'authentification du test précédent et recevait 201 au lieu de 401).
 */
public class SecurityContextCleanupExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) {
        SecurityTestHelper.logout();
        TenantContext.clear();
    }
}
