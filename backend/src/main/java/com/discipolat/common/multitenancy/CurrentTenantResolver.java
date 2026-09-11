package com.discipolat.common.multitenancy;

import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Centralized tenant resolution logic.
 * Determines the current tenant from authenticated user's memberships.
 * If user has multiple memberships, requires explicit context selection.
 */
@Component
public class CurrentTenantResolver {

    private static final Logger log = LoggerFactory.getLogger(CurrentTenantResolver.class);

    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public CurrentTenantResolver(TenantMembershipRepository membershipRepository, UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    /**
     * Resolve tenant for the current authenticated user.
     * Priority:
     * 1. Explicit tenant context already set (ThreadLocal)
     * 2. User has single active membership -> use that tenant
     * 3. User has multiple memberships -> require explicit selection (return null)
     * 4. Fallback to default tenant (for system jobs)
     */
    public UUID resolveTenantId() {
        // 1. Already set in context
        UUID existing = TenantContext.getTenantId();
        if (existing != null) {
            return existing;
        }

        // 2. Get current user
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return TenantContext.DEFAULT_TENANT_ID;
        }

        // 3. Find active memberships
        List<TenantMembership> memberships = membershipRepository.findActiveByUserId(userId, TenantMembership.MembershipStatus.ACTIVE);

        if (memberships.isEmpty()) {
            log.warn("User {} has no active tenant memberships", userId);
            return TenantContext.DEFAULT_TENANT_ID;
        }

        if (memberships.size() == 1) {
            UUID tenantId = memberships.get(0).getTenantId();
            log.debug("Auto-selected tenant {} for user {} (single membership)", tenantId, userId);
            return tenantId;
        }

        // 4. Multiple memberships - require explicit selection
        log.debug("User {} has {} active memberships, explicit selection required", userId, memberships.size());
        return null; // Caller must handle multi-tenant selection
    }

    /**
     * Validate that user has access to the given tenant
     */
    public boolean validateTenantAccess(UUID userId, UUID tenantId) {
        return membershipRepository.existsByUserIdAndTenantIdAndStatus(userId, tenantId, TenantMembership.MembershipStatus.ACTIVE);
    }

    /**
     * Get all accessible tenants for a user
     */
    public List<TenantMembership> getUserTenants(UUID userId) {
        return membershipRepository.findByUserIdAndStatus(userId, TenantMembership.MembershipStatus.ACTIVE);
    }

    /**
     * Switch tenant context for current user (validates access first)
     */
    public boolean switchTenant(UUID userId, UUID newTenantId) {
        if (!validateTenantAccess(userId, newTenantId)) {
            log.warn("User {} attempted to switch to unauthorized tenant {}", userId, newTenantId);
            return false;
        }
        TenantContext.setTenantId(newTenantId);
        return true;
    }

    private UUID getCurrentUserId() {
        // Extract from SecurityContext or request
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                return user.getId();
            }
            if (authentication != null && authentication.getName() != null) {
                // Try to find user by email
                return userRepository.findByEmail(authentication.getName()).map(User::getId).orElse(null);
            }
        } catch (Exception e) {
            log.debug("Could not extract user from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}