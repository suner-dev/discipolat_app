package com.discipolat.common.test;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.security.domain.TokenRevocationService;
import com.discipolat.modules.tenants.domain.AuthzSecurityBean;
import com.discipolat.modules.tenants.domain.AuthorizationService;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Configuration de test STRICTE pour {@code TenantControllerSecurityTest}.
 *
 * <p>Identique à {@link TestSecurityConfig} (qui laisse passer ADMIN pour ne pas
 * casser les tests de contrôleur existants), mais le bean {@code authz}
 * n'accepte QUE l'autorité {@code PLATFORM_SUPER_ADMIN} : c'est ce qui permet
 * d'écrire le test de régression « un admin d'église ne doit pas accéder à
 * l'API des tenants ».
 */
@TestConfiguration
public class StrictPlatformAuthzConfig {

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

        String privateKeyBase64 = Base64.getEncoder()
                .encodeToString(privateKeyPem.getBytes(StandardCharsets.UTF_8));
        String publicKeyBase64 = Base64.getEncoder()
                .encodeToString(publicKeyPem.getBytes(StandardCharsets.UTF_8));

        return new JwtTokenProvider(privateKeyBase64, publicKeyBase64, "", "");
    }

    @Bean
    public TokenRevocationService tokenRevocationService() {
        TokenRevocationService service = mock(TokenRevocationService.class);
        when(service.isRevoked(anyString())).thenReturn(false);
        return service;
    }

    /** STRICT : seul PLATFORM_SUPER_ADMIN est super admin de la plateforme. */
    @Bean("authz")
    @Primary
    public AuthzSecurityBean authzSecurityBean() {
        AuthzSecurityBean bean = mock(AuthzSecurityBean.class);
        when(bean.isPlatformSuperAdmin()).thenAnswer(invocation -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null && auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.endsWith("PLATFORM_SUPER_ADMIN"));
        });
        return bean;
    }

    @Bean
    @Primary
    public AuthorizationService authorizationService() {
        AuthorizationService mock = mock(AuthorizationService.class);
        when(mock.getCurrentUserPermissions()).thenReturn(Set.of());
        when(mock.getUserPermissions(any(), any())).thenReturn(Set.of());
        when(mock.getUserPermissionsInScope(any(), any(), any(MembershipScopeType.class), any()))
                .thenReturn(Set.of());
        return mock;
    }
}
