package com.discipolat.modules.platform.api;

import com.discipolat.modules.platform.domain.PlatformFeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Constat A16 — la documentation OpenAPI publique doit correspondre
 * <b>exactement</b> aux endpoints réellement exposés.
 *
 * <p>Une documentation qui annonce une route inexistante, ou qui omet une route
 * livrée, est une documentation fausse : c'est précisément ce que le plan
 * reproche à `AGENT_ORCHESTRATION.md` (« aucune doc mensongère », règle R10).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicApiDocsControllerTest {

    @Mock
    private PlatformFeatureFlagService featureFlagService;

    private PublicApiDocsController controller;

    @BeforeEach
    void setUp() {
        when(featureFlagService.isEnabled(anyString())).thenReturn(false);
        controller = new PublicApiDocsController(featureFlagService);
    }

    @Test
    @DisplayName("Le catalogue des modules pointe vers le bon préfixe (/api/v1)")
    void modulesPointAtTheRealPrefix() {
        Map<String, Object> docs = controller.getPublicDocs().getBody();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) docs.get("modules");

        Map<String, Object> onboarding = modules.stream()
                .filter(module -> "Onboarding".equals(module.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("le module Onboarding doit etre liste"));

        // AVANT la correction A16, ce chemin etait `/api/onboarding-wizard`
        // (prefixe /v1 manquant) : la documentation pointait dans le vide.
        assertThat(onboarding.get("path")).isEqualTo("/api/v1/onboarding-wizard");
    }

    @Test
    @DisplayName("Tous les chemins du catalogue commencent par /api")
    void everyModulePathIsWellFormed() {
        Map<String, Object> docs = controller.getPublicDocs().getBody();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) docs.get("modules");

        assertThat(modules).isNotEmpty();
        assertThat(modules)
                .allSatisfy(module -> {
                    assertThat((String) module.get("path"))
                            .as("chemin du module %s", module.get("name"))
                            .startsWith("/api");
                    assertThat((String) module.get("description")).isNotBlank();
                });
    }

    @Test
    @DisplayName("L'OpenAPI publie les 7 endpoints du wizard + le suivi d'inscription")
    void openApiPublishesEveryWizardEndpoint() {
        String yaml = controller.getOpenApiYaml().getBody();

        assertThat(yaml)
                .contains("/onboarding-wizard:")
                .contains("/onboarding-wizard/progress:")
                .contains("/onboarding-wizard/status:")
                .contains("/onboarding-wizard/initialize:")
                .contains("/onboarding-wizard/{id}/start:")
                .contains("/onboarding-wizard/{id}/complete:")
                .contains("/onboarding-wizard/{id}/skip:")
                .contains("/onboarding-wizard/templates/{role}:")
                .contains("/auth/registration-status:");
    }

    @Test
    @DisplayName("Chaque route du wizard est documentee pour la methode HTTP reelle")
    void everyWizardRouteDeclaresItsVerb() {
        String yaml = controller.getOpenApiYaml().getBody();

        // Lectures en GET, mutations en POST : une route de mutation documentee
        // en GET induirait les clients en erreur.
        assertThat(yaml).contains("/onboarding-wizard/progress:\n    get:");
        assertThat(yaml).contains("/onboarding-wizard/status:\n    get:");
        assertThat(yaml).contains("/onboarding-wizard/initialize:\n    post:");
        assertThat(yaml).contains("/onboarding-wizard/{id}/start:\n    post:");
        assertThat(yaml).contains("/onboarding-wizard/{id}/complete:\n    post:");
        assertThat(yaml).contains("/onboarding-wizard/{id}/skip:\n    post:");
    }

    @Test
    @DisplayName("L'OpenAPI est bien du YAML servi en texte")
    void openApiIsServedAsYaml() {
        var response = controller.getOpenApiYaml();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        // Sans `produces` explicite, Spring renvoyait `text/plain` et les
        // generateurs de SDK refusaient le document.
        assertThat(response.getHeaders().getFirst("Content-Type"))
                .isNotNull()
                .satisfies(contentType -> assertThat(contentType).contains("vnd.oai.openapi"));
        assertThat(response.getBody()).contains("openapi:");
    }
}
