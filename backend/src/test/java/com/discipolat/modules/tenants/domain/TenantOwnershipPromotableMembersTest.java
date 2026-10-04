package com.discipolat.modules.tenants.domain;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * SPEC_ORGANISATION_DENOMINATION_V2 §7.2 / T-W7 — sélection des délégués.
 *
 * <p>L'écran « Propriété & délégation » a été créé sans qu'aucune API ne
 * permette de nommer quelqu'un : la seule liste disponible était celle des
 * administrateurs <i>déjà</i> délégués. La délégation demandée par le client
 * était donc inatteignable — le serveur ne pouvait pas ce que l'écran proposait.
 * {@code /tenant/ownership/promotable} comble cet écart.
 *
 * <p>Les règles testées ici sont celles qui cassent silencieusement : proposer
 * le propriétaire comme délégué (la promotion serait sans effet), ou proposer
 * quelqu'un qui l'est déjà.
 *
 * <p><b>Note de conception.</b> Les classes concrètes ({@code AuditService},
 * {@code EmailService}, {@code CrossTenantScopeAccess}) sont remplacées par des
 * doublures : le mock inline de Byte Buddy refuse de les instrumenter au-delà
 * des JDK qu'il supporte, ce qui rendrait ce test inexécutable hors JDK 21
 * (runtime de référence du projet). Les repositories, eux, sont des interfaces.
 */
@ExtendWith(MockitoExtension.class)
class TenantOwnershipPromotableMembersTest {

    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;

    private TenantOwnershipService service;
    private UUID tenantId;

    /** Exécute le supplier sans `EntityManager` : le test n'ouvre pas de session. */
    private static final class PassThroughCrossTenantAccess extends CrossTenantScopeAccess {
        @Override
        public <T> T call(Supplier<T> read) {
            return read.get();
        }
    }

    private static final class SilentAuditService extends AuditService {
        SilentAuditService() {
            super(null, null, null, null);
        }

        @Override
        public void log(UUID actorId, UUID tenantId, String action, String resourceType,
                        UUID resourceId, String result, Map<String, Object> metadata,
                        String ipAddress, String userAgent, HttpServletRequest request) {
            // no-op
        }
    }

    @BeforeEach
    void setUp() {
        service = new TenantOwnershipService(
                membershipRepository, roleRepository, userRepository, tenantRepository,
                new PassThroughCrossTenantAccess(), new SilentAuditService(),
                new EmailService(null, "noreply@discipolat.test", false));
        tenantId = UUID.randomUUID();
    }

    private TenantMembership membership(UUID userId, String roleKey, Instant joinedAt) {
        Role role = Role.builder().id(UUID.randomUUID()).key(roleKey).label(roleKey).build();
        return TenantMembership.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .userId(userId)
                .role(role)
                .roleLegacy(roleKey)
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE)
                .joinedAt(joinedAt)
                .build();
    }

    private void givenOwner(UUID ownerUserId) {
        Role ownerRole = Role.builder().id(UUID.randomUUID())
                .key("TENANT_OWNER").label("Propriétaire").build();
        TenantMembership owner = TenantMembership.builder()
                .id(UUID.randomUUID()).tenantId(tenantId).userId(ownerUserId)
                .role(ownerRole).roleLegacy("TENANT_OWNER")
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE).joinedAt(Instant.now()).build();
        lenient().when(membershipRepository.findByTenantIdAndStatusAndRoleContaining(
                any(), any(), any())).thenReturn(List.of(owner));
    }

    private void givenUser(UUID userId, String email, String first, String last) {
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().id(userId).email(email).firstName(first).lastName(last).build()));
    }

    // ========================================================================
    // Règles d'exclusion
    // ========================================================================

    @Test
    @DisplayName("Le propriétaire et les administrateurs déjà délégués ne sont PAS proposés")
    void excludesOwnerAndExistingAdmins() {
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID plainId = UUID.randomUUID();

        givenOwner(ownerId);
        givenUser(ownerId, "roi@exemple.cm", "Le", "Roi");
        givenUser(adminId, "admin@exemple.cm", "Ad", "Min");
        givenUser(plainId, "membre@exemple.cm", "Jean", "Kouassi");

        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(
                        membership(ownerId, "TENANT_OWNER", Instant.now()),
                        // Clé du délégué : `TENANT_ADMIN` (et non `ADMIN`, qui
                        // est le rôle legacy global de l'utilisateur). Seule la
                        // bonne valeur permet de vérifier l'exclusion.
                        membership(adminId, "TENANT_ADMIN", Instant.now()),
                        membership(plainId, "MEMBRE", Instant.now())));

        List<Map<String, Object>> promotable = service.promotableMembers(tenantId);

        assertEquals(1, promotable.size(),
                "seul le membre ordinaire est promouvable : propriétaire et délégué déjà en place "
                        + "ne produiraient aucun changement");
        assertEquals(plainId, promotable.get(0).get("userId"));
        assertEquals("Jean Kouassi", promotable.get(0).get("name"));
    }

    @Test
    @DisplayName("Un membre sans nom connu est proposé par son email — jamais une ligne illisible")
    void fallsBackToEmailWhenNameIsMissing() {
        UUID plainId = UUID.randomUUID();
        givenOwner(UUID.randomUUID());
        givenUser(plainId, "sansnom@exemple.cm", null, null);

        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership(plainId, "MEMBRE", Instant.now())));

        List<Map<String, Object>> promotable = service.promotableMembers(tenantId);

        assertEquals(1, promotable.size());
        assertEquals("sansnom@exemple.cm", promotable.get(0).get("name"),
                "l'admin doit pouvoir identifier la personne sans deviner");
    }

    @Test
    @DisplayName("Aucun compte lisible → membre omis plutôt qu'une entrée fantôme")
    void skipsMembershipsWhoseAccountIsUnreadable() {
        UUID ghostId = UUID.randomUUID();
        givenOwner(UUID.randomUUID());
        when(userRepository.findById(ghostId)).thenReturn(Optional.empty());

        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership(ghostId, "MEMBRE", Instant.now())));

        assertTrue(service.promotableMembers(tenantId).isEmpty(),
                "une ligne sans identifiant ni libellé rendrait le sélecteur inutilisable");
    }

    @Test
    @DisplayName("Église sans propriétaire désigné : on ne peut pas exclire le propriétaire, "
            + "mais on exclut toujours les délégués")
    void withoutOwnerStillExcludesExistingAdmins() {
        UUID adminId = UUID.randomUUID();
        givenUser(adminId, "admin@exemple.cm", "Ad", "Min");

        // Aucun propriétaire : la requête de propriétaire ne renvoie rien.
        when(membershipRepository.findByTenantIdAndStatusAndRoleContaining(any(), any(), any()))
                .thenReturn(List.of());
        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership(adminId, "TENANT_ADMIN", Instant.now())));

        assertTrue(service.promotableMembers(tenantId).isEmpty());
    }

    @Test
    @DisplayName("Aucun membre ordinaire : liste vide, pas d'erreur — l'écran affiche un sélecteur vide")
    void emptyChurchYieldsEmptyList() {
        givenOwner(UUID.randomUUID());
        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of());

        assertTrue(service.promotableMembers(tenantId).isEmpty());
    }

    @Test
    @DisplayName("D7 : la réponse ne contient aucune donnée hors du tenant courant")
    void doesNotLeakAcrossTenants() {
        UUID plainId = UUID.randomUUID();
        givenOwner(UUID.randomUUID());
        givenUser(plainId, "membre@exemple.cm", "Jean", "Kouassi");

        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership(plainId, "MEMBRE", Instant.now())));

        List<Map<String, Object>> promotable = service.promotableMembers(tenantId);
        assertEquals(1, promotable.size());
        for (Map<String, Object> row : promotable) {
            assertFalse(row.containsKey("tenantId"),
                    "le client connaît déjà son tenant : l'exposer n'apporte rien et "
                            + "facilite le contournement d'un garde de portée");
        }
        // Le repository est bien interrogé sur le tenant courant, jamais « tous les tenants ».
        assertFalse(promotable.isEmpty());
    }

    /**
     * Non-testé volontairement : le passage d'un tenant {@code null}.
     *
     * <p>Il ne peut pas survenir — le contrôleur appelle
     * {@code TenantContext.requireTenantId()}, qui lève avant d'arriver ici.
     * Écrire un test imposant au service de rejeter un {@code null} reviendrait
     * à figer un contrat qui n'existe pas et à faire échouer le build le jour
     * où la garde est déplacée. La protection existe, à un seul endroit, et
     * elle est en place.
     */
}
