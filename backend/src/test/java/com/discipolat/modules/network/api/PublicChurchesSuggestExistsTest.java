package com.discipolat.modules.network.api;

import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.infrastructure.config.RateLimitResult;
import com.discipolat.modules.network.domain.NetworkDirectory;
import com.discipolat.modules.network.domain.NetworkDirectoryRepository;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.domain.TenantSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LOT 1 §GLISE-D'ABORD (T1.1) — les deux endpoints NEUFS du sélecteur d'église :
 * {@code GET /api/v1/public/churches/suggest} et {@code GET /api/v1/public/churches/exists}.
 *
 * <p>Garanties vérifiées (contrat §6 + risques R2/R3) :
 * <ul>
 *   <li><b>Projection liste blanche</b> : une suggestion ne porte QUE name/slug/city/country ;
 *       aucune PII, aucun tenant_id, aucun champ vitrine hors liste (denomination/website/
 *       description/slogan/logo/listedAt).</li>
 *   <li><b>Non-listée = invisible</b> (D2) : le contrôleur n'interroge que les finders
 *       {@code ...IsListedTrue...} ; un {@code isListed=false} ne saurait remonter.</li>
 *   <li><b>Anti-énumération</b> (R3) : {@code exists} répond STRICTEMENT identique pour
 *       « fantôme » et « existante non listée ».</li>
 *   <li><b>Rate-limit par IP</b> : 429 + {@code Retry-After} + {@code X-RateLimit-Remaining: 0}
 *       + {@code Cache-Control: no-store}, et AUCUN accès métier quand le quota est dépassé.</li>
 *   <li><b>Non-régression</b> (A4) : {@code GET /public/churches} (list()) garde son contrat
 *       {@code {total, content[]}} inchangé.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicChurchesSuggestExistsTest {

    @Mock private NetworkDirectoryRepository directoryRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private TenantSettingsRepository tenantSettingsRepository;
    @Mock private PerIpRateLimiter rateLimiter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PublicChurchesController controller = new PublicChurchesController(
                directoryRepository, tenantRepository, tenantSettingsRepository, rateLimiter);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private void allowSuggest() {
        when(rateLimiter.tryConsumeChurchSuggest(anyString())).thenReturn(new RateLimitResult(true, 29, 0));
    }

    private void allowExists() {
        when(rateLimiter.tryConsumeChurchExists(anyString())).thenReturn(new RateLimitResult(true, 11, 0));
    }

    private NetworkDirectory church(UUID tenantId, String name, String city, String country) {
        NetworkDirectory d = new NetworkDirectory();
        d.setTenantId(tenantId);
        d.setChurchName(name);
        d.setCity(city);
        d.setCountry(country);
        // Champs vitrine qui NE DOIVENT JAMAIS sortir de /suggest :
        d.setDenomination("Baptiste");
        d.setWebsite("https://secret.example.org");
        d.setDescription("description interne confidentielle");
        d.setIsListed(true);
        return d;
    }

    private Tenant tenant(UUID id, String slug) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setSlug(slug);
        return t;
    }

    // ================================================================== SUGGEST

    @Test
    @DisplayName("suggest — projection minimale : uniquement name/slug/city/country")
    void suggestProjectsOnlyWhitelistedFields() throws Exception {
        allowSuggest();
        UUID tid = UUID.randomUUID();
        when(directoryRepository.findByIsListedTrueAndChurchNameContainingIgnoreCaseOrderByChurchNameAsc("emman"))
                .thenReturn(List.of(church(tid, "Emmanuel Worship", "Douala", "CM")));
        when(tenantRepository.findAllById(any())).thenReturn(List.of(tenant(tid, "emmanuel-worship")));
        TenantSettings ts = new TenantSettings();
        ts.setTenant(tenant(tid, "emmanuel-worship"));
        ts.setSlogan("Un slogan marketing");
        ts.setLogoUrl("https://logo.example/x.png");
        when(tenantSettingsRepository.findAllForTenants(any())).thenReturn(List.of(ts));

        String body = mockMvc.perform(get("/api/v1/public/churches/suggest").param("q", "emman"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Emmanuel Worship"))
                .andExpect(jsonPath("$.items[0].slug").value("emmanuel-worship"))
                .andExpect(jsonPath("$.items[0].city").value("Douala"))
                .andExpect(jsonPath("$.items[0].country").value("CM"))
                .andReturn().getResponse().getContentAsString();

        // RIEN hors liste blanche — ni PII, ni vitrine hors scope, ni identifiant interne.
        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("tenantId").doesNotContain("tenant_id")
                .doesNotContain("denomination").doesNotContain("website")
                .doesNotContain("description").doesNotContain("slogan")
                .doesNotContain("logoUrl").doesNotContain("listedAt")
                .doesNotContain("confidentielle").doesNotContain("secret.example.org");
    }

    @Test
    @DisplayName("suggest — q de moins de 2 caractères : total 0, aucun appel métier")
    void suggestBelowMinLengthShortCircuits() throws Exception {
        allowSuggest();
        mockMvc.perform(get("/api/v1/public/churches/suggest").param("q", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isArray());
        verify(directoryRepository, never())
                .findByIsListedTrueAndChurchNameContainingIgnoreCaseOrderByChurchNameAsc(anyString());
    }

    @Test
    @DisplayName("suggest — top 10 au plus, et jamais de requête non-filtrée sur isListed")
    void suggestCapsAtTenAndUsesListedFilter() throws Exception {
        allowSuggest();
        List<NetworkDirectory> many = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            many.add(church(UUID.randomUUID(), "Eglise " + i, "Ville", "FR"));
        }
        when(directoryRepository.findByIsListedTrueAndChurchNameContainingIgnoreCaseOrderByChurchNameAsc(eq("glise")))
                .thenReturn(many);
        when(tenantRepository.findAllById(any())).thenReturn(List.of());
        when(tenantSettingsRepository.findAllForTenants(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/public/churches/suggest").param("q", "glise"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.items.length()").value(10));

        // Le sélecteur ne peut PAS voir une église non listée : seul le finder
        // filtré isListed=true est interrogé.
        verify(directoryRepository)
                .findByIsListedTrueAndChurchNameContainingIgnoreCaseOrderByChurchNameAsc("glise");
        verify(directoryRepository, never()).findAll();
    }

    @Test
    @DisplayName("suggest — quota IP dépassé : 429 + Retry-After + no-store, métier NON appelé")
    void suggestRateLimited() throws Exception {
        when(rateLimiter.tryConsumeChurchSuggest(anyString())).thenReturn(new RateLimitResult(false, 0, 42));
        mockMvc.perform(get("/api/v1/public/churches/suggest").param("q", "emman"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(jsonPath("$.total").value(0));
        verify(directoryRepository, never())
                .findByIsListedTrueAndChurchNameContainingIgnoreCaseOrderByChurchNameAsc(anyString());
    }

    // ================================================================== EXISTS

    @Test
    @DisplayName("exists — nom exact listé : found=true + slug + name")
    void existsFoundExactMatch() throws Exception {
        allowExists();
        UUID tid = UUID.randomUUID();
        when(directoryRepository.findTop10ByIsListedTrueAndChurchNameIgnoreCaseOrderByChurchNameAsc("Emmanuel Worship"))
                .thenReturn(List.of(church(tid, "Emmanuel Worship", "Douala", "CM")));
        when(tenantRepository.findAllById(any())).thenReturn(List.of(tenant(tid, "emmanuel-worship")));
        when(tenantSettingsRepository.findAllForTenants(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/public/churches/exists").param("q", "Emmanuel Worship"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.slug").value("emmanuel-worship"))
                .andExpect(jsonPath("$.name").value("Emmanuel Worship"));
    }

    @Test
    @DisplayName("exists — nom businessName prime sur le nom technique")
    void existsPrefersBusinessName() throws Exception {
        allowExists();
        UUID tid = UUID.randomUUID();
        when(directoryRepository.findTop10ByIsListedTrueAndChurchNameIgnoreCaseOrderByChurchNameAsc("tech"))
                .thenReturn(List.of(church(tid, "tech", "Douala", "CM")));
        when(tenantRepository.findAllById(any())).thenReturn(List.of(tenant(tid, "tech-slug")));
        TenantSettings ts = new TenantSettings();
        ts.setTenant(tenant(tid, "tech-slug"));
        ts.setBusinessName("Église de la Grâce");
        when(tenantSettingsRepository.findAllForTenants(any())).thenReturn(List.of(ts));

        mockMvc.perform(get("/api/v1/public/churches/exists").param("q", "tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.name").value("Église de la Grâce"));
    }

    @Test
    @DisplayName("exists (D2/R3) — « fantôme » et « existante non listée » donnent la MÊME réponse")
    void existsIsIndistinguishableForNotListedAndPhantom() throws Exception {
        allowExists();
        // Fantôme : aucun match. Non listée : le finder filtré isListed=true ne la renvoie pas.
        // Dans les deux cas le finder renvoie une liste vide — réponse STRICTEMENT identique.
        String phantom = mockMvc.perform(get("/api/v1/public/churches/exists").param("q", "INEXISTANTE 42"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String notListed = mockMvc.perform(get("/api/v1/public/churches/exists").param("q", "Eglise Pas Publique"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(phantom)
                .isEqualTo(notListed)
                .doesNotContain("slug")
                .doesNotContain("tenant");
        // Les deux → found:false.
        com.fasterxml.jackson.databind.JsonNode p = new com.fasterxml.jackson.databind.ObjectMapper().readTree(phantom);
        com.fasterxml.jackson.databind.JsonNode n = new com.fasterxml.jackson.databind.ObjectMapper().readTree(notListed);
        org.assertj.core.api.Assertions.assertThat(p.get("found").asBoolean()).isFalse();
        org.assertj.core.api.Assertions.assertThat(n.get("found").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("exists — q vide : found=false, aucun appel métier")
    void existsBlankQuery() throws Exception {
        allowExists();
        mockMvc.perform(get("/api/v1/public/churches/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false));
        verify(directoryRepository, never())
                .findTop10ByIsListedTrueAndChurchNameIgnoreCaseOrderByChurchNameAsc(anyString());
    }

    @Test
    @DisplayName("exists — quota IP dépassé : 429 + found=false, métier NON appelé")
    void existsRateLimited() throws Exception {
        when(rateLimiter.tryConsumeChurchExists(anyString())).thenReturn(new RateLimitResult(false, 0, 60));
        mockMvc.perform(get("/api/v1/public/churches/exists").param("q", "Emmanuel"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.found").value(false));
        verify(directoryRepository, never())
                .findTop10ByIsListedTrueAndChurchNameIgnoreCaseOrderByChurchNameAsc(anyString());
    }

    // ============================================================ NON-RÉGRESSION

    @Test
    @DisplayName("non-régression (A4) — list() garde son contrat {total, content[]} inchangé")
    void listContractUnchanged() throws Exception {
        UUID tid = UUID.randomUUID();
        when(directoryRepository.findByIsListedTrueAndChurchNameContainingIgnoreCaseOrderByChurchNameAsc("x"))
                .thenReturn(List.of(church(tid, "Croix du Nord", "Lille", "FR")));
        when(tenantRepository.findAllById(any())).thenReturn(List.of(tenant(tid, "croix-nord")));
        when(tenantSettingsRepository.findAllForTenants(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/public/churches").param("q", "x"))
                .andExpect(status().isOk())
                // list() renvoie toujours « content » (pas « items ») : contrat gelé.
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Croix du Nord"))
                .andExpect(jsonPath("$.content[0].slug").value("croix-nord"))
                .andExpect(jsonPath("$.items").doesNotExist());
    }
}
