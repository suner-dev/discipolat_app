package com.discipolat.common.test;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.tenants.domain.AuthorizationService;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Configuration de test pour la chaîne de sécurité complète (@WebMvcTest + SecurityConfig).
 * Fournit :
 * - JwtTokenProvider réel (clés RSA générées pour le test)
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
        AuthorizationService mock = mock(AuthorizationService.class);
        when(mock.can(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(mock.can(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(mock.canView(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(mock.canCreate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(mock.canUpdate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(mock.canDelete(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(mock.getCurrentUserPermissions()).thenReturn(Set.of());
        when(mock.getUserPermissions(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(Set.of());
        when(mock.getUserPermissionsInScope(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(Set.of());
        when(mock.isPlatformSuperAdmin(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        return mock;
    }
}