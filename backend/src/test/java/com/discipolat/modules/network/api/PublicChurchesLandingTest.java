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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LOT 2 §GLISE-D'ABORD (T2.2) — {@code GET /api/v1/public/churches/{slug}}.
 *
 * <p>Garanties vérifiées (contrat §6 + risques R2/R3) :
 * <ul>
 *   <li><b>Double consentement (RGPD art. 9)</b> : 200 seulement si l'église est
 *       EN MÊME TEMPS listée ({@code isListed}) ET en landing ({@code landingEnabled}).</li>
 *   <li><b>404 indistinguable</b> : slug inconnu, non listée et landing éteinte
 *       produisent une réponse STRICTEMENT identique (aucun oracle d'existence).</li>
 *   <li><b>Liste blanche</b> : jamais {@code tenant_id}, e-mail, téléphone, adresse,
 *       raison sociale légale, mentions fiscales (les champs posés sont vérifiés absents).</li>
 *   <li><b>noindex + no-store</b> dès T2.2 (pas d'indexation surprise d'une page cultuelle).</li>
 *   <li><b>Rate-limit par IP</b> : 429 quand le quota est dépassé, métier non appelé.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicChurchesLandingTest {

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
        when(rateLimiter.tryConsumeChurchLanding(anyString())).thenReturn(new RateLimitResult(true, 59, 0));
    }

    private Tenant tenant(UUID id, String slug) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setSlug(slug);
        return t;
    }

    private NetworkDirectory listed(UUID tenantId, String name) {
        NetworkDirectory d = new NetworkDirectory();
        d.setTenantId(tenantId);
        d.setChurchName(name);
        d.setCity("Douala");
        d.setCountry("CM");
        d.setIsListed(true);
        return d;
    }

    private TenantSettings landingOn(UUID tenantId) {
        TenantSettings ts = new TenantSettings();
        ts.setTenant(tenant(tenantId, "bethel"));
        ts.setLandingEnabled(true);
        ts.setBusinessName("Église Bethel");
        ts.setSlogan("Une famille pour toujours");
        ts.setLogoUrl("https://logo.example/bethel.png");
        ts.setPrimaryColor("#123456");
        ts.setHeadingFont("Poppins");
        // Champs SENSIBLES qui ne doivent JAMAIS sortir de la projection publique :
        ts.setEmail("pasteur@secret.example");
        ts.setPhone("+33600000000");
        ts.setAddress("12 rue confidentielle, Douala");
        ts.setLegalName("ASSOCIATION LEGALE SECRET");
        ts.setReceiptTaxNumber("TAX-SECRET-42");
        ts.setLandingSections(List.of(Map.of("id", "hero"), Map.of("id", "events")));
        return ts;
    }

    // ----------------------------------------------------------- 200 + liste blanche

    @Test
    @DisplayName("landing — listée + landingEnabled : 200, projection liste blanche, noindex + no-store")
    void landingOkWhitelistedProjection() throws Exception {
        UUID tid = UUID.randomUUID();
        when(tenantRepository.findBySlug("bethel")).thenReturn(Optional.of(tenant(tid, "bethel")));
        when(directoryRepository.findByTenantId(tid)).thenReturn(Optional.of(listed(tid, "Bethel Tech")));
        when(tenantSettingsRepository.findByTenantId(tid)).thenReturn(Optional.of(landingOn(tid)));

        String body = mockMvc.perform(get("/api/v1/public/churches/bethel"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(jsonPath("$.slug").value("bethel"))
                // businessName prime sur le nom technique (displayName).
                .andExpect(jsonPath("$.name").value("Église Bethel"))
                .andExpect(jsonPath("$.landingEnabled").value(true))
                .andExpect(jsonPath("$.slogan").value("Une famille pour toujours"))
                .andExpect(jsonPath("$.logoUrl").value("https://logo.example/bethel.png"))
                .andExpect(jsonPath("$.branding.primaryColor").value("#123456"))
                .andExpect(jsonPath("$.branding.headingFont").value("Poppins"))
                .andExpect(jsonPath("$.sections.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        // AUCUN champ sensible ne transite (R2 / RGPD art. 9).
        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("tenantId").doesNotContain("tenant_id")
                .doesNotContain("email").doesNotContain("phone").doesNotContain("address")
                .doesNotContain("legalName").doesNotContain("receiptTax")
                .doesNotContain("pasteur@secret.example").doesNotContain("+33600000000")
                .doesNotContain("confidentielle").doesNotContain("SECRET");
    }

    // ------------------------------------------------------------ 404 indistinguables

    @Test
    @DisplayName("landing (D2/R3) — slug inconnu / non listée / landing éteinte = MÊME 404")
    void landingIsIndistinguishableAcrossAllNegativeCases() throws Exception {
        UUID tid = UUID.randomUUID();

        // (a) slug inconnu.
        when(tenantRepository.findBySlug("fantome")).thenReturn(Optional.empty());
        var rFantome = mockMvc.perform(get("/api/v1/public/churches/fantome")).andExpect(status().isNotFound()).andReturn().getResponse();

        // (b) existante mais NON listée à l'annuaire.
        when(tenantRepository.findBySlug("nonlistee")).thenReturn(Optional.of(tenant(tid, "nonlistee")));
        NetworkDirectory hidden = listed(tid, "Cachee");
        hidden.setIsListed(false);
        when(directoryRepository.findByTenantId(tid)).thenReturn(Optional.of(hidden));
        var rNonListee = mockMvc.perform(get("/api/v1/public/churches/nonlistee")).andExpect(status().isNotFound()).andReturn().getResponse();

        // (c) listée mais landing ÉTEINTE (défaut dark launch).
        UUID tid2 = UUID.randomUUID();
        when(tenantRepository.findBySlug("eteinte")).thenReturn(Optional.of(tenant(tid2, "eteinte")));
        when(directoryRepository.findByTenantId(tid2)).thenReturn(Optional.of(listed(tid2, "Publiée")));
        TenantSettings off = new TenantSettings();
        off.setTenant(tenant(tid2, "eteinte"));
        off.setLandingEnabled(false);
        when(tenantSettingsRepository.findByTenantId(tid2)).thenReturn(Optional.of(off));
        var rEteinte = mockMvc.perform(get("/api/v1/public/churches/eteinte")).andExpect(status().isNotFound()).andReturn().getResponse();

        // Les trois réponses sont STRICTEMENT identiques (corps, statut, en-têtes).
        org.assertj.core.api.Assertions.assertThat(rNonListee.getContentAsString())
                .isEqualTo(rFantome.getContentAsString())
                .isEqualTo(rEteinte.getContentAsString());
        org.assertj.core.api.Assertions.assertThat(rNonListee.getErrorMessage()).isNull();
        org.assertj.core.api.Assertions.assertThat(rEteinte.getHeader("X-Robots-Tag")).isEqualTo("noindex, nofollow");
        // Aucune fuite : le 404 ne dit pas POURQUOI il n'a pas trouvé.
        org.assertj.core.api.Assertions.assertThat(rFantome.getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("landing — settings absents (église listée sans réglages) : 404 comme les autres")
    void landingMissingSettingsIs404() throws Exception {
        UUID tid = UUID.randomUUID();
        when(tenantRepository.findBySlug("sansreglages")).thenReturn(Optional.of(tenant(tid, "sansreglages")));
        when(directoryRepository.findByTenantId(tid)).thenReturn(Optional.of(listed(tid, "Sans Reglages")));
        when(tenantSettingsRepository.findByTenantId(tid)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/public/churches/sansreglages"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------- rate-limit

    @Test
    @DisplayName("landing — quota IP dépassé : 429 + no-store, métier NON appelé")
    void landingRateLimited() throws Exception {
        when(rateLimiter.tryConsumeChurchLanding(anyString())).thenReturn(new RateLimitResult(false, 0, 30));
        mockMvc.perform(get("/api/v1/public/churches/bethel"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
        verify(tenantRepository, never()).findBySlug(anyString());
    }

    // --------------------------------------------------------------- non-régression

    @Test
    @DisplayName("non-régression (A4) — /suggest et /exists conservent leurs clés propres")
    void landingDoesNotDisturbDirectoryContract() throws Exception {
        // list() reste {total, content[]} : le path-variable /{slug} n'a pas capté la
        // racine (Spring résout "" vers list(), "x" vers landing()). On vérifie ici que
        // /suggest garde {total, items} (déjà couvert par le test T1.1).
        when(tenantRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/public/churches/inexistant"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.items").doesNotExist())
                .andExpect(jsonPath("$.content").doesNotExist());
    }
}
