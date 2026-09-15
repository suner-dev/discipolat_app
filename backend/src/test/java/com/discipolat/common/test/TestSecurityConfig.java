package com.discipolat.common.test;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.tenants.domain.AuthorizationService;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

/**
 * Configuration de test pour la chaîne de sécurité complète (@WebMvcTest + SecurityConfig).
 * Fournit :
 * - JwtTokenProvider réel (clés RSA générées)
 * - AuthorizationService mock (autorise tout pour les tests de contrôleur)
 */
@TestConfiguration
@Import(SecurityConfig.class)
public class TestSecurityConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        String privDerB64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(pair.getPrivate().getEncoded());
        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + privDerB64 + "\n-----END PRIVATE KEY-----";

        String pubDerB64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(pair.getPublic().getEncoded());
        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + pubDerB64 + "\n-----END PUBLIC KEY-----";

        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKeyPem.getBytes(StandardCharsets.UTF_8));
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyPem.getBytes(StandardCharsets.UTF_8));

        return new JwtTokenProvider(privateKeyBase64, publicKeyBase64, "", "");
    }

    @Bean
    @Primary
    public AuthorizationService authorizationService() {
        return new AuthorizationService() {
            @Override
            public boolean can(String permissionKey, MembershipScopeType scopeType, UUID scopeId) {
                return true;
            }

            @Override
            public boolean can(UUID userId, UUID tenantId, String permissionKey, MembershipScopeType scopeType, UUID scopeId) {
                return true;
            }

            @Override
            public boolean canView(UUID userId, UUID tenantId, String resourceType, UUID resourceId) {
                return true;
            }

            @Override
            public boolean canCreate(UUID userId, UUID tenantId, String resourceType, MembershipScopeType scopeType, UUID scopeId) {
                return true;
            }

            @Override
            public boolean canUpdate(UUID userId, UUID tenantId, String resourceType, UUID resourceId) {
                return true;
            }

            @Override
            public boolean canDelete(UUID userId, UUID tenantId, String resourceType, UUID resourceId) {
                return true;
            }

            @Override
            public void require(String permissionKey, MembershipScopeType scopeType, UUID scopeId) {
            }

            @Override
            public void requireCurrentUser(String permissionKey, MembershipScopeType scopeType, UUID scopeId) {
            }

            @Override
            public Set<String> getCurrentUserPermissions() {
                return Set.of();
            }

            @Override
            public Set<String> getUserPermissions(UUID userId, UUID tenantId) {
                return Set.of();
            }

            @Override
            public Set<String> getUserPermissionsInScope(UUID userId, UUID tenantId, MembershipScopeType scopeType, UUID scopeId) {
                return Set.of();
            }

            @Override
            public boolean isPlatformSuperAdmin(UUID userId) {
                return true;
            }
        };
    }
}