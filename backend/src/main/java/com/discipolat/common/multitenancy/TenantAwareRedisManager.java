package com.discipolat.common.multitenancy;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * TenantAwareRedisManager — Préfixe toutes les cles Redis par le tenant ID (Section 38 du prompt).
 * 
 * Format: tenant:{tenantId}:user:{userId}:...
 * 
 * Cela garantit l'isolation des données Redis entre tenants.
 */
@Component
public class TenantAwareRedisManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "tenant:";

    public TenantAwareRedisManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Construit une clé Redis tenant-aware.
     * Utilise le tenant courant du ThreadLocal ou un tenantId explicite.
     */
    public String buildKey(String... parts) {
        UUID currentTenant = TenantContext.getTenantId();
        StringBuilder key = new StringBuilder(KEY_PREFIX);
        if (currentTenant != null) {
            key.append(currentTenant.toString());
        } else {
            key.append("system");
        }
        for (String part : parts) {
            key.append(":").append(part);
        }
        return key.toString();
    }

    /**
     * Construit une clé pour un tenant spécifique.
     */
    public String buildKeyForTenant(UUID tenantId, String... parts) {
        StringBuilder key = new StringBuilder(KEY_PREFIX);
        key.append(tenantId != null ? tenantId.toString() : "system");
        for (String part : parts) {
            key.append(":").append(part);
        }
        return key.toString();
    }

    /**
     * Stocke une valeur avec préfixe tenant automatique.
     */
    public void setValue(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(buildKey(key), value, timeout, unit);
    }

    /**
     * Récupère une valeur avec préfixe tenant automatique.
     */
    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(buildKey(key));
    }

    /**
     * Supprime une clé tenant-aware.
     */
    public void delete(String key) {
        redisTemplate.delete(buildKey(key));
    }

    /**
     * Supprime toutes les clés d'un tenant (lors de la suppression du tenant).
     */
    public void deleteAllForTenant(UUID tenantId) {
        String pattern = buildKeyForTenant(tenantId, "*");
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Vérifie si une clé existe pour le tenant courant.
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(key)));
    }

    /**
     * Obtient le RedisTemplate natif pour les opérations avancées.
     */
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    /**
     * Obtient le préfixe de clé pour le tenant courant.
     */
    public String getCurrentTenantPrefix() {
        UUID currentTenant = TenantContext.getTenantId();
        return KEY_PREFIX + (currentTenant != null ? currentTenant.toString() : "system") + ":";
    }
}
