package com.discipolat.modules.onboarding;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.onboarding.domain.OnboardingWizardRepository;
import com.discipolat.modules.onboarding.domain.OnboardingWizardStep;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantStatus;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Constat B1 + B2 / tâche A11 — isolation du wizard d'onboarding sur la
 * <b>chaîne HTTP réelle</b>.
 *
 * <p>JWT réel → {@code TenantInterceptor} → filtre Hibernate multi-tenant →
 * {@code TenantStatusInterceptor} (suspension, A1) → {@code @PreAuthorize} →
 * service (isolation tenant, A3). Chaque couche est vérifiée sur la même
 * requête : c'est la seule façon de prouver qu'elles ne se compensent pas.
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OnboardingWizardSecurityIT {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TenantMembershipRepository membershipRepository;
    @Autowired private OnboardingWizardRepository wizardRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private com.discipolat.modules.tenants.domain.TenantService tenantService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String adminTokenA;
    private String memberTokenA;
    private String adminTokenB;

    @BeforeEach
    void setUp() {
        ensureTenant(TENANT_A, "wizard-sec-a", TenantStatus.ACTIVE);
        ensureTenant(TENANT_B, "wizard-sec-b", TenantStatus.ACTIVE);
        ensureRole(TENANT_A, "TENANT_OWNER");
        ensureRole(TENANT_A, "MEMBER");
        ensureRole(TENANT_B, "TENANT_OWNER");

        jdbcTemplate.update("DELETE FROM onboarding_wizard_steps WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbcTemplate.update("DELETE FROM tenant_subscriptions WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        ensurePlanAndSubscription(TENANT_A);
        ensurePlanAndSubscription(TENANT_B);

        UUID adminA = saveUser("sec.admin.a@test", TENANT_A);
        UUID memberA = saveUser("sec.member.a@test", TENANT_A);
        UUID adminB = saveUser("sec.admin.b@test", TENANT_B);

        saveMembership(adminA, TENANT_A, "TENANT_OWNER");
        saveMembership(memberA, TENANT_A, "MEMBER");
        saveMembership(adminB, TENANT_B, "TENANT_OWNER");

        adminTokenA = token(adminA, "TENANT_OWNER", TENANT_A);
        memberTokenA = token(memberA, "MEMBER", TENANT_A);
        adminTokenB = token(adminB, "TENANT_OWNER", TENANT_B);
    }

    // ---------- IDOR inter-tenant sur la chaîne HTTP ----------

    @Test
    @DisplayName("[A11] Tenant B ne peut PAS compléter une étape du tenant A (404)")
    void tenantBCannotCompleteStepOfTenantA() throws Exception {
        OnboardingWizardStep stepA = persistStep(TENANT_A,
                OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0);

        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/complete", stepA.getId())
                        .header("Authorization", adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("STEP_NOT_FOUND"))
                // Aucune fuite inter-tenant dans la réponse d'erreur.
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Église"))));
    }

    @Test
    @DisplayName("[A11] Tenant B ne peut PAS démarrer ni sauter une étape du tenant A (404)")
    void tenantBCannotStartNorSkipStepOfTenantA() throws Exception {
        OnboardingWizardStep stepA = persistStep(TENANT_A,
                OnboardingWizardStep.StepType.MEMBER_IMPORT, 1);

        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/start", stepA.getId())
                        .header("Authorization", adminTokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("STEP_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/skip", stepA.getId())
                        .header("Authorization", adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"raison\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("STEP_NOT_FOUND"));
    }

    @Test
    @DisplayName("[A11] Tenant B ne voit QUE ses propres étapes dans GET /")
    void tenantBSeesOnlyItsOwnSteps() throws Exception {
        persistStep(TENANT_A, OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0);
        persistStep(TENANT_B, OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0);

        OnboardingWizardStep onlyStepOfB = wizardRepository
                .findByTenantIdOrderByStepOrderAsc(TENANT_B).get(0);

        mockMvc.perform(get("/api/v1/onboarding-wizard").header("Authorization", adminTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(onlyStepOfB.getId().toString()))
                .andExpect(jsonPath("$[0].stepType").value("CHURCH_IDENTITY"));
    }

    // ---------- RBAC ----------

    @Test
    @DisplayName("[A11] Un membre non-admin ne peut PAS muter le wizard (403)")
    void nonAdminMemberCannotMutate() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding-wizard/initialize")
                        .header("Authorization", memberTokenA))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/onboarding-wizard/{id}/complete", UUID.randomUUID())
                        .header("Authorization", memberTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[A11] Un membre non-admin peut LIRE le wizard (200)")
    void nonAdminMemberCanRead() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding-wizard").header("Authorization", memberTokenA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[A11] Sans jeton : 401")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding-wizard/progress")).andExpect(status().isUnauthorized());
    }

    // ---------- Suspension de tenant (constat B1) sur le wizard ----------

    @Test
    @DisplayName("[A11] Un tenant suspendu ne peut PLUS appeler le wizard (403 TENANT_SUSPENDED)")
    void suspendedTenantCannotCallTheWizard() throws Exception {
        suspend(TENANT_A);

        mockMvc.perform(get("/api/v1/onboarding-wizard").header("Authorization", adminTokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("TENANT_SUSPENDED"));
    }

    @Test
    @DisplayName("[A11] La suspension ne bloque PAS le tenant voisin")
    void suspensionDoesNotLeakToTheOtherTenant() throws Exception {
        suspend(TENANT_A);

        mockMvc.perform(get("/api/v1/onboarding-wizard").header("Authorization", adminTokenB))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[A11] Après réactivation, le wizard est de nouveau accessible")
    void reactivationRestoresAccess() throws Exception {
        suspend(TENANT_A);
        mockMvc.perform(get("/api/v1/onboarding-wizard").header("Authorization", adminTokenA))
                .andExpect(status().isForbidden());

        // Réactivation par le chemin applicatif : l'invalidation du cache est
        // immédiate, sans attendre le TTL de 30 s.
        tenantService.reactivate(TENANT_A);

        mockMvc.perform(get("/api/v1/onboarding-wizard").header("Authorization", adminTokenA))
                .andExpect(status().isOk());
    }

    // ---------- helpers ----------

    /**
     * Suspension par le <b>chemin applicatif reel</b> ({@code TenantService.deactivate}),
     * et non par un UPDATE SQL : c'est lui qui publie
     * {@code TenantStatusChangedEvent} et invalide donc le cache de 30 s de la
     * garde. Un UPDATE SQL laisserait le cache dire « ACTIVE » et le test ne
     * prouverait rien.
     */
    private void suspend(UUID tenantId) {
        tenantService.deactivate(tenantId);
    }

    private OnboardingWizardStep persistStep(UUID tenantId, OnboardingWizardStep.StepType type, int order) {
        OnboardingWizardStep step = new OnboardingWizardStep();
        step.setTenantId(tenantId);
        step.setStepType(type);
        step.setStepOrder(order);
        step.setStatus(OnboardingWizardStep.Status.PENDING);
        return wizardRepository.save(step);
    }

    private void ensureTenant(UUID tenantId, String slug, TenantStatus status) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE id = ?", Integer.class, tenantId);
        if (existing != null && existing > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, status, plan, country, currency, timezone, locale, "
                        + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tenantId, "Eglise " + slug, slug, status.name(), "NETWORK",
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

    private void ensurePlanAndSubscription(UUID tenantId) {
        Integer planExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM saas_plans WHERE key = 'NETWORK'", Integer.class);
        if (planExists == null || planExists == 0) {
            jdbcTemplate.update(
                    "INSERT INTO saas_plans (key, name, description, currency, is_active, is_public, status, "
                            + "sort_order, limits_json, features_json, seats_limit, storage_limit_mb, "
                            + "ai_credits_limit, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    "NETWORK", "Network", "Plan reseau (fixture)", "EUR", true, true, "ACTIVE", 1,
                    LIMITS_JSON, FEATURES_JSON, 2000, 100000, 5000,
                    Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
        }
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenant_subscriptions WHERE tenant_id = ? AND status = 'ACTIVE'",
                Integer.class, tenantId);
        if (existing == null || existing == 0) {
            jdbcTemplate.update(
                    "INSERT INTO tenant_subscriptions (id, tenant_id, plan_key, status, billing_cycle, "
                            + "current_period_start, current_period_end, cancel_at_period_end, created_at, updated_at)"
                            + " VALUES (?,?,?,?,?,?,?,?,?,?)",
                    UUID.randomUUID(), tenantId, "NETWORK", "ACTIVE", "monthly",
                    Timestamp.from(Instant.now()),
                    Timestamp.from(Instant.now().plusSeconds(30L * 24 * 3600)),
                    false, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
        }
        jdbcTemplate.update("UPDATE tenants SET plan = 'NETWORK' WHERE id = ?", tenantId);
    }

    private static final String LIMITS_JSON = "{\"members\":2000,\"max_users\":2000,"
            + "\"max_churches\":100,\"max_departments\":100,\"max_campuses\":50,\"max_groups\":50,"
            + "\"spaces\":100,\"storage_mb\":100000,\"max_storage_mb\":100000,\"events\":1000,"
            + "\"ai_credits\":5000,\"max_ai_requests_month\":5000,\"max_courses\":1000,"
            + "\"max_messages_month\":1000000}";

    private static final String FEATURES_JSON = "{\"people\":true,\"events\":true,\"ai\":true}";

    private UUID saveUser(String email, UUID tenantId) {
        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        return userRepository.save(User.builder()
                .tenantId(tenantId)
                .email(email)
                .passwordHash("PLACEHOLDER")
                .firstName("Securite")
                .lastName("Test")
                .role(com.discipolat.common.domain.UserRole.PASTEUR)
                .roles(Set.of(com.discipolat.common.domain.UserRole.PASTEUR))
                .activeRole(com.discipolat.common.domain.UserRole.PASTEUR)
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

    private String token(UUID userId, String role, UUID tenantId) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                userId, userId + "@test", role, Set.of(role), false, tenantId);
    }
}
