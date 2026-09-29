package com.discipolat.modules.people.api;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Qualification du {@code // TODO: authorizationService check} de
 * {@code PeopleService.assignRole} (passe-plat Agent B, HANDOVER P2) : la
 * mutation de {@code RoleAssignment} est-elle réellement gardée ?
 *
 * <p>Réponse apportée par ces tests, sur la <b>chaîne HTTP réelle</b>
 * (JWT → TenantInterceptor → TenantStatusInterceptor → {@code @PreAuthorize}) :
 * le service n'est atteignable que par {@code PeopleController}, dont chaque
 * mutation porte un garde méthode — {@code ADMIN/PASTEUR} pour assignation et
 * clôture de rôle, {@code PASTOR_PRINCIPAL} pour le transfert de pasteur. Le
 * trou n'est donc pas un défaut <i>accessible</i> ; ces tests verrouillent la
 * frontière pour que la qualification reste vraie et vérifiable, et non une
 * confiance verbale.
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PeopleRoleAssignmentRbacTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TenantMembershipRepository membershipRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String membreToken;
    private String departmentLeaderToken;
    private String pasteurToken;
    private String personId;

    @BeforeEach
    void setUp() {
        ensureActiveTenant();
        for (String key : Set.of("MEMBRE", "DEPARTMENT_LEADER", "PASTEUR", "ADMIN", "PASTOR_PRINCIPAL")) {
            ensureRole(key);
        }
        membreToken = tokenFor("people.membre@test", "MEMBRE");
        departmentLeaderToken = tokenFor("people.deptlead@test", "DEPARTMENT_LEADER");
        pasteurToken = tokenFor("people.pasteur@test", "PASTEUR");
        personId = userRepository.findByEmailIgnoreCase("people.membre@test").orElseThrow().getId().toString();
    }

    // ==================================================================
    // POST /{personId}/roles — reserved to ADMIN / PASTEUR
    // ==================================================================

    @Test
    @DisplayName("Un MEMBRE ne peut pas assigner un rôle permanent (403)")
    void membreCannotAssignRole() throws Exception {
        assignRoleCall(membreToken).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un DEPARTMENT_LEADER traverse la garde de classe mais bloque à la garde de méthode (403)")
    void departmentLeaderCannotAssignRole() throws Exception {
        // Le contrôleur entier est guarded hasAnyRole('ADMIN','PASTEUR',
        // 'DEPARTMENT_LEADER','FAMILY_LEADER') au niveau CLASSE ; si la garde
        // de méthode tombait, ce rôle d'encadrement intermédiaire pourrait
        // créer des rôles permanents. Ce test verrouille le niveau méthode.
        assignRoleCall(departmentLeaderToken).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un PASTEUR passe la garde — la demande n'est plus refusée par RBAC")
    void pasteurPassesTheGuard() throws Exception {
        mockMvc.perform(assignRoleRequest(pasteurToken))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("la garde ADMIN/PASTEUR doit laisser passer un PASTEUR")
                        .isNotEqualTo(403));
    }

    // ==================================================================
    // DELETE /roles/{assignmentId} — same boundary
    // ==================================================================

    @Test
    @DisplayName("Un DEPARTMENT_LEADER ne peut pas clôturer un rôle (403)")
    void departmentLeaderCannotEndRole() throws Exception {
        mockMvc.perform(delete("/api/v1/people/roles/{assignmentId}", UUID.randomUUID())
                        .header("Authorization", departmentLeaderToken))
                .andExpect(status().isForbidden());
    }

    // ==================================================================
    // POST /{personId}/transfer — reserved to PASTOR_PRINCIPAL
    // ==================================================================

    @Test
    @DisplayName("Un PASTEUR ne peut pas transférer un pasteur (403) — seul PASTOR_PRINCIPAL le peut")
    void pasteurCannotTransferPastor() throws Exception {
        mockMvc.perform(post("/api/v1/people/{personId}/transfer", personId)
                        .param("newOrgUnitId", UUID.randomUUID().toString())
                        .param("reason", "test de garde")
                        .header("Authorization", pasteurToken))
                .andExpect(status().isForbidden());
    }

    // ==================================================================
    // Contrôle positif : la garde de classe laisse bien passer la lecture
    // ==================================================================

    @Test
    @DisplayName("Un DEPARTMENT_LEADER peut lire les rôles d'une personne (200)")
    void departmentLeaderCanReadRoles() throws Exception {
        mockMvc.perform(get("/api/v1/people/{personId}/roles", personId)
                        .header("Authorization", departmentLeaderToken))
                .andExpect(status().isOk());
    }

    // ========== helpers (même recette que OnboardingWizardControllerTest) ==========

    private org.springframework.test.web.servlet.ResultActions assignRoleCall(String token) throws Exception {
        return mockMvc.perform(assignRoleRequest(token));
    }

    private org.springframework.test.web.servlet.RequestBuilder assignRoleRequest(String token) {
        return post("/api/v1/people/{personId}/roles", personId)
                .param("roleId", UUID.randomUUID().toString())
                .header("Authorization", token);
    }

    private String tokenFor(String email, String roleKey) {
        UUID userId = saveUser(email);
        saveMembership(userId, roleKey);
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                userId, email, roleKey, Set.of(roleKey), false, TENANT);
    }

    private void ensureActiveTenant() {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE id = ?", Integer.class, TENANT);
        if (existing != null && existing > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, status, plan, country, currency, timezone, locale, "
                        + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                TENANT, "Eglise du peuple", "people-eglise-rbac", "ACTIVE", "DISCOVERY",
                "CM", "XAF", "Africa/Douala", "fr",
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
    }

    private void ensureRole(String roleKey) {
        if (roleRepository.findByTenantIdAndKey(TENANT, roleKey).isPresent()) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO roles (id, tenant_id, key, label, description, system, priority, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), TENANT, roleKey, roleKey, roleKey, true, 500,
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
    }

    private UUID saveUser(String email) {
        return userRepository.findByEmailIgnoreCase(email).map(User::getId).orElseGet(() ->
                userRepository.save(User.builder()
                        .tenantId(TENANT)
                        .email(email)
                        .passwordHash("PLACEHOLDER")
                        .firstName("People")
                        .lastName("Rbac")
                        .role(UserRole.PASTEUR)
                        .roles(Set.of(UserRole.PASTEUR))
                        .activeRole(UserRole.PASTEUR)
                        .statut(UserStatus.ACTIVE)
                        .build()).getId());
    }

    private void saveMembership(UUID userId, String roleKey) {
        boolean alreadyMember = membershipRepository
                .findAllByUserIdAndTenantIdAndStatus(userId, TENANT, MembershipStatus.ACTIVE)
                .stream()
                .anyMatch(m -> m.getScopeType() == MembershipScopeType.TENANT
                        && roleKey.equals(m.getRoleLegacy()));
        if (alreadyMember) {
            return;
        }
        Role role = roleRepository.findByTenantIdAndKey(TENANT, roleKey).orElseThrow();
        membershipRepository.save(TenantMembership.builder()
                .tenantId(TENANT)
                .userId(userId)
                .role(role)
                .roleLegacy(roleKey)
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE)
                .build());
    }
}
