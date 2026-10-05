package com.discipolat.modules.tenants.api;

import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantDisputeRepository;
import com.discipolat.modules.tenants.domain.TenantGovernanceService;
import com.discipolat.modules.tenants.domain.TenantWarningRepository;
import com.discipolat.modules.tenants.domain.TenantKind;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantService;
import com.discipolat.modules.tenants.domain.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SPEC_ORGANISATION_DENOMINATION_V2 §7.1 / T-B2 — la CONSOLE PLATEFORME doit
 * exposer le modèle d'organisation.
 *
 * <p>Audit du 04/10/2026 : sur les cinq champs exigés par la tâche
 * ({@code kind}, {@code parentTenantId}, {@code rootTenantId},
 * {@code childCount}, {@code memberCount}), <b>zéro</b> n'était exposé par
 * {@code GET /platform/tenants} — alors que {@code countByRootTenantId}
 * existait déjà, écrit et inutilisé.
 *
 * <p>Ce test verrouille les deux propriétés qui comptent : les cinq champs
 * sont exacts, et <b>D7 — agrégats seulement</b> : aucun email, aucun nom de
 * membre ne franchit la frontière.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformTenantGovernanceControllerOrganizationTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private TenantWarningRepository warningRepository;
    @Mock private TenantDisputeRepository disputeRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    /**
     * `TenantService` est une classe concrète : même contrainte que pour le
     * service de gouvernance. Seules ses lectures sont utilisées ici
     * (`list()`), donc une doublure suffit et s'exécute sous tous les JDK.
     */
    private final TenantServiceStub tenantService = new TenantServiceStub();

    /**
     * `TenantGovernanceService` est une classe CONCRÈTE, et le mock inline de
     * Byte Buddy refuse de l'instrumenter au-delà des JDK qu'il supporte (le
     * runtime de référence est JDK 21 ; ici on tourne sous 26). Les tests de
     * cette classe n'exercent QUE la lecture réseau (`list`, `detail`), donc
     * une vraie instance est inutile et un mock serait inexécutable ailleurs.
     *
     * <p>On construit donc le contrôleur manuellement avec une instance réelle
     * du service de gouvernance, et on ne mocke que les INTERFACES
     * (repositories).
     */
    private PlatformTenantGovernanceController controller;

    @BeforeEach
    void setUp() {
        controller = new PlatformTenantGovernanceController(
                new TenantGovernanceService(
                        tenantRepository, warningRepository, disputeRepository,
                        // AuditService et EntityPropagationPublisher ne sont pas
                        // sollicités par les tests de lecture réseau : on passe
                        // `null` plutôt qu'un mock de classe concrète (contrainte
                        // Byte Buddy/JDK, cf. la note de conception du projet).
                        null, null, eventPublisher),
                tenantService, tenantRepository, membershipRepository);
    }

    /**
     * Doublure de {@link TenantService} : les tests de cette classe n'utilisent
     * que la LECTURE (`list()`), jamais la création. On fournit donc une liste
     *-réponse et on délègue le reste à une implémentation vide.
     *
     * <p>Convention du projet (cf. `FakeTenantService` dans
     * `TenantOrganizationServiceTest`) : on étend la classe et on passe des
     * `null` au constructeur — seule la méthode réellement observée est
     * redéfinie. Le mock inline de Byte Buddy ne peut pas faire le travail sur
     * cette classe sous les JDK que le projet ne supporte pas (référence : JDK 21).
     */
    private static final class TenantServiceStub extends TenantService {

        private List<TenantResponse> rows = List.of();

        TenantServiceStub() {
            super(null, null, null, null, null, null, null, null, null);
        }

        void withRows(List<TenantResponse> rows) {
            this.rows = rows;
        }

        @Override
        public List<TenantResponse> list() {
            return rows;
        }
    }

    private static Tenant tenant(UUID id, String name, TenantKind kind, UUID parent, UUID root) {
        Tenant t = Tenant.builder().id(id).name(name).slug("slug-" + name)
                .status(TenantStatus.ACTIVE).plan("PRO").build();
        t.setKind(kind);
        t.setParentTenantId(parent);
        t.setRootTenantId(root);
        return t;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> rowAt(Map<String, Object> body, int index) {
        List<?> content = (List<?>) body.get("content");
        assertNotNull(content, "la réponse doit exposer 'content'");
        return (Map<String, Object>) content.get(index);
    }

    @Test
    @DisplayName("GET /platform/tenants expose les 5 champs du modèle d'organisation")
    void list_exposeLeModeleOrganisation() {
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        tenantService.withRows(List.of(
                TenantResponse.from(tenant(rootId, "Reseau", TenantKind.DENOMINATION, null, rootId)),
                TenantResponse.from(tenant(childId, "EgliseA", TenantKind.CHURCH, rootId, rootId))));
        // 2 organisations partagent cette racine → childCount de la racine = 1.
        when(tenantRepository.countByRootTenantId(any())).thenReturn(2L);
        when(membershipRepository.countByTenantIdAndStatus(any(), any())).thenReturn(7L);

        Map<String, Object> body = controller.list(null, null, 0, 50).getBody();
        assertEquals(2, ((List<?>) body.get("content")).size());

        Map<String, Object> root = rowAt(body, 0);
        assertEquals("DENOMINATION", root.get("kind"));
        assertNull(root.get("parentTenantId"), "une racine n'a pas de parent");
        assertEquals(rootId, root.get("rootTenantId"));
        assertTrue((Boolean) root.get("isNetworkRoot"));
        assertEquals(1L, root.get("childCount"));
        assertEquals(7L, root.get("memberCount"));

        Map<String, Object> child = rowAt(body, 1);
        assertEquals("CHURCH", child.get("kind"));
        assertEquals(rootId, child.get("parentTenantId"));
        assertFalse((Boolean) child.get("isNetworkRoot"));
    }

    @Test
    @DisplayName("D7 — la réponse ne contient AUCUNE donnée nominative")
    void list_neFuitAucuneDonneeNominative() {
        UUID id = UUID.randomUUID();
        tenantService.withRows(List.of(
                TenantResponse.from(tenant(id, "Grace", TenantKind.CHURCH, null, id))));
        when(tenantRepository.countByRootTenantId(any())).thenReturn(1L);
        when(membershipRepository.countByTenantIdAndStatus(any(), any())).thenReturn(3L);

        String body = controller.list(null, null, 0, 50).getBody().toString();
        for (String forbidden : new String[]{"email", "firstname", "lastname", "phone", "members"}) {
            assertFalse(body.toLowerCase().contains(forbidden),
                    "la réponse ne doit pas exposer '" + forbidden + "'");
        }
    }

    @Test
    @DisplayName("Une organisation isolée est sa propre racine et n'a pas d'enfant")
    void organisationIsolee_naPasDeFille() {
        UUID solo = UUID.randomUUID();
        // root_tenant_id NULL en base (colonne nullable, cf. V222).
        tenantService.withRows(List.of(
                TenantResponse.from(tenant(solo, "Seule", TenantKind.CHURCH, null, null))));
        when(tenantRepository.countByRootTenantId(any())).thenReturn(1L);
        when(membershipRepository.countByTenantIdAndStatus(any(), any())).thenReturn(1L);

        Map<String, Object> row = rowAt(controller.list(null, null, 0, 50).getBody(), 0);

        // effectiveRootTenantId() produit l'identifiant lui-même — jamais null
        // (c'est le détecteur du transfert, §4.4).
        assertEquals(solo, row.get("rootTenantId"));
        assertTrue((Boolean) row.get("isNetworkRoot"));
        assertEquals(0L, row.get("childCount"));
        assertEquals("CHURCH", row.get("kind"));
    }

    @Test
    @DisplayName("GET /platform/tenants/{id} expose les enfants et le réseau")
    void detail_exposeLaDescendance() {
        UUID rootId = UUID.randomUUID();
        UUID childA = UUID.randomUUID();
        UUID childB = UUID.randomUUID();
        when(tenantRepository.findById(rootId))
                .thenReturn(Optional.of(tenant(rootId, "Reseau", TenantKind.DENOMINATION, null, rootId)));
        when(tenantRepository.findByParentTenantIdOrderByNameAsc(rootId)).thenReturn(List.of(
                tenant(childA, "A", TenantKind.CHURCH, rootId, rootId),
                tenant(childB, "B", TenantKind.CHURCH, rootId, rootId)));
        when(tenantRepository.findByRootTenantId(rootId)).thenReturn(List.of(
                tenant(childA, "A", TenantKind.CHURCH, rootId, rootId),
                tenant(childB, "B", TenantKind.CHURCH, rootId, rootId)));
        when(tenantRepository.countByRootTenantId(any())).thenReturn(3L);
        when(membershipRepository.countByTenantIdAndStatus(any(), any())).thenReturn(5L);

        Map<String, Object> body = controller.detail(rootId).getBody();

        assertEquals("DENOMINATION", body.get("kind"));
        assertEquals(2, ((List<?>) body.get("children")).size());
        assertEquals(2, ((List<?>) body.get("network")).size());
        assertEquals(2, body.get("networkSize"));
    }

    @Test
    @DisplayName("Le filtre par statut et la pagination survivent à l'enrichissement")
    void pagination_etFiltreSurvivent() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Tenant banned = Tenant.builder().id(b).name("Bannie").slug("b")
                .status(TenantStatus.CANCELLED).plan("PRO").build();
        banned.setKind(TenantKind.CHURCH);
        banned.setRootTenantId(b);
        tenantService.withRows(List.of(
                TenantResponse.from(tenant(a, "Active", TenantKind.CHURCH, null, a)),
                TenantResponse.from(banned)));
        when(tenantRepository.countByRootTenantId(any())).thenReturn(1L);
        when(membershipRepository.countByTenantIdAndStatus(any(), any())).thenReturn(0L);

        ResponseEntity<Map<String, Object>> response = controller.list(null, "ACTIVE", 0, 50);
        assertEquals(1, ((List<?>) response.getBody().get("content")).size(),
                "le filtre ACTIVE doit exclure la tenant bannie");
        assertEquals(1, response.getBody().get("total"));
    }
}