package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * verrou d'ÉQUIVALENCE du pont rôle-actif → permission
 * (PARAMÉTRISATION PAR ÉGLISE, Phase 1).
 *
 * <p>Contrat prouvé ici, en miroir de la matrice par défaut installée par
 * V258 : pour toute clé {@code <CLÉ>} accordée par {@code hasAnyRole('A','B',...)},
 * on a exactement — et dans les deux sens —
 * <pre>@authz.can('<CLÉ>','TENANT',null)  ≡  hasAnyRole('A','B',...)</pre>
 * (1) le pont ACCORDE la clé si l'autorité active du jeton ({@code ROLE_<clé>})
 * résout un rôle global qui la détient dans la matrice — sans exiger la
 * moindre appartenance, comme le faisait le littéral legacy ;
 * (2) le pont REFUSE toute clé absente de la matrice du rôle actif
 * (fail-closed par clé — un jeton {@code ROLE_X} inventé n'accorde rien) ;
 * (3) la surcouche locale d'une église PRIME le rôle global pour la même clé
 * (surface de customisation : l'église peut restreindre comme étendre) ;
 * (4) le pont est purement ADDITIF : les chemins existants (super-admin,
 * appartenance avec permission, assignations V3) gardent leur verdict, et
 * hors requête authentifiée le pont n'accorde rien.
 *
 * <p>Le pont lit les autorités via {@code SecurityUtils.getActiveAuthorityRoleKeys()}
 * (infra — la règle R1 interdit Spring Security dans le domaine) : les tests
 * posent donc un vrai {@code SecurityContextHolder}, sans Spring context.
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceEquivalenceBridgeTest {

    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000e2");

    /** Clé d'exemple de la matrice V258 : MEMBERS_EXECUTE = {ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE}. */
    private static final String MEMBERS_EXECUTE = "MEMBERS_EXECUTE";
    /** Clé que la matrice n'accorde PAS à PASTEUR (absente de toute matrice de test). */
    private static final String FINANCE_EXECUTE = "FINANCE_EXECUTE";

    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private OrganizationNodeRepository orgNodeRepository;
    @Mock private UserRepository userRepository;
    @Mock private MemberRoleAssignmentRepository assignmentRepository;
    @Mock private RoleRepository roleRepository;

    private AuthorizationService authz;

    @BeforeEach
    void setUp() {
        authz = new AuthorizationService(membershipRepository, permissionRepository,
                orgNodeRepository, userRepository, assignmentRepository, roleRepository);
        SecurityContextHolder.clearContext();
        // Stubs de résolution CUMULATIFS (la matrice du test s'enrichit via
        // globalRole/tenantRole) : plusieurs rôles posés dans un même scénario
        // résolvent chacun, sans qu'un second stub n'écrase le premier.
        lenient().when(roleRepository.findGlobalByKey(any())).thenAnswer(inv ->
                Optional.ofNullable(globalMatrix.get(inv.<String>getArgument(0))));
        lenient().when(roleRepository.findByTenantIdAndKey(any(), any())).thenAnswer(inv ->
                Optional.ofNullable(tenantMatrix.get(inv.<String>getArgument(1))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== helpers ====================

    /** Imite le filtre JWT : autorités {@code ROLE_<clé>} pour l'utilisateur courant. */
    private void authenticate(String... roleKeys) {
        List<GrantedAuthority> authorities = Arrays.stream(roleKeys)
                .map(k -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + k))
                .collect(Collectors.toList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER, "N/A", authorities));
    }

    private static Permission perm(String key) {
        return Permission.builder().id(UUID.randomUUID()).key(key).label(key)
                .scope(PermissionScope.TENANT).system(true).build();
    }

    private final Map<String, Role> globalMatrix = new HashMap<>();
    private final Map<String, Role> tenantMatrix = new HashMap<>();

    private static Role buildRole(UUID tenantId, String key, String... grantedPermissions) {
        return Role.builder().id(UUID.randomUUID()).tenantId(tenantId).key(key).label(key)
                .system(tenantId == null).priority(100)
                .permissions(Arrays.stream(grantedPermissions).map(AuthorizationServiceEquivalenceBridgeTest::perm)
                        .collect(Collectors.toCollection(HashSet::new)))
                .build();
    }

    /** Rôle global (tenant_id NULL, comme créés par V258 §1) ajouté à la matrice de résolution globale.
     *  Cumulatif : plusieurs rôles posés dans un même scénario résolvent chacun. */
    private void globalRole(String key, String... grantedPermissions) {
        globalMatrix.put(key, buildRole(null, key, grantedPermissions));
    }

    /** Surcouche locale de l'église pour la MÊME clé, ajoutée à la résolution tenant (prime sur le global). */
    private void tenantRole(String key, String... grantedPermissions) {
        tenantMatrix.put(key, buildRole(TENANT, key, grantedPermissions));
    }

    private void withMembership(Role role) {
        TenantMembership membership = TenantMembership.builder()
                .id(UUID.randomUUID()).userId(USER).tenantId(TENANT).role(role)
                .roleLegacy(role.getKey()).scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE).joinedAt(Instant.now())
                .build();
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(USER, TENANT, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership));
    }

    private boolean can(String permissionKey) {
        return authz.can(USER, TENANT, permissionKey, MembershipScopeType.TENANT, null);
    }

    // ==================== (1) reproduction exacte de hasAnyRole ====================

    @Test
    @DisplayName("hasAnyRole('PASTEUR',…) ≡ @authz.can : l'autorité active dont la matrice détient la clé accorde, SANS appartenance")
    void bridgeReproducesHasAnyRoleWithoutRequiringMembership() {
        authenticate("PASTEUR");
        globalRole("PASTEUR", MEMBERS_EXECUTE);
        // aucune stub d'appartenance : le littéral legacy ne demandait pas d'appartenance.
        assertThat(can(MEMBERS_EXECUTE)).isTrue();
    }

    @Test
    @DisplayName("Hérité du filtre JWT (ADMIN porte aussi ROLE_PASTEUR) : la clé détenue par l'un des rôles actifs suffit")
    void anyActiveAuthorityMayGrant() {
        authenticate("ADMIN", "PASTEUR");
        globalRole("ADMIN");                    // matrice ADMIN sans cette clé dans ce test
        globalRole("PASTEUR", MEMBERS_EXECUTE); // mais PASTEUR la détient
        assertThat(can(MEMBERS_EXECUTE)).isTrue();
        assertThat(can(FINANCE_EXECUTE)).isFalse(); // ni l'un ni l'autre : refus
    }

    @Test
    @DisplayName("Normalisation des autorités : « ROLE_pasteur » minuscule résout le rôle global PASTEUR")
    void authorityKeysAreNormalized() {
        authenticate("pasteur");
        globalRole("PASTEUR", MEMBERS_EXECUTE);
        assertThat(can(MEMBERS_EXECUTE)).isTrue();
    }

    // ==================== (2) fail-closed par clé ====================

    @Test
    @DisplayName("Porte de scope : une vérification scopée sur un nœud n'est JAMAIS accordée par les seules autorités")
    void bridgeNeverBypassesNodeScope() {
        authenticate("PASTEUR");
        globalRole("PASTEUR", "MEMBER_READ");
        UUID autreDepartement = UUID.randomUUID();
        // Le pont reproduit les littéraux legacy, toujours de portée tenant ;
        // l'isolation DEPARTMENT/CHURCH/FAMILY/ASSIGNED/OWN reste d'appartenance
        // (cf. MultiTenantSecurityTests) — le pont ne saute pas par-dessus.
        assertThat(authz.can(USER, TENANT, "MEMBER_READ", MembershipScopeType.DEPARTMENT, autreDepartement))
                .isFalse();
        assertThat(authz.can(USER, TENANT, "MEMBER_READ", MembershipScopeType.TENANT, null))
                .isTrue();
    }

    @Test
    @DisplayName("Clé absente de la matrice du rôle actif : refus — le pont n'élargit jamais")
    void ungrantedKeyIsRefused() {
        authenticate("PASTEUR");
        globalRole("PASTEUR", MEMBERS_EXECUTE);
        assertThat(can(FINANCE_EXECUTE)).isFalse();
    }

    @Test
    @DisplayName("Autorité ROLE_<inconnu> : aucun rôle résolu, rien n'est accordé")
    void unknownAuthorityRoleGrantsNothing() {
        authenticate("PROPHETE_INVENTE");
        assertThat(can(MEMBERS_EXECUTE)).isFalse();
    }

    @Test
    @DisplayName("Hors requête authentifiée : le pont n'accorde rien (fail-closed)")
    void anonymousYieldsNoGrant() {
        globalRole("PASTEUR", MEMBERS_EXECUTE);
        assertThat(can(MEMBERS_EXECUTE)).isFalse();
    }

    // ==================== (3) surface de customisation par église ====================

    @Test
    @DisplayName("L'église peut RESTREINDRE : sa surcouche locale du rôle prime la matrice globale")
    void tenantOverrideCanNarrow() {
        authenticate("PASTEUR");
        tenantRole("PASTEUR");                  // l'église retire MEMBERS_EXECUTE à son PASTEUR local
        globalRole("PASTEUR", MEMBERS_EXECUTE); // la matrice globale, elle, la détient
        assertThat(can(MEMBERS_EXECUTE)).isFalse();
    }

    @Test
    @DisplayName("L'église peut ÉTENDRE : une clé ajoutée à son rôle local est accordée sans migration")
    void tenantOverrideCanWiden() {
        authenticate("RESPONSABLE");
        tenantRole("RESPONSABLE", FINANCE_EXECUTE); // aucun rôle global RESPONSABLE dans ce test
        assertThat(can(FINANCE_EXECUTE)).isTrue();
    }

    // ==================== (4) purement additif : les chemins existants gardent leur verdict ====================

    @Test
    @DisplayName("Chemin appartenance (sans aucune autorité) : inchangé — la permission du rôle d'appartenance accorde")
    void membershipPathUnchangedWithoutAuthorities() {
        Role memberRole = Role.builder().id(UUID.randomUUID()).tenantId(TENANT).key("MEMBRE").label("MEMBRE")
                .permissions(new HashSet<>(List.of(perm(MEMBERS_EXECUTE))))
                .build();
        withMembership(memberRole);
        assertThat(can(MEMBERS_EXECUTE)).isTrue();
        assertThat(can(FINANCE_EXECUTE)).isFalse();
    }

    @Test
    @DisplayName("Additivité : appartenance insuffisante + autorité active dotée → accord (le pont s'ajoute, ne remplace pas)")
    void bridgeAddsToNegativeMembershipOutcome() {
        Role plainMember = Role.builder().id(UUID.randomUUID()).tenantId(TENANT).key("MEMBRE").label("MEMBRE")
                .permissions(new HashSet<>())
                .build();
        withMembership(plainMember);
        authenticate("PASTEUR");
        globalRole("PASTEUR", MEMBERS_EXECUTE);
        assertThat(can(MEMBERS_EXECUTE)).isTrue();
    }

    @Test
    @DisplayName("Court-circuit super-admin plateforme : antérieur au pont, intact")
    void platformSuperAdminShortCircuits() {
        when(membershipRepository.findPlatformRoleKeysByUserId(USER, MembershipStatus.ACTIVE.name()))
                .thenReturn(List.of("PLATFORM_SUPER_ADMIN"));
        assertThat(can("N_IMPORTE_QUI_CETTE_CLE")).isTrue();
    }

    // ==================== (5) miroir servi au client (Phase 2-client) ====================

    @Test
    @DisplayName("Ensemble SERVI = clés des autorités actives : tout ce que le pont accorde est annoncément visible")
    void servedSetMirrorsBridgeGrants() {
        authenticate("PASTEUR", "RESPONSABLE");
        globalRole("PASTEUR", MEMBERS_EXECUTE, "MEMBERS_EXECUTE_QRCHECKIN");
        globalRole("RESPONSABLE", FINANCE_EXECUTE);
        assertThat(authz.permissionKeysForActiveAuthorities(TENANT))
                .containsExactlyInAnyOrder(MEMBERS_EXECUTE, "MEMBERS_EXECUTE_QRCHECKIN", FINANCE_EXECUTE);
    }

    @Test
    @DisplayName("Customisation par église visible côté client : la surcouche tenant prime dans le set servi")
    void servedSetFollowsTenantOverride() {
        authenticate("PASTEUR");
        tenantRole("PASTEUR", FINANCE_EXECUTE);   // l'église redéfinit son PASTEUR local
        globalRole("PASTEUR", MEMBERS_EXECUTE);   // la matrice globale, elle, détient l'autre clé
        assertThat(authz.permissionKeysForActiveAuthorities(TENANT))
                .containsExactly(FINANCE_EXECUTE);
    }

    @Test
    @DisplayName("Hors requête authentifiée : le set servi est vide (aucune fuite hors session)")
    void servedSetEmptyWhenAnonymous() {
        globalRole("PASTEUR", MEMBERS_EXECUTE);
        assertThat(authz.permissionKeysForActiveAuthorities(TENANT)).isEmpty();
    }
}
