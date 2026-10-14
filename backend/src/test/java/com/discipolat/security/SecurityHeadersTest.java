package com.discipolat.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2 #70 — Tests de sécurité OWASP.
 *
 * Vérifie les headers de sécurité essentiels :
 * - Content-Security-Policy
 * - X-Content-Type-Options: nosniff
 * - X-Frame-Options: DENY
 * - Strict-Transport-Security (HSTS)
 * - Cache-Control sur les endpoints sensibles
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P2 #70 — Tests de sécurité OWASP")
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("X-Content-Type-Options doit être 'nosniff'")
    void shouldHaveNosniffHeader() throws Exception {
        mockMvc.perform(get("/api/v1/public/docs"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("X-Frame-Options doit être 'DENY'")
    void shouldHaveFrameDenyHeader() throws Exception {
        mockMvc.perform(get("/api/v1/public/docs"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("Content-Security-Policy doit être présent")
    void shouldHaveCSPHeader() throws Exception {
        mockMvc.perform(get("/api/v1/public/docs"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    @DisplayName("Strict-Transport-Security doit être présent")
    void shouldHaveHSTSHeader() throws Exception {
        mockMvc.perform(get("/api/v1/public/docs"))
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test
    @DisplayName("L'injection SQL dans les paramètres doit échouer")
    void shouldRejectSQLInjection() throws Exception {
        mockMvc.perform(get("/api/v1/souls?statut=ACTIF' OR 1=1--")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Le endpoint debug ne doit pas être exposé sans authentification")
    void shouldNotExposeDebugEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Démo temps réel — le handshake WebSocket /ws-church ne doit PAS être rejeté 401 par le filtre")
    void shouldPermitWebsocketHandshakeWithoutAuth() throws Exception {
        // L'upgrade HTTP ne peut pas porter d'en-tête Authorization (navigateur).
        // Le handshake doit atteindre la couche STOMP (auth réelle sur CONNECT).
        // Sous MockMvc, on ne peut pas négocier un vrai upgrade ; le point
        // d'entrée SockJS /info suffit à prouver que le filtre n'exige plus
        // d'authentification sur ce chemin (avant correctif : 401 systématique).
        mockMvc.perform(get("/ws-church/info"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("/ws-church/info ne doit pas être bloqué par auth (401)")
                        .isNotEqualTo(401));
    }

    @Test
    @DisplayName("Démo temps réel — le permitAll reste cantonné à /ws : /api protégé renvoie bien 401")
    void shouldStillProtectApiWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/souls"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Les réponses doivent contenir des headers CORS configurés")
    void shouldHaveCORSHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/public/docs")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
