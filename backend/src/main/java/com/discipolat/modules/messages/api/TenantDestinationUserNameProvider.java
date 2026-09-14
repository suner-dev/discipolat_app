package com.discipolat.modules.messages.api;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tenant-aware destination user name provider.
 * Extracts tenant from Principal and includes it in destination resolution.
 */
@Component
class TenantDestinationUserNameProvider implements DestinationUserNameProvider {

    @Override
    public String getDestinationUserName() {
        // Extract tenant from current security context
        // The auth interceptor should have set tenant context
        UUID tenantId = TenantContext.getTenantId();
        String name = getCurrentUserName();
        
        if (tenantId != null && name != null) {
            return "tenant:" + tenantId + ":" + name;
        }
        return name;
    }

    private String getCurrentUserName() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getName() != null) {
                return authentication.getName();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}