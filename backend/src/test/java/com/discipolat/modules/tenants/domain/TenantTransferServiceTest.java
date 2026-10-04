package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.multitenancy.CrossTenantScopeAccess;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * SPEC_ORGANISATION_DENOMINATION_V2 §4.4 / §7.1 / T-B5 — <b>transfert de membre</b>.
 *
 * <p>C'est le besoin central énoncé par le client (« un membre qui change
 * d'église ne se réinscrit pas ») et la règle la plus subtile du chantier :
 * la décision repose sur le {@code root_tenant_id} des deux organisations, et
 * ses deux issues doivent rester <b>distinctes</b> — un transfert qui
 * préserve l'historique d'un côté, une simple adhésion qui laisse l'ancien
 * rattachement intact de l'autre. Confondre les deux reviendrait à effacer
 * silencieusement une appartenance.
 *
 * <p><b>Note de conception du test.</b> Les dépendances <i>concrètes</i>
 * ({@code JoinCodeService}, {@code ActiveTenantService}, {@code AuditService},
 * {@code EmailService}, {@code CrossTenantScopeAccess}) sont remplacées par des
 * doublures écrites à la main : le mock inline de Byte Buddy refuse
 * d'instrumenter les classes au-delà des JDK qu'il supporte, ce qui rendrait
 * ce test inexécutable hors du runtime de référence (JDK 21). Les
 * repositories étant des interfaces, ils restent mockés.
 */
@ExtendWith(MockitoExtension.class)
class TenantTransferServiceTest {

    private static final String CODE = "BETH-7K2M";

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private OrganizationNodeRepository organizationNodeRepository;
    @Mock private UserRepository userRepository;

    /** Codes connus du faux {@code JoinCodeService}. */
    private TenantJoinCode registeredCode;

    /** Appels d'audit observés. */
    private final List<String> auditActions = new ArrayList<>();

    /** Bascules de tenant demandées (doivent toujours être réémises). */
    private final List<UUID> switchedTo = new ArrayList<>();

    private UUID userId;
    private User user;
    private TenantTransferService service;

    // ========================================================================
    // Doublures
    // ========================================================================

    private final class FixedJoinCodeService extends JoinCodeService {
        FixedJoinCodeService() {
            super(null, null, null);
        }

        @Override
        public Optional<TenantJoinCode> findActiveByRawInput(String raw) {
            return Optional.ofNullable(registeredCode);
        }
    }

    private final class FixedActiveTenantService extends ActiveTenantService {
        FixedActiveTenantService() {
            super(null, null, null);
        }

        @Override
        public SwitchOutcome switchTenant(UUID userId, UUID newTenantId) {
            switchedTo.add(newTenantId);
            return new SwitchOutcome(newTenantId, "access-" + newTenantId, "refresh", "MEMBRE");
        }
    }

    private final class RecordingAuditService extends AuditService {
        RecordingAuditService() {
            super(null, null, null, null);
        }

        @Override
        public void log(UUID actorId, UUID tenantId, String action, String resourceType,
                        UUID resourceId, String result, Map<String, Object> metadata,
                        String ipAddress, String userAgent, HttpServletRequest request) {
            auditActions.add(action);
        }

        @Override
        public void logSimple(String action, String entiteType, UUID entiteId) {
            auditActions.add(action);
        }
    }

    /**
     * Remplace la suspension du filtre Hibernate par une simple exécution du
     * supplier. Le test n'ouvre pas de {@code EntityManager} : sans cette
     * doublure, {@code call()} toucherait un proxy nul.
     */
    private static final class PassThroughCrossTenantAccess extends CrossTenantScopeAccess {
        @Override
        public <T> T call(Supplier<T> read) {
            return read.get();
        }
    }

    // ========================================================================
    // Mise en place
    // ========================================================================

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("membre@exemple.cm")
                .firstName("Grâce")
                .build();

        auditActions.clear();
        switchedTo.clear();
        registeredCode = null;

        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        lenient().when(roleRepository.findByTenantIdAndKey(any(), eq("MEMBRE")))
                .thenReturn(Optional.of(memberRole()));
        lenient().when(roleRepository.findGlobalByKey(anyString()))
                .thenReturn(Optional.of(memberRole()));

        // Un rôle MEMBRE semé par le catalogue système.
        lenient().when(roleRepository.findByTenantIdAndKey(any(), eq("MEMBRE")))
                .thenAnswer(inv -> Optional.of(memberRole()));

        lenient().when(membershipRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        // `EmailService` désactivé (`app.email.enabled=false`) : `send()` devient
        // un no-op, ce qui permet de vérifier que le transfert n'échoue pas
        // quand la notification ne part pas (spéc §8.3).
        EmailService quietEmail = new EmailService(null, "noreply@discipolat.test", false);

        service = new TenantTransferService(
                tenantRepository, membershipRepository, roleRepository,
                organizationNodeRepository, userRepository,
                new FixedJoinCodeService(), new FixedActiveTenantService(),
                new PassThroughCrossTenantAccess(), new RecordingAuditService(), quietEmail);
    }

    private Role memberRole() {
        return Role.builder()
                .id(UUID.randomUUID())
                .key("MEMBRE")
                .label("Membre")
                .system(true)
                .priority(100)
                .build();
    }

    // ========================================================================
    // Helpers de scénario
    // ========================================================================

    /** Enregistre un code d'entrée actifs pour {@code target}. */
    private TenantJoinCode codeFor(Tenant target) {
        registeredCode = TenantJoinCode.builder()
                .id(UUID.randomUUID())
                .tenantId(target.getId())
                .code(CODE)
                .joinMode(JoinMode.OPEN)
                .isActive(true)
                .createdAt(Instant.now())
                .build();
        return registeredCode;
    }

    private Tenant tenant(UUID id, String name, String slug, TenantKind kind,
                          UUID parentId, UUID rootId, TenantStatus status) {
        return Tenant.builder()
                .id(id)
                .name(name)
                .slug(slug)
                .kind(kind)
                .parentTenantId(parentId)
                .rootTenantId(rootId)
                .status(status)
                .plan("free")
                .build();
    }

    private TenantMembership membership(UUID tenantId, Instant joinedAt) {
        return TenantMembership.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .userId(userId)
                .role(memberRole())
                .roleLegacy("MEMBRE")
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE)
                .joinedAt(joinedAt)
                .build();
    }

    /** Bride le repository « hors filtre » sur la liste des appartenances. */
    private void givenActiveMemberships(List<TenantMembership> memberships) {
        when(membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE))
                .thenReturn(memberships);
    }

    private void givenExistingIn(UUID tenantId, boolean any) {
        lenient().when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(
                        eq(userId), eq(tenantId), eq(MembershipStatus.ACTIVE)))
                .thenReturn(any ? List.of(membership(tenantId, Instant.now())) : List.of());
    }

    // ========================================================================
    // CAS 1 — MÊME RÉSEAU : TRANSFERT (§4.4)
    // ========================================================================

    @Test
    @DisplayName("Même racine → TRANSFERT : l'appartenance source est tracée, pas supprimée (D5)")
    void sameRootTransfersInsteadOfRejoining() {
        UUID rootId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Tenant target = tenant(targetId, "Église de la Grâce", "grace", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);
        Tenant source = tenant(sourceId, "Église Bethel", "bethel", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);

        when(tenantRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(tenantRepository.findById(sourceId)).thenReturn(Optional.of(source));

        TenantMembership sourceMembership = membership(sourceId, Instant.now().minusSeconds(86_400));
        givenActiveMemberships(List.of(sourceMembership));
        givenExistingIn(targetId, false);
        user.setTenantId(sourceId);
        codeFor(target);

        TenantTransferService.TransferOutcome outcome =
                service.transfer(userId, CODE, "Déménagement");

        assertEquals("TRANSFERRED", outcome.status());
        assertEquals(sourceId, outcome.fromTenantId());
        assertEquals(targetId, outcome.toTenantId());

        // (1) L'historique est PRÉSERVÉ : la ligne source n'est pas supprimée,
        //     elle passe en REVOKED et porte sa trace (V223).
        assertEquals(MembershipStatus.REVOKED, sourceMembership.getStatus());
        assertNotNull(sourceMembership.getTransferredAt());
        assertEquals(targetId, sourceMembership.getTransferredToTenantId());
        assertEquals(userId, sourceMembership.getTransferredByUserId());
        assertEquals("Déménagement", sourceMembership.getTransferReason());
        assertTrue(sourceMembership.isTransferred());

        // (2) L'appartenance cible existe.
        assertEquals(1, switchedTo.size(), "une seule bascule doit être demandée");
        assertEquals(targetId, switchedTo.get(0));

        // (3) Les jetons sont RÉÉMIS : sans cela le membre resterait chez lui
        //     alors que l'écran annonce « vous avez été transféré » (T-B0bis).
        assertTrue(outcome.tokenSwitched());
        assertNotNull(outcome.accessToken());
        assertTrue(outcome.accessToken().contains(targetId.toString()));

        // (4) Tracé.
        assertTrue(auditActions.contains("TENANT_TRANSFER"));
    }

    @Test
    @DisplayName("Même racine : le membre reçoit bien une NOUVELLE appartenance dans la cible")
    void sameRootCreatesTargetMembership() {
        UUID rootId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Tenant target = tenant(targetId, "Grâce", "grace", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);
        Tenant source = tenant(sourceId, "Bethel", "bethel", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);

        when(tenantRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(tenantRepository.findById(sourceId)).thenReturn(Optional.of(source));
        givenActiveMemberships(List.of(membership(sourceId, Instant.now())));
        givenExistingIn(targetId, false);
        user.setTenantId(sourceId);
        codeFor(target);

        service.transfer(userId, CODE, null);

        // Deux écritures attendues, et c'est volontaire : (1) la ligne source
        // tracée en REVOKED, (2) la nouvelle appartenance cible. Une seule
        // écriture aurait signifié soit un historique perdu, soit une
        // appartenance orpheline.
        var captor = org.mockito.ArgumentCaptor.forClass(TenantMembership.class);
        org.mockito.Mockito.verify(membershipRepository, org.mockito.Mockito.times(2))
                .save(captor.capture());
        List<TenantMembership> saved = captor.getAllValues();

        TenantMembership tracedSource = saved.stream()
                .filter(TenantMembership::isTransferred)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "l'appartenance source doit être marquée transférée (D5)"));
        assertEquals(sourceId, tracedSource.getTenantId());
        assertEquals(MembershipStatus.REVOKED, tracedSource.getStatus());

        TenantMembership created = saved.stream()
                .filter(m -> MembershipStatus.ACTIVE == m.getStatus())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "une appartenance ACTIVE doit être créée dans la cible"));
        assertEquals(targetId, created.getTenantId());
        assertEquals(MembershipScopeType.TENANT, created.getScopeType());
        assertEquals("MEMBRE", created.getRoleLegacy());
        // Une nouvelle appartenance n'est pas « transférée » : le marqueur
        // appartient à la ligne PARTIE, sinon la trace perdrait son sens.
        assertFalse(created.isTransferred());
        assertNull(created.getTransferReason());
    }

    @Test
    @DisplayName("Même racine : un code de sous-église rattache le membre à CE nœud, pas à l'église entière")
    void transferHonoursTheSubChurchScope() {
        UUID rootId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();

        Tenant target = tenant(targetId, "Grâce", "grace", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);
        Tenant source = tenant(sourceId, "Bethel", "bethel", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);

        when(tenantRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(tenantRepository.findById(sourceId)).thenReturn(Optional.of(source));
        lenient().when(organizationNodeRepository.findById(nodeId))
                .thenReturn(Optional.of(OrganizationNode.builder()
                        .id(nodeId).tenantId(targetId).name("Campus Nord")
                        .code("CN").type(OrganizationNodeType.SUB_CHURCH).build()));

        givenActiveMemberships(List.of(membership(sourceId, Instant.now())));
        givenExistingIn(targetId, false);
        user.setTenantId(sourceId);
        codeFor(target).setOrgNodeId(nodeId);

        service.transfer(userId, CODE, null);

        var captor = org.mockito.ArgumentCaptor.forClass(TenantMembership.class);
        org.mockito.Mockito.verify(membershipRepository, org.mockito.Mockito.times(2))
                .save(captor.capture());
        TenantMembership created = captor.getAllValues().stream()
                .filter(m -> MembershipStatus.ACTIVE == m.getStatus())
                .findFirst()
                .orElseThrow();
        assertEquals(MembershipScopeType.SUB_CHURCH, created.getScopeType());
        assertEquals(nodeId, created.getScopeId());
    }

    // ========================================================================
    // CAS 2 — RÉSEAUX DIFFÉRENTS : ADHÉSION (§4.4)
    // ========================================================================

    @Test
    @DisplayName("Racines différentes → simple ADHÉSION : l'appartenance d'origine reste INTACTE")
    void differentRootsJoinsAndKeepsPreviousMembership() {
        UUID rootA = UUID.randomUUID();
        UUID rootB = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Tenant target = tenant(targetId, "Église du Port", "port", TenantKind.CHURCH,
                rootB, rootB, TenantStatus.ACTIVE);
        Tenant source = tenant(sourceId, "Église Bethel", "bethel", TenantKind.CHURCH,
                rootA, rootA, TenantStatus.ACTIVE);

        when(tenantRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(tenantRepository.findById(sourceId)).thenReturn(Optional.of(source));

        TenantMembership sourceMembership = membership(sourceId, Instant.now());
        givenActiveMemberships(List.of(sourceMembership));
        givenExistingIn(targetId, false);
        user.setTenantId(sourceId);
        codeFor(target);

        TenantTransferService.TransferOutcome outcome =
                service.transfer(userId, CODE, null);

        assertEquals("JOINED", outcome.status());
        // Le point essentiel : un réseau différent n'est PAS un transfert.
        assertEquals(MembershipStatus.ACTIVE, sourceMembership.getStatus(),
                "l'appartenance d'origine doit rester active : on rejoint EN PLUS");
        assertFalse(sourceMembership.isTransferred());
        assertNull(sourceMembership.getTransferReason());

        // Tracé : action dédiée, distincte du transfert.
        assertTrue(auditActions.contains("TENANT_JOIN_CROSS_ROOT"));
        assertFalse(auditActions.contains("TENANT_TRANSFER"));

        // Les jetons basculent néanmoins : l'organisation ACTIVE change.
        assertEquals(List.of(targetId), switchedTo);
        assertTrue(outcome.tokenSwitched());
    }

    @Test
    @DisplayName("Racine identique mais organisation distincte (église et campus) → TRANSFERT")
    void siblingChairsOfTheSameRootAreTransferNotJoin() {
        UUID rootId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        // Deux enfants directs de la même dénomination : c'est le cas le plus
        // fréquent en pratique (un pasteur change de campus), et le cas pour
        // lequel une comparaison par slug échouerait à coup sûr.
        Tenant target = tenant(targetId, "Campus Nord", "campus-nord", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);
        Tenant source = tenant(sourceId, "Campus Sud", "campus-sud", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);

        when(tenantRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(tenantRepository.findById(sourceId)).thenReturn(Optional.of(source));

        TenantMembership sourceMembership = membership(sourceId, Instant.now());
        givenActiveMemberships(List.of(sourceMembership));
        givenExistingIn(targetId, false);
        user.setTenantId(sourceId);
        codeFor(target);

        assertEquals("TRANSFERRED", service.transfer(userId, CODE, null).status());
        assertEquals(MembershipStatus.REVOKED, sourceMembership.getStatus());
    }

    // ========================================================================
    // CAS 3 — IDEMPOTENCE
    // ========================================================================

    @Test
    @DisplayName("Déjà membre de la cible → rejeu idempotent, aucune ligne dupliquée")
    void alreadyMemberIsANoOp() {
        UUID rootId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Tenant target = tenant(targetId, "Grâce", "grace", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);
        when(tenantRepository.findById(targetId)).thenReturn(Optional.of(target));

        TenantMembership existing = membership(targetId, Instant.now());
        givenActiveMemberships(List.of(existing));
        codeFor(target);

        TenantTransferService.TransferOutcome outcome = service.transfer(userId, CODE, null);

        assertEquals("ALREADY_MEMBER", outcome.status());
        // Aucune bascule : le client garde ses jetons valides.
        assertTrue(switchedTo.isEmpty());
        assertFalse(outcome.tokenSwitched());
        assertNull(outcome.accessToken());
        // L'appartenence existante n'est ni révoquée ni modifiée.
        assertEquals(MembershipStatus.ACTIVE, existing.getStatus());
        assertFalse(existing.isTransferred());

        org.mockito.Mockito.verify(membershipRepository, org.mockito.Mockito.never())
                .save(any());
    }

    // ========================================================================
    // CAS 4 — MEMBRE SANS ORGANISATION ACTIVE
    // ========================================================================

    @Test
    @DisplayName("Nouveau compte sans organisation → adhésion simple, pas de transfert")
    void brandNewMemberSimplyJoins() {
        UUID rootId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Tenant target = tenant(targetId, "Grâce", "grace", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);
        when(tenantRepository.findById(targetId)).thenReturn(Optional.of(target));
        givenActiveMemberships(List.of());
        givenExistingIn(targetId, false);
        user.setTenantId(null);
        codeFor(target);

        TenantTransferService.TransferOutcome outcome = service.transfer(userId, CODE, null);

        assertEquals("JOINED", outcome.status());
        assertNull(outcome.fromTenantId());
        assertEquals(List.of(targetId), switchedTo);
    }

    // ========================================================================
    // CAS 5 — REFUS
    // ========================================================================

    @Test
    @DisplayName("Code inconnu → 404 explicite, aucune écriture")
    void unknownCodeIsRejected() {
        registeredCode = null;

        DomainException failure = assertThrows(DomainException.class,
                () -> service.transfer(userId, "INCONNU-0000", null));

        assertEquals("JOIN_CODE_NOT_FOUND", failure.getCode());
        assertTrue(switchedTo.isEmpty());
        assertTrue(auditActions.isEmpty());
    }

    @Test
    @DisplayName("Église bloquée ou bannie → 410 : elle n'accepte pas d'adhésion")
    void inactiveTargetIsRejected() {
        UUID targetId = UUID.randomUUID();
        Tenant blocked = tenant(targetId, "Bethel", "bethel", TenantKind.CHURCH,
                targetId, targetId, TenantStatus.SUSPENDED);
        when(tenantRepository.findById(targetId)).thenReturn(Optional.of(blocked));
        codeFor(blocked);

        DomainException failure = assertThrows(DomainException.class,
                () -> service.transfer(userId, CODE, null));

        assertEquals("TENANT_NOT_ACCEPTING_JOIN", failure.getCode());
        assertTrue(switchedTo.isEmpty());
    }

    // ========================================================================
    // PREVIEW — ce que l'IHM doit annoncer AVANT confirmation
    // ========================================================================

    @Test
    @DisplayName("Preview : même réseau → l'IHM doit parler de TRANSFERT, pas d'adhésion")
    void previewAnnouncesTransferForSameNetwork() {
        UUID rootId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Tenant target = tenant(targetId, "Grâce", "grace", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);
        Tenant source = tenant(sourceId, "Bethel", "bethel", TenantKind.CHURCH,
                rootId, rootId, TenantStatus.ACTIVE);

        when(tenantRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(tenantRepository.findById(sourceId)).thenReturn(Optional.of(source));
        givenActiveMemberships(List.of(membership(sourceId, Instant.now())));
        user.setTenantId(sourceId);
        codeFor(target);

        TenantTransferService.TransferPreview preview = service.preview(userId, CODE);

        assertTrue(preview.sameNetwork());
        assertFalse(preview.alreadyMember());
        assertEquals("Bethel", preview.fromChurch());
        assertEquals("Grâce", preview.toChurch());
    }

    @Test
    @DisplayName("Preview : réseaux différents → l'IHM doit prévenir que l'adhésion antérieure reste")
    void previewAnnouncesJoinForDifferentNetwork() {
        UUID rootA = UUID.randomUUID();
        UUID rootB = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Tenant target = tenant(targetId, "Port", "port", TenantKind.CHURCH,
                rootB, rootB, TenantStatus.ACTIVE);
        Tenant source = tenant(sourceId, "Bethel", "bethel", TenantKind.CHURCH,
                rootA, rootA, TenantStatus.ACTIVE);

        when(tenantRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(tenantRepository.findById(sourceId)).thenReturn(Optional.of(source));
        givenActiveMemberships(List.of(membership(sourceId, Instant.now())));
        user.setTenantId(sourceId);
        codeFor(target);

        TenantTransferService.TransferPreview preview = service.preview(userId, CODE);

        assertFalse(preview.sameNetwork());
        assertTrue(preview.activeInOther(),
                "le membre garde son adhésion : l'IHM doit le dire explicitement");
    }

    @Test
    @DisplayName("Preview : déjà membre → « rien à faire », jamais « vous allez rejoindre »")
    void previewReportsAlreadyMember() {
        UUID targetId = UUID.randomUUID();
        Tenant target = tenant(targetId, "Grâce", "grace", TenantKind.CHURCH,
                targetId, targetId, TenantStatus.ACTIVE);
        when(tenantRepository.findById(targetId)).thenReturn(Optional.of(target));
        givenActiveMemberships(List.of(membership(targetId, Instant.now())));
        user.setTenantId(targetId);
        codeFor(target);

        TenantTransferService.TransferPreview preview = service.preview(userId, CODE);

        assertTrue(preview.alreadyMember());
    }

    @Test
    @DisplayName("Preview : la nature de l'organisation cible est exposée (D2)")
    void previewExposesTargetKind() {
        UUID rootId = UUID.randomUUID();
        Tenant target = tenant(UUID.randomUUID(), "Dénomination Nord", "denomination-nord",
                TenantKind.DENOMINATION, null, rootId, TenantStatus.ACTIVE);
        when(tenantRepository.findById(target.getId())).thenReturn(Optional.of(target));
        givenActiveMemberships(List.of());
        user.setTenantId(null);
        codeFor(target);

        assertEquals(TenantKind.DENOMINATION.name(),
                service.preview(userId, CODE).toKind());
    }
}
