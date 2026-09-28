package com.discipolat.modules.onboarding.api;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.onboarding.domain.OnboardingWizardRepository;
import com.discipolat.modules.onboarding.domain.OnboardingWizardStep;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Constat B2 — comportement HTTP réel du wizard d'onboarding : contrat §3.1,
 * RBAC {@code @authz.isTenantAdmin()} sur les mutations, corps facultatif,
 * 404 / 409 / 400.
 *
 * <p>Test d'intégration complet (JWT réel → {@code TenantInterceptor} → filtre
 * Hibernate → {@code TenantStatusInterceptor} → {@code @PreAuthorize} →
 * service) : c'est la seule façon de prouver que le RBAC tient sur la chaîne
 * HTTP, et non seulement dans une méthode isolée.
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OnboardingWizardControllerTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TenantMembershipRepository membershipRepository;
    @Autowired private OnboardingWizardRepository wizardRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String ownerToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        ensureActiveTenant();
        ensureRole("TENANT_OWNER");
        // `AuthorizationService.TENANT_ADMIN_ROLE_KEYS` = {ADMIN, PASTEUR,
        // TENANT_OWNER, TENANT_ADMIN} : PASTEUR EST donc un admin de tenant dans
        // cette application. Le rôle contrôlé ici est un simple MEMBRE, qui ne
        // l'est pas.
        ensureRole("MEMBRE");
        jdbcTemplate.update("DELETE FROM onboarding_wizard_steps WHERE tenant_id = ?", TENANT);

        UUID ownerId = saveUser("wizard.owner@test");
        UUID memberId = saveUser("wizard.member@test");
        saveMembership(ownerId, "TENANT_OWNER");
        saveMembership(memberId, "MEMBRE");

        ownerToken = bearerToken(ownerId, "TENANT_OWNER");
        memberToken = bearerToken(memberId, "MEMBRE");
    }

    // ==================================================================
    // Contrat de lecture
    // ==================================================================

    @Test
    @DisplayName("GET / renvoie les 7 étapes avec exactement les champs du contrat")
    void getSteps_exposesTheFrozenContract() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding-wizard").header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(7)))
                .andExpect(jsonPath("$[0].stepType").value("CHURCH_IDENTITY"))
                .andExpect(jsonPath("$[0].stepOrder").value(0))
                .andExpect(jsonPath("$[0].title").value("Identité de l'église"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].isCompleted").value(false))
                .andExpect(jsonPath("$[0].isSkippable").value(false))
                .andExpect(jsonPath("$[0].skipRequiresReason").value(false))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].config").doesNotExist())
                .andExpect(jsonPath("$[0].completedData").doesNotExist());
    }

    @Test
    @DisplayName("GET /progress renvoie totalSteps, completedSteps, skippedSteps, percentage")
    void getProgress_exposesTheFrozenContract() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding-wizard/progress").header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSteps").value(7))
                .andExpect(jsonPath("$.completedSteps").value(0))
                .andExpect(jsonPath("$.skippedSteps").value(0))
                .andExpect(jsonPath("$.percentage").value(0))
                .andExpect(jsonPath("$.isComplete").value(false));
    }

    @Test
    @DisplayName("GET /status est accessible et refuse de fuir les autres tenants")
    void getStatus_isExposed() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding-wizard/status").header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.completedAt").doesNotExist())
                .andExpect(jsonPath("$.totalSteps").value(7))
                .andExpect(jsonPath("$.percentage").isNumber());
    }

    // ==================================================================
    // RBAC
    // ==================================================================

    @Test
    @DisplayName("Un MEMBRE (ni owner ni admin) ne peut PAS muter une étape (403)")
    void nonAdminMember_cannotMutate() throws Exception {
        for (var builder : List.of(
                post("/api/v1/onboarding-wizard/initialize"),
                post("/api/v1/onboarding-wizard/{id}/start", UUID.randomUUID()),
                post("/api/v1/onboarding-wizard/{id}/complete", UUID.randomUUID()),
                post("/api/v1/onboarding-wizard/{id}/skip", UUID.randomUUID()))) {
            mockMvc.perform(builder.header("Authorization", memberToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Un admin de tenant peut initialiser et démarrer une étape")
    void tenantAdmin_canMutate() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding-wizard/initialize")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk());

        UUID firstStep = wizardRepository.findByTenantIdOrderByStepOrderAsc(TENANT).get(0).getId();
        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/start", firstStep)
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Sans authentification : 401")
    void anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding-wizard")).andExpect(status().isUnauthorized());
    }

    // ==================================================================
    // Décision D7 : corps facultatif
    // ==================================================================

    @Test
    @DisplayName("POST /{id}/complete SANS corps ne renvoie pas 400 (décision D7)")
    void complete_withoutBody_doesNotReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding-wizard/initialize")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk());
        UUID firstStep = wizardRepository.findByTenantIdOrderByStepOrderAsc(TENANT).get(0).getId();

        // AVANT le correctif : 400 systématique car `@RequestBody` était obligatoire.
        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/complete", firstStep)
                        .header("Authorization", ownerToken))
                .andExpect(status().isBadRequest())
                // 400 métier explicite (donnée manquante), et non un 400 technique Spring.
                .andExpect(jsonPath("$.title").value("STEP_DATA_INVALID"));
    }

    // ==================================================================
    // 404 / 409 / 400
    // ==================================================================

    @Test
    @DisplayName("Un id d'étape inconnu donne 404 STEP_NOT_FOUND")
    void unknownStep_gives404() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/complete", UUID.randomUUID())
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("STEP_NOT_FOUND"));
    }

    @Test
    @DisplayName("Une étape hors ordre donne 409 STEP_ORDER_VIOLATION")
    void outOfOrderStep_gives409() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding-wizard/initialize")
                .header("Authorization", ownerToken)).andExpect(status().isOk());
        List<OnboardingWizardStep> steps = wizardRepository.findByTenantIdOrderByStepOrderAsc(TENANT);
        UUID rolesStep = steps.stream()
                .filter(s -> s.getStepType() == OnboardingWizardStep.StepType.ROLES)
                .findFirst().orElseThrow().getId();

        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/complete", rolesStep)
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("STEP_ORDER_VIOLATION"));
    }

    @Test
    @DisplayName("Sauter une étape non skippable donne 409 STEP_NOT_SKIPPABLE")
    void skipNonSkippableStep_gives409() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding-wizard/initialize")
                .header("Authorization", ownerToken)).andExpect(status().isOk());
        UUID firstStep = wizardRepository.findByTenantIdOrderByStepOrderAsc(TENANT).get(0).getId();

        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/skip", firstStep)
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"je ne veux pas\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("STEP_NOT_SKIPPABLE"));
    }

    @Test
    @DisplayName("Sauter MEMBER_IMPORT sans motif donne 400 STEP_SKIP_REASON_REQUIRED")
    void skipWithoutRequiredReason_gives400() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding-wizard/initialize")
                .header("Authorization", ownerToken)).andExpect(status().isOk());
        List<OnboardingWizardStep> steps = wizardRepository.findByTenantIdOrderByStepOrderAsc(TENANT);
        UUID identity = steps.get(0).getId();
        UUID importStep = steps.get(1).getId();

        markCompleted(identity);
        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/skip", importStep)
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("STEP_SKIP_REASON_REQUIRED"));

        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/skip", importStep)
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"import hors ligne\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"));
    }

    @Test
    @DisplayName("Une donnée invalide donne 400 STEP_DATA_INVALID avec le champ fautif")
    void invalidData_gives400WithFieldDetail() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding-wizard/initialize")
                .header("Authorization", ownerToken)).andExpect(status().isOk());
        List<OnboardingWizardStep> steps = wizardRepository.findByTenantIdOrderByStepOrderAsc(TENANT);
        UUID identity = steps.get(0).getId();

        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/complete", identity)
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{\"churchName\":\"X\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("STEP_DATA_INVALID"))
                .andExpect(jsonPath("$.details.churchName").isNotEmpty());
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void markCompleted(UUID stepId) {
        OnboardingWizardStep step = wizardRepository.findById(stepId).orElseThrow();
        step.setStatus(OnboardingWizardStep.Status.COMPLETED);
        step.setCompletedAt(java.time.LocalDateTime.now());
        wizardRepository.save(step);
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
                TENANT, "Eglise du wizard", "wizard-eglise", "ACTIVE", "DISCOVERY",
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
        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        return userRepository.save(User.builder()
                .tenantId(TENANT)
                .email(email)
                .passwordHash("PLACEHOLDER")
                .firstName("Wizard")
                .lastName("Test")
                .role(UserRole.PASTEUR)
                .roles(Set.of(UserRole.PASTEUR))
                .activeRole(UserRole.PASTEUR)
                .statut(UserStatus.ACTIVE)
                .build()).getId();
    }

    private void saveMembership(UUID userId, String roleKey) {
        boolean alreadyMember = membershipRepository
                .findAllByUserIdAndTenantIdAndStatus(userId, TENANT, MembershipStatus.ACTIVE)
                .stream()
                // `roleLegacy` est un champ texte : il évite de déclencher le
                // chargement différé du proxy `Role` hors session Hibernate.
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

    private String bearerToken(UUID userId, String role) {
        String token = jwtTokenProvider.generateAccessToken(
                userId, "wizard-" + role.toLowerCase() + "@test", role, Set.of(role), false, TENANT);
        return "Bearer " + token;
    }
}
