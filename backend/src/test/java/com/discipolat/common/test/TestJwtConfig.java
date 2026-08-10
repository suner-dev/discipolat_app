package com.discipolat.common.test;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Fournit un {@link JwtTokenProvider} RÉEL (clés RSA générées pour le test)
 * aux tests de contrôleurs {@code @WebMvcTest} importés via {@code @Import}.
 *
 * La chaîne de sécurité réelle ({@link com.discipolat.common.infrastructure.security.JwtAuthenticationFilter}
 * + {@code SecurityConfig}) peut alors signer et valider les tokens comme en
 * production, ce qui permet de tester les règles {@code @PreAuthorize}
 * (401/403) de bout en bout au niveau API.
 */
@TestConfiguration
public class TestJwtConfig {

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
}
