package com.discipolat.common.multitenancy;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.NonNull;

import java.util.UUID;

/**
 * KeyGenerator that prefixes all cache keys with the current tenant ID.
 * Ensures tenant isolation in shared caches.
 * 
 * Usage: @Cacheable(cacheNames = "users", keyGenerator = "tenantAwareKeyGenerator")
 */
public class TenantAwareKeyGenerator implements KeyGenerator {

    private static final String TENANT_PREFIX = "tenant:";
    private static final String SYSTEM_PREFIX = "tenant:system:";

    @Override
    @NonNull
    public Object generate(@NonNull Object target, @NonNull java.lang.reflect.Method method, @NonNull Object... params) {
        UUID currentTenant = TenantContext.getTenantId();
        String prefix = currentTenant != null ? TENANT_PREFIX + currentTenant.toString() + ":" : SYSTEM_PREFIX;
        
        // Build key from method name and parameters
        StringBuilder keyBuilder = new StringBuilder(prefix);
        keyBuilder.append(method.getName());
        
        if (params != null && params.length > 0) {
            keyBuilder.append(":");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) keyBuilder.append(":");
                keyBuilder.append(params[i] != null ? params[i].toString() : "null");
            }
        }
        
        return keyBuilder.toString();
    }
}