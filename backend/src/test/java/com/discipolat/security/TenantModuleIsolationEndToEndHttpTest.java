package com.discipolat.security;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.payments.domain.PaymentIntent;
import com.discipolat.modules.payments.domain.PaymentIntentRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.domain.TenantSettingsRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import com.discipolat.support.DatabaseReset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A5 #6 — ISOLATION MULTI-TENANT BOUT-EN-BOUT, CHAQUE MODULE DU TENANT B.
 *
 * <p>C'est le test qui protège contre le risque juridique n°1 du produit :
 * une église qui lirait les données d'une autre. Contrairement au test
 * historique {@code TenantIsolationIntegrationTest} (tokens factices → 401
 * générique, qui ne prouve que l'authentification), ici chaque requête est
 * portée par un JWT RÉELLEMENT signé (claim tenantId) d'un utilisateur
 * authentifié du tenant A, ciblant l'identifiant d'une ressource qui EXISTE
 * et appartient RÉELLEMENT au tenant B : le 404 obtenu ne peut donc venir
 * que de la garde d'isolation (TenantAwareSimpleJpaRepository.findById +
 * filtre Hibernate), jamais d'une simple absence de donnée.
 *
 * <p>Pour chaque module, double épreuve :
 * <ol>
 *   <li>tenant A → ressource de B = 403/404 (refus, sans fuite d'existence) ;</li>
 *   <li>tenant B → sa propre ressource = 200 (contrôle positif : la ressource
 *       est bien là, seule les clés étrangères du tenant la protègent).</li>
 * </ol>
 *
 * <p>Périmètre exigé : souls, families, departments, events, reports,
 * payments, users, settings, backups. Plus l'usurpation d'en-tête
 * {@code X-Tenant-Id} : le JWT prime, l'en-tête ne déplace jamais la
 * frontière de tenant.
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("A5 #6 — Isolation multi-tenant HTTP bout-en-bout (9 modules)")
class TenantModuleIsolationEndToEndHttpTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SoulRepository soulRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private MakerReportRepository makerReportRepository;
    @Autowired private PaymentIntentRepository paymentIntentRepository;
    @Autowired private TenantSettingsRepository tenantSettingsRepository;

    /** Ressources du tenant B, créées à chaque test (le contrôle positif prouve qu'elles existent). */
    private UUID adminAId;
    private UUID soulBId;
    private UUID familyBId;
    private UUID departmentBId;
    private UUID eventBId;
    private UUID reportBId;
    private UUID paymentBId;
    private UUID userBId;

    @BeforeEach
    void seedTwoChurches() {
        DatabaseReset.truncate(jdbcTemplate,
                "soul_history", "soul_departments", "soul_notes", "soul_tags",
                "souls", "families", "departments", "event", "maker_reports",
                "payment_intents", "users", "user_roles", "tenant_settings");

        ensureActiveTenant(TENANT_A);
        ensureActiveTenant(TENANT_B);

        adminAId = saveUser("admin.a@eglise-a.test", TENANT_A);
        UUID adminBId = saveUser("admin.b@eglise-b.test", TENANT_B);
        userBId = adminBId;

        // ---- Une ressource RÉELLE par module, uniquement côté tenant B ----
        soulBId = soulRepository.save(Soul.builder()
                .tenantId(TENANT_B).nom("Bob").prenom("Fidèle")
                .email("bob@eglise-b.test")
                .typeDisciple(TypeDisciple.NOUVEL_ARRIVANT).dateIntegration(LocalDate.now())
                .statut(StatutAme.ACTIF).faiseurId(adminBId)
                .build()).getId();

        familyBId = familyRepository.save(Family.builder()
                .tenantId(TENANT_B).nom("Famille Bob").chefFamilleId(adminBId)
                .dateCreation(LocalDate.now()).statut(com.discipolat.common.enums.StatutEntite.ACTIVE)
                .build()).getId();

        departmentBId = departmentRepository.save(Department.builder()
                .tenantId(TENANT_B).nom("Louange B").responsableId(adminBId)
                .statut(com.discipolat.common.enums.StatutEntite.ACTIVE)
                .build()).getId();

        eventBId = eventRepository.save(Event.builder()
                .tenantId(TENANT_B).titre("Retraite B").typeEvenement("RETRAITE")
                .organisateurId(adminBId).dateDebut(LocalDateTime.now().plusDays(7))
                .statut("PLANIFIE")
                .build()).getId();

        reportBId = makerReportRepository.save(MakerReport.builder()
                .tenantId(TENANT_B).faiseurId(adminBId).ameId(soulBId)
                .semaine(LocalDate.now().with(java.time.DayOfWeek.MONDAY))
                .build()).getId();

        paymentBId = paymentIntentRepository.save(PaymentIntent.builder()
                .tenantId(TENANT_B).userId(adminBId)
                .operator(PaymentIntent.Operator.M_PESA)
                .amount(new BigDecimal("2500.00")).currency("KES")
                .purpose(PaymentIntent.Purpose.DIME).status(PaymentIntent.Status.PENDING)
                .build()).getId();

        // Réglages propres à chaque tenant : le contrôle croisé prouve que
        // /admin/settings résout le tenant depuis le JWT, jamais en travers.
        saveSettings(TENANT_A, "Église Alpha (A)");
        saveSettings(TENANT_B, "Église Bêta (B)");
    }

    private void saveSettings(UUID tenantId, String businessName) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("tenant manquant " + tenantId));
        tenantSettingsRepository.save(TenantSettings.builder()
                .tenant(tenant).businessName(businessName)
                .build());
    }

    private UUID saveUser(String email, UUID tenantId) {
        return userRepository.save(User.builder()
                .tenantId(tenantId).email(email).passwordHash("PLACEHOLDER")
                .firstName("Admin").lastName("Isolation")
                .role(UserRole.ADMIN).roles(Set.of(UserRole.ADMIN)).activeRole(UserRole.ADMIN)
                .statut(UserStatus.ACTIVE)
                .build()).getId();
    }

    private String bearer(UUID userId, UUID tenantId) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                userId, "admin@isolation.test", "ADMIN", Set.of("ADMIN"), false, tenantId);
    }

    /** Jeton de l'admin authentifié du tenant A (sujet = utilisateur réellement créé). */
    private String tokenA() {
        return bearer(adminAId, TENANT_A);
    }

    /** Jeton de l'admin authentifié du tenant B (sujet = utilisateur réellement créé). */
    private String tokenB() {
        return bearer(userBId, TENANT_B);
    }

    /* ==================================================================== */
    /* SOULS                                                                */
    /* ==================================================================== */

    @Test
    @DisplayName("souls : A authentifié sur l'âme de B → 404 ; B lit la sienne → 200")
    void souls_isolationEtControlePositif() throws Exception {
        mockMvc.perform(get("/api/v1/souls/" + soulBId)
                        .header("Authorization", tokenA()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/souls/" + soulBId)
                        .header("Authorization", tokenB()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(soulBId.toString()));
    }

    /* ==================================================================== */
    /* FAMILIES                                                             */
    /* ==================================================================== */

    @Test
    @DisplayName("families : A authentifié sur la famille de B → 404 ; B lit la sienne → 200")
    void families_isolationEtControlePositif() throws Exception {
        mockMvc.perform(get("/api/v1/families/" + familyBId)
                        .header("Authorization", tokenA()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/families/" + familyBId)
                        .header("Authorization", tokenB()))
                .andExpect(status().isOk());
    }

    /* ==================================================================== */
    /* DEPARTMENTS                                                          */
    /* ==================================================================== */

    @Test
    @DisplayName("departments : A authentifié sur le département de B → 404 ; B lit le sien → 200")
    void departments_isolationEtControlePositif() throws Exception {
        mockMvc.perform(get("/api/v1/departments/" + departmentBId)
                        .header("Authorization", tokenA()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/departments/" + departmentBId)
                        .header("Authorization", tokenB()))
                .andExpect(status().isOk());
    }

    /* ==================================================================== */
    /* EVENTS                                                               */
    /* ==================================================================== */

    @Test
    @DisplayName("events : A authentifié sur l'événement de B → 404 ; B lit le sien → 200")
    void events_isolationEtControlePositif() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + eventBId)
                        .header("Authorization", tokenA()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/events/" + eventBId)
                        .header("Authorization", tokenB()))
                .andExpect(status().isOk());
    }

    /* ==================================================================== */
    /* REPORTS                                                              */
    /* ==================================================================== */

    @Test
    @DisplayName("reports : A authentifié sur le rapport de B → 404 ; B lit le sien → 200")
    void reports_isolationEtControlePositif() throws Exception {
        mockMvc.perform(get("/api/v1/reports/maker-weekly/" + reportBId)
                        .header("Authorization", tokenA()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/reports/maker-weekly/" + reportBId)
                        .header("Authorization", tokenB()))
                .andExpect(status().isOk());
    }

    /* ==================================================================== */
    /* PAYMENTS                                                             */
    /* ==================================================================== */

    @Test
    @DisplayName("payments : A authentifié sur le paiement de B → 404 sans fuite d'existence ; B → 200")
    void payments_isolationEtControlePositif() throws Exception {
        mockMvc.perform(get("/api/v1/payments/" + paymentBId)
                        .header("Authorization", tokenA()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/payments/" + paymentBId)
                        .header("Authorization", tokenB()))
                .andExpect(status().isOk());
    }

    /* ==================================================================== */
    /* USERS                                                                */
    /* ==================================================================== */

    @Test
    @DisplayName("users : A authentifié sur le compte utilisateur de B → 404 ; B → 200")
    void users_isolationEtControlePositif() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + userBId)
                        .header("Authorization", tokenA()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/users/" + userBId)
                        .header("Authorization", tokenB()))
                .andExpect(status().isOk());
    }

    /* ==================================================================== */
    /* SETTINGS                                                             */
    /* ==================================================================== */

    @Test
    @DisplayName("settings : chaque tenant ne lit QUE ses réglages (résolus depuis le JWT)")
    void settings_chaqueTenantLitLesSiens() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings")
                        .header("Authorization", tokenA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Église Alpha (A)"))
                .andExpect(jsonPath("$.tenantId").value(TENANT_A.toString()));
        mockMvc.perform(get("/api/v1/admin/settings")
                        .header("Authorization", tokenB()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Église Bêta (B)"))
                .andExpect(jsonPath("$.tenantId").value(TENANT_B.toString()));
    }

    /* ==================================================================== */
    /* BACKUPS                                                              */
    /* ==================================================================== */

    @Test
    @DisplayName("backups : un admin de tenant ne peut ni lister ni lire d'archives → 403")
    void backups_inaccessiblesAuxTenants() throws Exception {
        // Garde RBAC plateforme (@authz.isPlatformSuperAdmin) : un simple admin
        // de tenant ne voit AUCUNE archive, celles d'un autre tenant a fortiori.
        // L'isolation des données d'archive elles-mêmes (find/verify/delete
        // cross-tenant → 404 fail-closed) est prouvée dans
        // BackupServiceTest et IsolationBackupCurrencyTest.
        mockMvc.perform(get("/api/v1/backups")
                        .header("Authorization", tokenA()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/backups/" + UUID.randomUUID())
                        .header("Authorization", tokenA()))
                .andExpect(status().isForbidden());
    }

    /* ==================================================================== */
    /* USURPATION D'EN-TÊTE X-Tenant-Id                                     */
    /* ==================================================================== */

    @Test
    @DisplayName("X-Tenant-Id frauduleux ne déplace jamais la frontière : le JWT prime")
    void enTeteTenantNePeutPasUsurperLeJwt() throws Exception {
        // A tente de se faire passer pour B via l'en-tête : la ressource de B
        // doit rester inaccessible (le tenant vient du JWT signé, pas de l'en-tête).
        mockMvc.perform(get("/api/v1/souls/" + soulBId)
                        .header("Authorization", tokenA())
                        .header("X-Tenant-Id", TENANT_B.toString()))
                .andExpect(status().isNotFound());
    }

    /**
     * Constat B1 : la garde de statut de tenant lit la table `tenants` (globale)
     * en fail-closed. Insertion en SQL direct, comme dans le test d'isolation
     * historique (Hibernate 6 et les entités à id attribué).
     */
    private void ensureActiveTenant(UUID tenantId) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE id = ?", Integer.class, tenantId);
        if (existing != null && existing > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, status, plan, country, currency, timezone, locale, "
                        + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tenantId,
                "Eglise de test " + tenantId,
                "iso-" + tenantId.toString().substring(0, 8),
                "ACTIVE", "DISCOVERY", "CM", "XAF", "Africa/Douala", "fr",
                java.sql.Timestamp.from(Instant.now()),
                java.sql.Timestamp.from(Instant.now()));
    }
}
