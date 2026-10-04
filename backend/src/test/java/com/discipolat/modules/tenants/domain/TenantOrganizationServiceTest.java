package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.api.CreateTenantRequest;
import com.discipolat.modules.tenants.api.TenantResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SPEC_ORGANISATION_DENOMINATION_V2 §7.1 / T-B3 — création d'un réseau
 * d'organisations.
 *
 * <p>Verrouille les règles qui seraient silencieusement fausses : une église
 * ne peut pas fédérer d'autres églises, une enfant hérite de la racine de sa
 * mère (c'est ce qui rend le transfert détectable, §4.4), et toute création
 * passe par le service de provisionnement existant plutôt que par une
 * duplication de la logique.
 *
 * <p><b>Note de conception du test.</b> Les dépendancesConcrete
 * ({@code TenantService}, {@code AuditService}, …) sont remplacées par des
 * <b>fakes écrits à la main</b> plutôt que par des mocks Mockito : le mock
 * inline de Byte Buddy refuse d'instrumenter les classes sur les JDK
 * récents, ce qui rendrait ce test inexécutable hors JDK 21 (le runtime de
 * référence du projet). Les repositories étant des interfaces, ils restent
 * mockés — c'est sans risque et sans surcoût.
 */
@ExtendWith(MockitoExtension.class)
class TenantOrganizationServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private RoleRepository roleRepository;

    /** Faux TenantService : enregistre les créations et persiste un tenant. */
    private static final class FakeTenantService extends TenantService {
        private final java.util.Map<UUID, Tenant> store;
        private final List<CreateTenantRequest> requests = new ArrayList<>();

        FakeTenantService(java.util.Map<UUID, Tenant> store) {
            super(null, null, null, null, null, null, null, null, null);
            this.store = store;
        }

        @Override
        public TenantResponse create(CreateTenantRequest request) {
            requests.add(request);
            Tenant created = Tenant.builder()
                    .id(UUID.randomUUID())
                    .name(request.name())
                    .slug(request.slug())
                    .status(TenantStatus.PENDING_SETUP)
                    .plan("free")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            store.put(created.getId(), created);
            return TenantResponse.from(created);
        }
    }

    private FakeTenantService tenantService;
    private RecordingAuditService auditService;
    private TenantOrganizationService service;

    /** Tenant « persistés » par le fake : sert à la relecture de l'appelant. */
    private final java.util.Map<UUID, Tenant> persisted = new java.util.HashMap<>();

    @BeforeEach
    void setUp() {
        persisted.clear();
        tenantService = new FakeTenantService(persisted);
        auditService = new RecordingAuditService();

        // Le rôle propriétaire est semé par le catalogue système (V135) :
        // sans lui, la création échouerait sur OWNER_ROLE_MISSING. On en
        // fournit un minimal — l'objet n'est pas observé ici.
        lenient().when(roleRepository.findGlobalByKey(anyString()))
                .thenReturn(Optional.of(Role.builder()
                        .id(UUID.randomUUID())
                        .key("TENANT_OWNER")
                        .label("Propriétaire")
                        .system(true)
                        .priority(900)
                        .build()));

        // Le service relit l'entité après `TenantService.create` (contrat d'API
        // qui renvoie un DTO). On bride donc le repository sur le registre du
        // fake — sinon toute création se terminerait par TENANT_NOT_PERSISTED.
        //
        // `lenient` : ces deux brides ne sont utilisées que par les scénarios
        // de création et de lecture. Sans `lenient`, Mockito les déclencherait
        // comme « stubbing inutile » dans les cas de refus (nom vide, parent
        // inconnu…) — un faux échec qui masquerait le vrai comportement testé.
        lenient().when(tenantRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(persisted.get(inv.getArgument(0))));
        lenient().when(tenantRepository.save(any()))
                .thenAnswer(inv -> {
                    Tenant t = inv.getArgument(0);
                    persisted.put(t.getId(), t);
                    return t;
                });

        service = new TenantOrganizationService(
                tenantRepository, tenantService, membershipRepository, roleRepository,
                new NoopOrganizationNodeService(), auditService);
    }

    /** Faux qui n'enregistre que les actions d'audit — sans dépendance Mockito. */
    private static final class RecordingAuditService extends AuditService {
        private final List<String> actions = new ArrayList<>();

        RecordingAuditService() {
            super(null, null, null, null);
        }

        @Override
        public void log(UUID actorId, UUID tenantId, String action, String resourceType,
                        UUID resourceId, String result, java.util.Map<String, Object> metadata,
                        String ipAddress, String userAgent, jakarta.servlet.http.HttpServletRequest request) {
            actions.add(action);
        }

        @Override
        public void logSimple(String action, String entiteType, UUID entiteId) {
            actions.add(action);
        }
    }

    private static final class NoopOrganizationNodeService extends OrganizationNodeService {
        NoopOrganizationNodeService() {
            super(null, null, null, null, null);
        }

        @Override
        public OrganizationNode createRootChurch(UUID tenantId, String name, String code, UUID ownerId) {
            return OrganizationNode.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .name(name)
                    .code(code)
                    .type(OrganizationNodeType.ROOT_CHURCH)
                    .build();
        }
    }


    // ========================================================================
    // DÉNOMINATION
    // ========================================================================

    @Test
    @DisplayName("Une dénomination est sa propre racine — c'est ce qui rend le transfert détectable")
    void denominationIsItsOwnRoot() {
        Tenant root = service.createDenomination("Dénomination Nord", UUID.randomUUID(),
                TenantKind.DENOMINATION, null);

        assertEquals(TenantKind.DENOMINATION, root.getKind());
        assertEquals(null, root.getParentTenantId(), "une racine n'a pas de parent");
        assertEquals(root.getId(), root.effectiveRootTenantId(),
                "sans racine non nulle, le détecteur du transfert (§4.4) n'a rien à comparer");
        assertTrue(tenantService.requests.size() == 1,
                "la création doit passer par TenantService.create (provisionnement, non dupliqué)");
        assertEquals(1, auditService.actions.size());
        assertEquals("TENANT_ORG_CREATED", auditService.actions.get(0));
    }

    @Test
    @DisplayName("Une ÉGLISE ne peut pas fédérer d'autres églises — refus AVANT toute écriture")
    void churchKindCannotHaveChildren() {
        DomainException failure = assertThrows(DomainException.class,
                () -> service.createDenomination("Bethel", UUID.randomUUID(),
                        TenantKind.CHURCH, null));

        assertEquals("KIND_CANNOT_HAVE_CHILDREN", failure.getCode());
        assertTrue(tenantService.requests.isEmpty(),
                "le refus doit précéder la création : sinon un tenant orphelin subsiste");
    }

    @Test
    @DisplayName("Le nom est obligatoire")
    void nameIsRequired() {
        assertThrows(DomainException.class,
                () -> service.createDenomination("   ", UUID.randomUUID(),
                        TenantKind.DENOMINATION, null));
        assertTrue(tenantService.requests.isEmpty());
    }

    @Test
    @DisplayName("Un nom démesuré est refusé plutôt que tronqué")
    void oversizedNameIsRefused() {
        String tooLong = "É".repeat(200);
        DomainException failure = assertThrows(DomainException.class,
                () -> service.createDenomination(tooLong, UUID.randomUUID(),
                        TenantKind.DENOMINATION, null));
        assertEquals("NAME_TOO_LONG", failure.getCode());
    }

    // ========================================================================
    // ÉGLISE ENFANT — héritage de racine
    // ========================================================================

    @Test
    @DisplayName("Une église enfant hérite de la racine de sa mère (§4.4, cas 1)")
    void childChurchInheritsRootFromParent() {
        UUID parentId = UUID.randomUUID();
        UUID rootId = UUID.randomUUID();
        persisted.put(parentId, denomination(parentId, rootId));

        Tenant child = service.createChildChurch(parentId, "Église de la Grâce",
                UUID.randomUUID(), null);

        assertEquals(parentId, child.getParentTenantId());
        assertEquals(rootId, child.getRootTenantId(),
                "l'héritage de racine est ce qui permet le transfert entre églises sœurs");
        assertEquals(TenantKind.CHURCH, child.getKind());
    }

    @Test
    @DisplayName("Une église sans parent est sa propre racine : le modèle reste utilisable hors réseau")
    void childChurchWithoutParentIsItsOwnRoot() {
        Tenant child = service.createChildChurch(null, "Église Isolée", null, null);

        assertEquals(child.getId(), child.effectiveRootTenantId());
        assertEquals(null, child.getParentTenantId());
    }

    @Test
    @DisplayName("On ne rattache pas une église à une église : le parent doit être un conteneur")
    void parentChurchCannotHostChildren() {
        UUID parentId = UUID.randomUUID();
        persisted.put(parentId, Tenant.builder()
                .id(parentId).name("Bethel").slug("bethel")
                .kind(TenantKind.CHURCH)
                .rootTenantId(parentId)
                .status(TenantStatus.ACTIVE)
                .build());

        DomainException failure = assertThrows(DomainException.class,
                () -> service.createChildChurch(parentId, "Sous-église", UUID.randomUUID(), null));

        assertEquals("PARENT_KIND_CANNOT_HAVE_CHILDREN", failure.getCode());
        assertTrue(tenantService.requests.isEmpty());
    }

    @Test
    @DisplayName("Un parent suspendu n'accueille pas de nouvelle église")
    void suspendedParentRefusesChildren() {
        UUID parentId = UUID.randomUUID();
        persisted.put(parentId, Tenant.builder()
                .id(parentId).name("Dénomination").slug("denomination")
                .kind(TenantKind.DENOMINATION)
                .rootTenantId(parentId)
                .status(TenantStatus.SUSPENDED)
                .build());

        DomainException failure = assertThrows(DomainException.class,
                () -> service.createChildChurch(parentId, "Nouvelle", UUID.randomUUID(), null));

        assertEquals("PARENT_TENANT_NOT_ACCEPTING", failure.getCode());
        assertTrue(tenantService.requests.isEmpty());
    }

    @Test
    @DisplayName("Doublon de nom refusé dans la même dénomination")
    void duplicateChildNameRefused() {
        UUID parentId = UUID.randomUUID();
        persisted.put(parentId, denomination(parentId, parentId));
        when(tenantRepository.existsByParentTenantIdAndNameIgnoreCase(any(), anyString()))
                .thenReturn(true);

        DomainException failure = assertThrows(DomainException.class,
                () -> service.createChildChurch(parentId, "Grâce", UUID.randomUUID(), null));

        assertEquals("CHILD_NAME_TAKEN", failure.getCode());
        assertTrue(tenantService.requests.isEmpty());
    }

    @Test
    @DisplayName("Parent introuvable : 404 explicite, aucune création fantôme")
    void unknownParentIsNotFound() {
        DomainException failure = assertThrows(DomainException.class,
                () -> service.createChildChurch(UUID.randomUUID(), "X", UUID.randomUUID(), null));

        assertEquals("PARENT_TENANT_NOT_FOUND", failure.getCode());
        assertTrue(tenantService.requests.isEmpty());
    }

    // ========================================================================
    // LECTURE DU RÉSEAU (D7 — agrégats seulement)
    // ========================================================================

    @Test
    @DisplayName("La vue réseau expose un agrégat et aucune donnée nominative")
    void networkViewExposesAggregatesOnly() {
        UUID rootId = UUID.randomUUID();
        persisted.put(rootId, denomination(rootId, rootId));
        when(tenantRepository.countByRootTenantId(rootId)).thenReturn(3L);

        TenantOrganizationService.OrganizationView view = service.view(rootId);

        assertEquals(rootId, view.id());
        assertEquals(TenantKind.DENOMINATION.name(), view.kind());
        assertEquals(3, view.childCount());
        assertTrue(!view.toString().contains("@"),
                "D7 : la vue réseau ne doit contenir aucune donnée personnelle");
    }

    @Test
    @DisplayName("La racine d'un réseau est lue par son identifiant, pas devinée")
    void networkOfReadsByRoot() {
        UUID rootId = UUID.randomUUID();
        Tenant root = denomination(rootId, rootId);
        persisted.put(rootId, root);
        when(tenantRepository.countByRootTenantId(rootId)).thenReturn(1L);
        when(tenantRepository.findByRootTenantId(rootId)).thenReturn(List.of(root));

        List<TenantOrganizationService.OrganizationView> network = service.networkOf(rootId);

        assertEquals(1, network.size());
        assertEquals(rootId, network.get(0).id());
    }

    @Test
    @DisplayName("Une organisation introuvable est un 404 explicite")
    void unknownOrganizationIsNotFound() {
        DomainException failure = assertThrows(DomainException.class,
                () -> service.view(UUID.randomUUID()));

        assertEquals("TENANT_NOT_FOUND", failure.getCode());
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private Tenant denomination(UUID id, UUID rootId) {
        return Tenant.builder()
                .id(id)
                .name("Dénomination Nord")
                .slug("denomination-nord")
                .kind(TenantKind.DENOMINATION)
                .rootTenantId(rootId)
                .status(TenantStatus.ACTIVE)
                .plan("free")
                .build();
    }
}
