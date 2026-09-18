package com.discipolat.security;

import com.discipolat.common.multitenancy.TenantAwareRedisManager;
import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TenantAwareRedisManager (G6.3 - §38).
 * Verifies that Redis keys are properly prefixed with tenant ID
 * to ensure isolation between tenants.
 */
@DisplayName("G6.3 — TenantAwareRedisManager Isolation")
class TenantAwareRedisManagerTest {

    @Test
    @DisplayName("buildKey should prefix with current tenant ID")
    void buildKeyShouldPrefixWithCurrentTenant() {
        UUID tenantId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        TenantContext.setTenantId(tenantId);
        
        try {
            TenantAwareRedisManager manager = new TenantAwareRedisManager(null); // redisTemplate not needed for key building
            
            String key = manager.buildKey("user", "123", "session");
            String expected = "tenant:" + tenantId.toString() + ":user:123:session";
            
            assertEquals(expected, key);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("buildKey should use 'system' when no tenant context")
    void buildKeyShouldUseSystemWhenNoTenant() {
        TenantContext.clear();
        
        TenantAwareRedisManager manager = new TenantAwareRedisManager(null);
        
        String key = manager.buildKey("global", "config");
        String expected = "tenant:system:global:config";
        
        assertEquals(expected, key);
    }

    @Test
    @DisplayName("buildKeyForTenant should use explicit tenant ID")
    void buildKeyForTenantShouldUseExplicitTenant() {
        UUID tenantId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        
        TenantAwareRedisManager manager = new TenantAwareRedisManager(null);
        
        String key = manager.buildKeyForTenant(tenantId, "user", "456", "cache");
        String expected = "tenant:" + tenantId.toString() + ":user:456:cache";
        
        assertEquals(expected, key);
    }

    @Test
    @DisplayName("buildKeyForTenant should use 'system' for null tenant")
    void buildKeyForTenantShouldUseSystemForNull() {
        TenantAwareRedisManager manager = new TenantAwareRedisManager(null);
        
        String key = manager.buildKeyForTenant(null, "system", "settings");
        String expected = "tenant:system:system:settings";
        
        assertEquals(expected, key);
    }

    @Test
    @DisplayName("getCurrentTenantPrefix should return correct prefix")
    void getCurrentTenantPrefixShouldReturnCorrectPrefix() {
        UUID tenantId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        TenantContext.setTenantId(tenantId);
        
        try {
            TenantAwareRedisManager manager = new TenantAwareRedisManager(null);
            
            String prefix = manager.getCurrentTenantPrefix();
            String expected = "tenant:" + tenantId.toString() + ":";
            
            assertEquals(expected, prefix);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Different tenants should get different prefixes")
    void differentTenantsShouldGetDifferentPrefixes() {
        UUID tenant1 = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        UUID tenant2 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        
        TenantAwareRedisManager manager = new TenantAwareRedisManager(null);
        
        TenantContext.setTenantId(tenant1);
        String prefix1 = manager.getCurrentTenantPrefix();
        
        TenantContext.setTenantId(tenant2);
        String prefix2 = manager.getCurrentTenantPrefix();
        
        assertNotEquals(prefix1, prefix2);
        assertTrue(prefix1.contains(tenant1.toString()));
        assertTrue(prefix2.contains(tenant2.toString()));
        
        TenantContext.clear();
    }
}