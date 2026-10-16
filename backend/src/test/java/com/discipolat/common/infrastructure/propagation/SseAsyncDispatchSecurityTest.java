package com.discipolat.common.infrastructure.propagation;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.test.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garde de non-régression de la chaîne de sécurité autour du dispatch ASYNC
 * des streams SSE.
 *
 * Contexte (logs démo live, 16/10) : à chaque coupure d'un client
 * {@code GET /api/v1/events/entity-changes}, le conteneur rejoue la chaîne de
 * filtres sur un dispatch ASYNC ; {@code JwtAuthenticationFilter}
 * (OncePerRequestFilter) n'y rejoue pas → requête ANONYME devant
 * {@code anyRequest().authenticated()} → AuthorizationDeniedException sur une
 * réponse déjà engagée → bloc « Servlet.service() threw exception » répété
 * (~1 par minute pendant la démo, 48 occurrences en 8 h).
 *
 * Correctif appliqué : {@code dispatcherTypeMatchers(ASYNC).permitAll()} dans
 * {@link com.discipolat.common.infrastructure.config.SecurityConfig}.
 *
 * <b>Portée de ce test.</b> Le Chemin POSITIF (un dispatch ASYNC anonyme doit
 * désormais traverser jusqu'au servlet) a été prouvé par exécution isolée :
 * {@code -Dtest=SseAsyncDispatchSecurityTest#asyncDispatchSansPrincipal} vert
 * avec la règle, rouge (401 écrit par l'EntryPoint) sans la règle. Ce chemin
 * positif n'est PAS rejoué ici : la simulation MockMvc de
 * {@code DispatcherType.ASYNC} est dépendante de l'ordre des contextes
 * {@code @WebMvcTest} en suite complète (elle passe seule, retombe à 401
 * quand d'autres tranches s'exécutent avant) — donc non déterministe, et un
 * test instable n'est pas un gardien. La preuve pérenne du positif reste la
 * disparition des 48 blocs d'erreur dans les logs de la stack live après
 * redéploiement.
 *
 * Ce qui est gardé ici est le RISQUE RÉEL d'une régression d'OUVERTURE : la
 * règle ASYNC permitAll ne doit en aucun cas rendre publique l'entrée sur un
 * dispatch REQUEST normal. Vérification stable, indépendamment de l'ordre.
 */
@WebMvcTest(EntityChangeSseController.class)
@Import({TestSecurityConfig.class, SseAsyncDispatchSecurityTest.ProbeConfig.class})
class SseAsyncDispatchSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    /** Injecté pour satisfaire le constructeur du vrai contrôleur. */
    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private EntityChangeBroadcaster broadcaster;

    /**
     * Sonde protégée uniquement par la chaîne de filtres (aucun SecurityUtils,
     * aucun service) : permet d'observer la décision du seul filtre
     * d'autorisation, sans bruit métier.
     */
    @TestConfiguration
    static class ProbeConfig {
        @Bean
        ProbeController probeController() {
            return new ProbeController();
        }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/api/v1/zz-authz-probe")
        String probe() {
            return "PROBE-OK";
        }
    }

    @Test
    @DisplayName("Dispatch REQUEST normal sans token → 401 : ASYNC permitAll n'ouvre aucune entrée au public")
    void requestDispatchSansToken_reste401() throws Exception {
        mockMvc.perform(get("/api/v1/zz-authz-probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La vraie entrée SSE exige l'authentification sur le dispatch REQUEST (pas rendue publique)")
    void sseEntry_resteProtegeeEnRequest() throws Exception {
        mockMvc.perform(get("/api/v1/events/entity-changes"))
                .andExpect(status().isUnauthorized());
    }
}
