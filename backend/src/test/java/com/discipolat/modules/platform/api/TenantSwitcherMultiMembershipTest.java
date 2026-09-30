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
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ARBITRAGE D2 — la lecture de sa PROPRE identite ne doit pas dependre du
 * tenant courant.
 *
 * <p>{@code users.tenant_id} porte le tenant d'ORIGINE (une seule valeur), alors
 * que {@code TenantContext} porte le tenant d'ACTION. Le {@code findById} de
 * {@code TenantAwareSimpleJpaRepository} ajoute {@code AND tenant_id =
 * TenantContext}. Pour un utilisateur multi-appartenance (constat B2) deja
 * bascule dans un tenant qui n'est pas son origine, cette lecture — utilisee par
 * {@code POST /tenant-switcher/switch} pour re-emettre un jeton — renvoyait vide
 * et levait un 500 « Utilisateur introuvable » : impossible d'enchaîner deux
 * bascules (A->B puis B->C).
 *
 * <p>Le correctif lit la propre identite du sujet authentifie via la lecture
 * cross-tenant declaree, BORNEE a l'userId du principal. Ce test rejoue la
 * bascule en chaine sur la chaîne HTTP réelle : la seconde bascule, partie d'un
 * contexte B alors que l'origine est A, doit reussir (200) et non plus 500.
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TenantSwitcherMultiMembershipTest {

    private static final UUID ORIGIN = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-0000000000d2");
    private static final UUID THIRD = UUID.fromString("00000000-0000-0000-0000-0000000000d3");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TenantMembershipRepository membershipRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String originToken;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ensureActiveTenant(ORIGIN, "chain-origin");
        ensureActiveTenant(SECOND, "chain-second");
        ensureActiveTenant(THIRD, "chain-third");
        ensureRole(ORIGIN, "MEMBRE");
        ensureRole(SECOND, "MEMBRE");
        ensureRole(THIRD, "MEMBRE");

        // Un seul utilisateur, rattache (membership ACTIVE) aux trois eglises ;
        // son tenant d'ORIGINE (users.tenant_id) reste `ORIGIN`.
        userId = saveUser("chain.multi@test", ORIGIN);
        saveMembership(userId, ORIGIN, "MEMBRE");
        saveMembership(userId, SECOND, "MEMBRE");
        saveMembership(userId, THIRD, "MEMBRE");

        originToken = bearerToken(userId, "MEMBRE", ORIGIN);
    }

    @Test
    @DisplayName("Bascule en chaine A->B->C : la 2e bascule (contexte != origine) ne leve plus 500")
    void chainedSwitchFromNonOriginContextSucceeds() throws Exception {
        // Premiere bascule : contexte courant == origine. Passe deja avant le
        // correctif ; on s'en sert pour obtenir un jeton positionne sur SECOND.
        MvcResult first = mockMvc.perform(post("/api/v1/tenant-switcher/switch")
                        .header("Authorization", originToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + SECOND + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tenantId").value(SECOND.toString()))
                .andReturn();

        String tokenInSecond = "Bearer " + accessTokenOf(first);

        // Seconde bascule : le contexte est desormais SECOND, mais l'origine de
        // l'utilisateur est ORIGIN. `findById` sous `tenant_id = SECOND` ne
        // trouvait plus la ligne (elle porte tenant_id = ORIGIN) -> 500 avant D2.
        mockMvc.perform(post("/api/v1/tenant-switcher/switch")
                        .header("Authorization", tokenInSecond)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + THIRD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tenantId").value(THIRD.toString()));
    }

    // ==================== HELPERS ====================

    @SuppressWarnings("unchecked")
    private String accessTokenOf(MvcResult result) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        Object token = body.get("accessToken");
        if (token == null) {
            throw new IllegalStateException("pas d'accessToken dans la reponse de bascule");
        }
        return token.toString();
    }

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
        return userRepository.findByEmailIgnoreCase(email)
                .map(User::getId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .tenantId(originTenant)
                        .email(email)
                        .passwordHash("PLACEHOLDER")
                        .firstName("Chain")
                        .lastName("Test")
                        .role(UserRole.MEMBRE)
                        .roles(Set.of(UserRole.MEMBRE))
                        .activeRole(UserRole.MEMBRE)
                        .statut(UserStatus.ACTIVE)
                        .build()).getId());
    }

    private void saveMembership(UUID user, UUID tenantId, String roleKey) {
        boolean alreadyMember = membershipRepository
                .findAllByUserIdAndTenantIdAndStatus(user, tenantId, MembershipStatus.ACTIVE)
                .stream()
                .anyMatch(m -> m.getScopeType() == MembershipScopeType.TENANT
                        && roleKey.equals(m.getRoleLegacy()));
        if (alreadyMember) {
            return;
        }
        Role role = roleRepository.findByTenantIdAndKey(tenantId, roleKey).orElseThrow();
        membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantId)
                .userId(user)
                .role(role)
                .roleLegacy(roleKey)
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE)
                .build());
    }

    private String bearerToken(UUID user, String activeRole, UUID tenantId) {
        String token = jwtTokenProvider.generateAccessToken(
                user, "chain-" + activeRole.toLowerCase() + "@test", activeRole,
                Set.of(activeRole), false, tenantId);
        return "Bearer " + token;
    }
}
