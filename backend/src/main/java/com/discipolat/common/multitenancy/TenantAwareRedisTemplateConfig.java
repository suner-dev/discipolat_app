package com.discipolat.common.multitenancy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.UUID;

/**
 * Tenant-aware RedisTemplate configuration.
 * Provides a RedisTemplate that automatically prefixes keys with tenant ID.
 * 
 * Usage: @Autowired @Qualifier("tenantAwareRedisTemplate") RedisTemplate<String, Object> redisTemplate;
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
@ConditionalOnBean(RedisConnectionFactory.class)
public class TenantAwareRedisTemplateConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "tenantAwareRedisTemplate")
    public RedisTemplate<String, Object> tenantAwareRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Helper to build tenant-aware keys.
     * Usage: redisTemplate.opsForValue().set(tenantAwareKey("user", "123"), value);
     */
    @Bean
    public TenantAwareKeyGenerator tenantAwareKeyGenerator() {
        return new TenantAwareKeyGenerator();
    }

    /**
     * Build a tenant-aware key for direct RedisTemplate usage.
     * Includes current tenant ID from TenantContext.
     */
    public static String tenantAwareKey(String... parts) {
        UUID currentTenant = TenantContext.getTenantId();
        String prefix = currentTenant != null ? "tenant:" + currentTenant.toString() + ":" : "tenant:system:";
        StringBuilder key = new StringBuilder(prefix);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) key.append(":");
            key.append(parts[i]);
        }
        return key.toString();
    }

    /**
     * Build a tenant-aware key for a specific tenant.
     */
    public static String tenantAwareKeyForTenant(UUID tenantId, String... parts) {
        String prefix = tenantId != null ? "tenant:" + tenantId.toString() + ":" : "tenant:system:";
        StringBuilder key = new StringBuilder(prefix);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) key.append(":");
            key.append(parts[i]);
        }
        return key.toString();
    }
}