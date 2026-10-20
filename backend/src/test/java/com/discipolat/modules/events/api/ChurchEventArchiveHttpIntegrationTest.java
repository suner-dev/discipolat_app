package com.discipolat.modules.events.api;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.test.TestPermissionMatrix;
import com.discipolat.modules.events.domain.ChurchEventArchive;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.repository.ChurchEventArchiveRepository;
import com.discipolat.modules.events.repository.ChurchEventRepository;
import com.discipolat.modules.events.service.ChurchEventService;
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
import com.discipolat.support.DatabaseReset;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V272 — contrat HTTP complet du parcours « Archives » (section C du fichier
 * {@code TODO_ARCHIVES_V272.md}), sur la chaîne réelle
 * JWT → TenantInterceptor → {@code @PreAuthorize('@authz.can(…)')} → service.
 *
 * <p>Sont verrouillés ici, endpoint par endpoint :
 * <ul>
 *   <li>POST /{eventId}/archive — 200 + version incrémentée à la re-clôture ;</li>
 *   <li>GET /archives (legacy, instantanés complets) + filtre {@code eventId} ;</li>
 *   <li>GET /archives/search — paginé, résumés SANS {@code snapshotJson}
 *       (la borne anti-téléchargement intégral décidée par l'audit) ;</li>
 *   <li>GET /archives/facets — années/mois réellement archivés ;</li>
 *   <li>GET /archives/{id} — détail, 404 inter-tenant ;</li>
 *   <li>403 sans la clé EVENTS_EXECUTE_R6A2 dans la matrice servie ;</li>
 *   <li>isolation stricte entre deux tenants ;</li>
 *   <li>clôtures concurrentes → versions uniques (verrou pessimiste +
 *       index unique {@code uq_event_archive_event_version}) ;</li>
 *   <li>GET /church-events?status= enfin appliqué ;</li>
 *   <li>PUT refusant le statut ARCHIVED forgé (400 — POST /archive est
 *       l'unique producteur d'archive).</li>
 * </ul>
 *
 * <p>Recette d'environnement identique à {@code PeopleRoleAssignmentRbacTest} :
 * rôles locaux homonymes + rejouage de l'héritage global ({@code
 * TestPermissionMatrix.INHERIT_GLOBAL_GRANTS_H2}) pour que le pont
 * d'équivalence {@code @authz.can} résolve la vraie matrice V258/V272.
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ChurchEventArchiveHttpIntegrationTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

    @Autowired private MockMvc mockMvc;
    @Autowired private com.discipolat.common.infrastructure.security.JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TenantMembershipRepository membershipRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ChurchEventRepository churchEventRepository;
    @Autowired private ChurchEventArchiveRepository archiveRepository;
    @Autowired private ChurchEventService churchEventService;

    private String pasteurTokenA;
    private String deptLeaderTokenA;
    private String membreTokenA;
    private String pasteurTokenB;
    private UUID actorA;

    @BeforeEach
    void setUp() {
        DatabaseReset.truncate(jdbcTemplate,
                "event_archive", "event_space", "event_team", "event_task", "event_schedule",
                "event_asset", "event_expense", "event_document", "event_attendance", "event");
        ensureActiveTenant(TENANT_A, "eglise-archives-a");
        ensureActiveTenant(TENANT_B, "eglise-archives-b");
        for (String key : Set.of("PASTEUR", "DEPARTMENT_LEADER", "MEMBRE")) {
            ensureRole(TENANT_A, key);
        }
        ensureRole(TENANT_B, "PASTEUR");
        // Les rôles LOCAUX ombragent les globaux seedés par import.sql : sans ce
        // rejouage de l'héritage production (V259 §5 / V260 §2), le pont
        // d'équivalence résoudrait des rôles sans capacités → 403 factices.
        jdbcTemplate.update(TestPermissionMatrix.INHERIT_GLOBAL_GRANTS_H2);

        actorA = saveUser("archives.pasteur@a.test");
        pasteurTokenA = tokenFor(actorA, "PASTEUR", TENANT_A);
        saveMembership(actorA, TENANT_A, "PASTEUR");
        deptLeaderTokenA = tokenFor(saveUser("archives.deptlead@a.test"), "DEPARTMENT_LEADER", TENANT_A);
        membreTokenA = tokenFor(saveUser("archives.membre@a.test"), "MEMBRE", TENANT_A);
        pasteurTokenB = tokenFor(saveUser("archives.pasteur@b.test"), "PASTEUR", TENANT_B);
    }

    // ==================================================================
    // Clôture + lecture versionnée
    // ==================================================================

    @Test
    @DisplayName("Clôture : v1 puis v2 ; legacy /archives sert les instantanés complets ; /search les résumés bornés ; facets ; détail ; filtre status")
    void fullArchivesContractOverHttp() throws Exception {
        UUID eventId = saveEvent(TENANT_A, "Séminaire du 4 mars");

        mockMvc.perform(post("/api/v1/church-events/{eventId}/archive", eventId)
                        .header("Authorization", pasteurTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.attendanceCount").value(0))
                .andExpect(jsonPath("$.eventYear").value(2023))
                .andExpect(jsonPath("$.eventMonth").value(3))
                .andExpect(jsonPath("$.snapshotJson.title").value("Séminaire du 4 mars"));

        mockMvc.perform(post("/api/v1/church-events/{eventId}/archive", eventId)
                        .header("Authorization", deptLeaderTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        // Legacy : liste complète (contrat historique conservé — instantanés inclus).
        mockMvc.perform(get("/api/v1/church-events/archives?year=2023&month=3")
                        .header("Authorization", pasteurTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].snapshotJson").exists());

        // Navigation de versions par eventId (additif V272).
        mockMvc.perform(get("/api/v1/church-events/archives").param("eventId", eventId.toString())
                        .header("Authorization", pasteurTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));

        // Recherche paginée : résumés SANS snapshot (la borne decidée par l'audit).
        mockMvc.perform(get("/api/v1/church-events/archives/search")
                        .param("year", "2023").param("page", "0").param("size", "1")
                        .header("Authorization", deptLeaderTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].snapshotJson").doesNotExist())
                .andExpect(jsonPath("$.content[0].attendanceCount").exists())
                .andExpect(jsonPath("$.content[0].version").value(2));

        // Facettes : seule l'année/mois réellement archivés.
        mockMvc.perform(get("/api/v1/church-events/archives/facets")
                        .header("Authorization", pasteurTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].eventYear").value(2023))
                .andExpect(jsonPath("$[0].months", org.hamcrest.Matchers.contains(3)))
                .andExpect(jsonPath("$[0].archiveCount").value(2));

        // Détail d'une version.
        UUID archiveV1Id = archiveRepository.findByTenantIdAndEventIdOrderByVersionDesc(TENANT_A, eventId)
                .get(1).getId();
        mockMvc.perform(get("/api/v1/church-events/archives/{archiveId}", archiveV1Id)
                        .header("Authorization", pasteurTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(archiveV1Id.toString()))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.snapshotJson").exists());

        // GET /church-events?status= — paramètre mort depuis V203, enfin appliqué.
        UUID plannedEventId = saveEvent(TENANT_A, "Culte de dimanche");
        mockMvc.perform(get("/api/v1/church-events").param("status", "PLANIFIE")
                        .header("Authorization", pasteurTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(plannedEventId.toString()));
        mockMvc.perform(get("/api/v1/church-events").param("status", "ARCHIVED")
                        .header("Authorization", pasteurTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(eventId.toString()));
    }

    // ==================================================================
    // Gardes et isolation
    // ==================================================================

    @Test
    @DisplayName("Un MEMBRE sans EVENTS_EXECUTE_R6A2 est refusé (403) en lecture comme en clôture")
    void memberWithoutKeyIsForbidden() throws Exception {
        UUID eventId = saveEvent(TENANT_A, "Culte d'évangélisation");

        mockMvc.perform(get("/api/v1/church-events/archives")
                        .header("Authorization", membreTokenA))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/church-events/archives/search")
                        .header("Authorization", membreTokenA))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/church-events/{eventId}/archive", eventId)
                        .header("Authorization", membreTokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Isolation : le tenant B ne voit ni les listes, ni les facettes, ni le détail (404) des archives du tenant A")
    void crossTenantIsolationHolds() throws Exception {
        UUID eventId = saveEvent(TENANT_A, "Convention régionale");
        UUID archiveId = churchEventService.archiveEvent(TENANT_A, actorA, eventId).getId();

        mockMvc.perform(get("/api/v1/church-events/archives")
                        .header("Authorization", pasteurTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.empty()));
        mockMvc.perform(get("/api/v1/church-events/archives/search")
                        .header("Authorization", pasteurTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/church-events/archives/facets")
                        .header("Authorization", pasteurTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.empty()));
        mockMvc.perform(get("/api/v1/church-events/archives/{archiveId}", archiveId)
                        .header("Authorization", pasteurTokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /church-events ne peut pas forger ARCHIVED (400) — seul POST /archive produit une archive")
    void putCannotForgeArchivedStatus() throws Exception {
        UUID eventId = saveEvent(TENANT_A, "Réunion de prière");

        mockMvc.perform(put("/api/v1/church-events/{id}", eventId)
                        .header("Authorization", pasteurTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isBadRequest());

        assertThat(churchEventRepository.findById(eventId).orElseThrow().getStatut())
                .isEqualTo(Event.STATUT_PLANIFIE);
        assertThat(archiveRepository.findByTenantId(TENANT_A)).isEmpty();
    }

    // ==================================================================
    // Intégrité de version sous concurrence
    // ==================================================================

    @Test
    @DisplayName("Deux clôtures simultanées du même événement produisent des versions uniques {1,2}")
    void concurrentClosuresProduceUniqueVersions() throws Exception {
        UUID eventId = saveEvent(TENANT_A, "Conférence pasteurs");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> close = () -> {
            start.await(10, TimeUnit.SECONDS);
            return churchEventService.archiveEvent(TENANT_A, actorA, eventId).getVersion();
        };
        Future<Integer> first = pool.submit(close);
        Future<Integer> second = pool.submit(close);
        start.countDown();

        List<Integer> versions;
        try {
            versions = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        assertThat(versions).containsExactlyInAnyOrder(1, 2);
        assertThat(archiveRepository.findByEventIdOrderByVersionDesc(eventId))
                .extracting(ChurchEventArchive::getVersion)
                .containsExactlyInAnyOrder(1, 2);
    }

    @Test
    @DisplayName("L'index unique (event_id, version) est le dernier rempart : une écriture doublonnée est refusée en base")
    void uniqueIndexRejectsDuplicateVersion() {
        UUID eventId = saveEvent(TENANT_A, "Veillée de louange");
        ChurchEventArchive v1 = ChurchEventArchive.builder()
                .tenantId(TENANT_A).eventId(eventId).eventTitle("Veillée de louange")
                .eventStartAt(java.time.OffsetDateTime.parse("2023-03-04T09:00:00Z"))
                .eventYear(2023).eventMonth(3).version(1).attendanceCount(0)
                .snapshotJson(new LinkedHashMap<>())
                .build();
        archiveRepository.saveAndFlush(v1);

        ChurchEventArchive duplicate = ChurchEventArchive.builder()
                .tenantId(TENANT_A).eventId(eventId).eventTitle("Veillée de louange")
                .eventStartAt(java.time.OffsetDateTime.parse("2023-03-04T09:00:00Z"))
                .eventYear(2023).eventMonth(3).version(1).attendanceCount(0)
                .snapshotJson(new LinkedHashMap<>())
                .build();

        assertThatThrownBy(() -> archiveRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ==================================================================
    // helpers (recette des tests RBAC HTTP du dépôt)
    // ==================================================================

    private UUID saveEvent(UUID tenantId, String title) {
        Event event = Event.builder()
                .tenantId(tenantId)
                .titre(title)
                .description("Test V272")
                .typeEvenement("REUNION")
                .statut(Event.STATUT_PLANIFIE)
                .dateDebut(LocalDateTime.of(2023, 3, 4, 9, 0))
                .dateFin(LocalDateTime.of(2023, 3, 4, 17, 0))
                .build();
        return churchEventRepository.save(event).getId();
    }

    private void ensureActiveTenant(UUID tenantId, String slug) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE id = ?", Integer.class, tenantId);
        if (existing != null && existing > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, status, plan, country, currency, timezone, locale, "
                        + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tenantId, "Eglise Archives " + slug, slug, "ACTIVE", "DISCOVERY",
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

    private UUID saveUser(String email) {
        return userRepository.findByEmailIgnoreCase(email).map(User::getId).orElseGet(() ->
                userRepository.save(User.builder()
                        .tenantId(TENANT_A)
                        .email(email)
                        .passwordHash("PLACEHOLDER")
                        .firstName("Archives")
                        .lastName("Test")
                        .role(UserRole.PASTEUR)
                        .roles(Set.of(UserRole.PASTEUR))
                        .activeRole(UserRole.PASTEUR)
                        .statut(UserStatus.ACTIVE)
                        .build()).getId());
    }

    private void saveMembership(UUID userId, UUID tenantId, String roleKey) {
        boolean already = membershipRepository
                .findAllByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE)
                .stream().anyMatch(m -> roleKey.equals(m.getRoleLegacy()));
        if (already) {
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

    private String tokenFor(UUID userId, String roleKey, UUID tenantId) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                userId, userId + "@archives.test", roleKey, Set.of(roleKey), false, tenantId);
    }
}
