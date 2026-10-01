package com.discipolat.modules.publicapi;

import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.domain.TenantSettingsRepository;
import com.discipolat.modules.tenants.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * §G6.9 — l'annuaire public ne doit renvoyer QUE les églises ayant opté (query serveur
 * {@code findPublicDirectoryEntries} = toggle true + tenant ACTIVE) et exposer une fiche
 * STRICTEMENT marketing (aucun contact privé, membre, finance, pastorale).
 */
@ExtendWith(MockitoExtension.class)
class PublicDirectoryControllerTest {

    @Mock
    private TenantSettingsRepository tenantSettingsRepository;

    @Test
    void exposesOnlyPublicMarketingFields() {
        Tenant tenant = Tenant.builder()
                .name("Église Central").slug("eglise-central").country("FR")
                .status(TenantStatus.ACTIVE).build();
        TenantSettings settings = TenantSettings.builder()
                .tenant(tenant).businessName("Central Church").slogan("Bienvenue")
                .description("Une communauté").logoUrl("https://cdn/logo.png")
                .coverUrl("https://cdn/cover.png").city("Paris")
                .email("privé@eglise.org").phone("+33600000000").address("1 rue secrète")
                .publicDirectoryEnabled(true).build();
        when(tenantSettingsRepository.findPublicDirectoryEntries()).thenReturn(List.of(settings));

        PublicDirectoryController controller = new PublicDirectoryController(tenantSettingsRepository);
        List<Map<String, Object>> cards = controller.list(null).getBody();

        assertEquals(1, cards.size());
        Map<String, Object> card = cards.get(0);
        assertEquals("eglise-central", card.get("slug"));
        assertEquals("Central Church", card.get("name"), "businessName prime sur le nom technique");
        assertEquals("Paris", card.get("city"));
        // Minimization : aucun champ sensible ne transite par la vitrine publique.
        assertFalse(card.containsKey("email"), "l'e-mail privé ne doit jamais être exposé");
        assertFalse(card.containsKey("phone"), "le téléphone privé ne doit jamais être exposé");
        assertFalse(card.containsKey("address"), "l'adresse ne doit jamais être exposée");
        assertTrue(card.size() <= 9, "fiche volontairement minimale");
    }

    @Test
    void emptyWhenNoChurchHasOptedIn() {
        when(tenantSettingsRepository.findPublicDirectoryEntries()).thenReturn(List.of());
        PublicDirectoryController controller = new PublicDirectoryController(tenantSettingsRepository);
        ResponseEntity<List<Map<String, Object>>> res = controller.list(null);
        assertTrue(res.getBody() != null && res.getBody().isEmpty());
    }

    @Test
    void filtersByCountryWhenProvided() {
        Tenant fr = Tenant.builder().name("FR").slug("fr").country("FR").status(TenantStatus.ACTIVE).build();
        Tenant us = Tenant.builder().name("US").slug("us").country("US").status(TenantStatus.ACTIVE).build();
        TenantSettings frS = TenantSettings.builder().tenant(fr).publicDirectoryEnabled(true).build();
        TenantSettings usS = TenantSettings.builder().tenant(us).publicDirectoryEnabled(true).build();
        when(tenantSettingsRepository.findPublicDirectoryEntries()).thenReturn(List.of(frS, usS));

        PublicDirectoryController controller = new PublicDirectoryController(tenantSettingsRepository);
        List<Map<String, Object>> cards = controller.list("fr").getBody();
        assertEquals(1, cards.size());
        assertEquals("fr", cards.get(0).get("slug"));
    }
}
