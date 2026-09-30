package com.discipolat.modules.platform.api;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ARBITRAGE D5-bis — le RBAC des invitations est SCOPÉ AU TENANT, plus simple
 * lecture du rôle porté par le JWT.
 *
 * <p>Avant ce correctif, les cinq endpoints d'administration de
 * {@code /api/v1/admin/invitations} étaient gardés par
 * {@code @PreAuthorize("hasAnyRole('TENANT_OWNER','TENANT_ADMIN')")}. Cette
 * expression n'évalue QUE l'autorité issue du claim « role » du jeton
 * (voir {@code JwtAuthenticationFilter}) : un jeton dont le rôle actif est
 * {@code TENANT_ADMIN} franchissait la garde, QUEL QUE SOIT le tenant porté
 * par la requête. Or le modèle prévoit explicitement le multi-appartenance
 * (constat B2) : un admin de l'église A, muni d'un jeton pointant vers
 * l'église B, obtenait la liste des invitations de B et pouvait en créer —
 * fuite d'autorité inter-tenant.
 *
 * <p>Le correctif bascule ces gardes sur {@code @authz.isTenantAdmin()} (même
 * migration déjà appliquée aux mutations de l'assistant d'onboarding), qui ne
 * lit plus le jeton mais la table des appartenances : il exige une membership
 * ACTIVE de portée TENANT dont le rôle figure dans
 * {@code AuthorizationService.TENANT_ADMIN_ROLE_KEYS} POUR LE TENANT COURANT.
 *
 * <p>Le cas discriminant, joué ici sur la chaîne HTTP réelle (JWT signé →
 * TenantInterceptor → {@code @PreAuthorize} → service) : un jeton qui crie
 * {@code TENANT_ADMIN} mais dont le porteur n'AUCUNE appartenance
 * d'administration dans le tenant visé doit être REFUSÉ (403). Le contrôle
 * positif prouve que ce n'est pas un 403 aveugle : le même rôle, adossé à une
 * vraie membership, passe (200).
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class InvitationAdminTenantScopeRbacTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-0000000000f2");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TenantMembershipRepository membershipRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    /** Jeton « TENANT_ADMIN » dont le porteur n'est admin que dans A, pas dans B. */
    private String crossTenantAdminToken;
    /** Jeton « TENANT_ADMIN » dont le porteur a une vraie membership d'admin dans B. */
    private String realAdminInBToken;

    @BeforeEach
    void setUp() {
        ensureActiveTenant(TENANT_A, "eglise-a-rbac");
        ensureActiveTenant(TENANT_B, "eglise-b-rbac");
        ensureRole(TENANT_A, "TENANT_OWNER");
        ensureRole(TENANT_B, "TENANT_ADMIN");

        // Acteur « mutenant » : admin (OWNER) dans A, RIEN dans B. Son jeton
        // porte le rôle actif TENANT_ADMIN (autorité ROLE_TENANT_ADMIN) et le
        // tenant B — le exact profil qui fuitait avant D5-bis.
        UUID multiUser = saveUser("rbac.multi@test", TENANT_A);
        saveMembership(multiUser, TENANT_A, "TENANT_OWNER");
        crossTenantAdminToken = bearerToken(multiUser, "TENANT_ADMIN", TENANT_B);

        // Contrôle positif : admin RÉELLEMENT rattaché à B.
        UUID realAdmin = saveUser("rbac.admin-b@test", TENANT_B);
        saveMembership(realAdmin, TENANT_B, "TENANT_ADMIN");
        realAdminInBToken = bearerToken(realAdmin, "TENANT_ADMIN", TENANT_B);
    }

    @Test
    @DisplayName("LISTER les invitations d'un tenant où l'on n'a pas d'appartenance d'admin = 403")
    void crossTenantAdminCannotListInvitationsOfForeignTenant() throws Exception {
        mockMvc.perform(get("/api/v1/admin/invitations")
                        .header("Authorization", crossTenantAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CRÉER une invitation dans un tenant où l'on n'a pas d'appartenance d'admin = 403")
    void crossTenantAdminCannotCreateInvitationInForeignTenant() throws Exception {
        mockMvc.perform(post("/api/v1/admin/invitations")
                        .header("Authorization", crossTenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"quelqu'un@eglise-b.test\",\"role\":\"MEMBRE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Contrôle positif : un vrai admin du tenant liste ses invitations (200)")
    void realTenantAdminCanListInvitations() throws Exception {
        // Sans ce contrôle, le 403 ci-dessus ne prouverait rien : il pourrait
        // venir d'une garde aveuglément fermée. Ici la même expression SpEL
        // rend l'accès, adossée à une membership ACTIVE d'admin dans B.
        mockMvc.perform(get("/api/v1/admin/invitations")
                        .header("Authorization", realAdminInBToken))
                .andExpect(status().isOk());
    }

    // ==================== HELPERS ====================

    private void ensureActiveTenant(UUID id, String slug) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE id = ?", Integer.class, id);
        if (existing != null && existing > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, status, plan, country, currency, timezone, locale, "
                        + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, "Eglise " + slug, slug, "ACTIVE", "DISCOVERY",
                "CM", "XAF", "Africa/Douala", "fr",
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
    }

    private void ensureRole(UUID tenantId, String roleKey) {
        if (roleRepository.findByTenantIdAndKey(tenantId, roleKey).isPresent()) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO roles (id, tenant_id, key, label, description, system, priority, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), tenantId, roleKey, roleKey, roleKey, true, 500,
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
    }

    private UUID saveUser(String email, UUID originTenant) {
        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        return userRepository.save(User.builder()
                .tenantId(originTenant)
                .email(email)
                .passwordHash("PLACEHOLDER")
                .firstName("RBAC")
                .lastName("Test")
                .role(UserRole.PASTEUR)
                .roles(Set.of(UserRole.PASTEUR))
                .activeRole(UserRole.PASTEUR)
                .statut(UserStatus.ACTIVE)
                .build()).getId();
    }

    private void saveMembership(UUID userId, UUID tenantId, String roleKey) {
        boolean alreadyMember = membershipRepository
                .findAllByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE)
                .stream()
                .anyMatch(m -> m.getScopeType() == MembershipScopeType.TENANT
                        && roleKey.equals(m.getRoleLegacy()));
        if (alreadyMember) {
            return;
        }
        Role role = roleRepository.findByTenantIdAndKey(tenantId, roleKey).orElseThrow();
        membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantId)
                .userId(userId)
                .role(role)
                .roleLegacy(roleKey)
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE)
                .build());
    }

    private String bearerToken(UUID userId, String activeRole, UUID tenantId) {
        String token = jwtTokenProvider.generateAccessToken(
                userId, "rbac-" + activeRole.toLowerCase() + "@test", activeRole,
                Set.of(activeRole), false, tenantId);
        return "Bearer " + token;
    }
}
