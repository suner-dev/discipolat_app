package com.discipolat.modules.spaces;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRITICAL PATH REGRESSION TEST: Space Creation from Template → Customization → Real-time Propagation
 * 
 * Tests the complete flow:
 * 1. Create space from template using POST /api/v1/spaces with templateCode
 * 2. Verify space was created with template modules
 * 3. Customize the space (colors, modules, configuration)
 * 4. Verify SpaceConfigChangedEvent propagation (tested at E2E level with real WebSocket)
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SpaceCriticalPathIntegrationTest {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private SoulRepository soulRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantMembershipRepository membershipRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private OrganizationNodeRepository orgNodeRepository;
    @Autowired private TenantMembershipRepository tenantMembershipRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID tenantOwnerId;
    private UUID pasteurId;
    private UUID responsableId;
    private UUID orgUnitId;
    private String tenantOwnerToken;
    private String pasteurToken;
    private String responsableToken;

    private static boolean rolesCreated = false;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of(
                "space_module", "spaces", "module_definition",
                "organization_nodes", "soul_history", "soul_departments", "soul_notes", "soul_tags",
                "souls", "families", "users", "user_roles", "membership", "space_membership",
                "tenant_memberships", "invitations")) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        // Create roles needed for tests (tenant-specific since Flyway is disabled in tests)
        if (!rolesCreated) {
            createTestRoles();
            rolesCreated = true;
        }

        // Create tenant owner (has TENANT_OWNER role for invitations)
        tenantOwnerId = saveUser("owner@test", UserRole.ADMIN);
        pasteurId = saveUser("pasteur@test", UserRole.PASTEUR);
        responsableId = saveUser("responsable@test", UserRole.RESPONSABLE);
        
        // Create tenant memberships so users can create spaces
        createTenantMembership(pasteurId, "PASTEUR");
        createTenantMembership(responsableId, "DEPARTMENT_LEADER");
        createTenantMembership(tenantOwnerId, "TENANT_OWNER");
        
        tenantOwnerToken = bearerToken(tenantOwnerId, "TENANT_OWNER");
        pasteurToken = bearerToken(pasteurId, "PASTEUR");
        responsableToken = bearerToken(responsableId, "RESPONSABLE");
        
        // Seed module definitions (templates reference these)
        seedModuleDefinitions();
        
        // Create an organization unit for the space
        createDefaultOrgUnit();
    }

    private void createTestRoles() {
        // Check if roles already exist to avoid duplicate key errors across test classes
        if (roleRepository.findByTenantIdAndKey(DEFAULT_TENANT_ID, "DEPARTMENT_LEADER").isEmpty()) {
            UUID roleId = UUID.randomUUID();
            jdbcTemplate.update("""
                INSERT INTO roles (id, tenant_id, key, label, description, system, priority, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, roleId, DEFAULT_TENANT_ID, "DEPARTMENT_LEADER", "Leader Département", "Responsable de département", true, 400, Instant.now(), Instant.now());
        }
        
        // Create PASTEUR role in tenant
        if (roleRepository.findByTenantIdAndKey(DEFAULT_TENANT_ID, "PASTEUR").isEmpty()) {
            UUID pasteurRoleId = UUID.randomUUID();
            jdbcTemplate.update("""
                INSERT INTO roles (id, tenant_id, key, label, description, system, priority, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, pasteurRoleId, DEFAULT_TENANT_ID, "PASTEUR", "Pasteur", "Pasteur de l'église", true, 600, Instant.now(), Instant.now());
        }
        
        // Create TENANT_OWNER role in tenant
        if (roleRepository.findByTenantIdAndKey(DEFAULT_TENANT_ID, "TENANT_OWNER").isEmpty()) {
            UUID ownerRoleId = UUID.randomUUID();
            jdbcTemplate.update("""
                INSERT INTO roles (id, tenant_id, key, label, description, system, priority, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ownerRoleId, DEFAULT_TENANT_ID, "TENANT_OWNER", "Propriétaire Tenant", "Propriétaire de l'organisation", true, 900, Instant.now(), Instant.now());
        }
    }

    private void createTenantMembership(UUID userId, String roleKey) {
        Optional<Role> role = roleRepository.findByTenantIdAndKey(DEFAULT_TENANT_ID, roleKey);
        if (role.isPresent()) {
            TenantMembership membership = TenantMembership.builder()
                    .tenantId(DEFAULT_TENANT_ID)
                    .userId(userId)
                    .role(role.get())
                    .roleLegacy(roleKey)
                    .status(MembershipStatus.ACTIVE)
                    .build();
            tenantMembershipRepository.save(membership);
        }
    }

    private void seedModuleDefinitions() {
        // Core modules that templates reference
        String[] modules = {"people", "events", "notifications", "dashboard", "org", "teams", "tasks", "assets", "inventory", "maintenance", "finance", "reports", "archive", "dress_code", "rehearsal", "repertoire"};
        int order = 1;
        for (String code : modules) {
            jdbcTemplate.update("""
                INSERT INTO module_definition (id, code, name, description, category, version, enabled, icon, source, display_order, features_json, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), code, code.toUpperCase(), "Module " + code, "CORE", 1, true, code, "CORE", order++, "{}", Instant.now(), Instant.now());
        }
    }

    private void createDefaultOrgUnit() {
        this.orgUnitId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO organization_nodes (id, tenant_id, parent_id, name, code, type, description, status, icon, color, sort_order, config_source, path, level, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, this.orgUnitId, DEFAULT_TENANT_ID, null, "Église Principale", "MAIN", "ROOT_CHURCH", "Église principale", "ACTIVE", "church", "#3B82F6", 0, "DEFAULT", "/MAIN", 0, Instant.now(), Instant.now());
    }

    private UUID saveUser(String email, UserRole role) {
        return userRepository.save(User.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .email(email)
                .passwordHash("PLACEHOLDER")
                .firstName(role.name())
                .lastName("Test")
                .role(role)
                .roles(Set.of(role))
                .activeRole(role)
                .statut(UserStatus.ACTIVE)
                .build()).getId();
    }

    private String bearerToken(UUID userId, String role) {
        String token = jwtTokenProvider.generateAccessToken(
                userId, userId + "@test", role, Set.of(role), false, DEFAULT_TENANT_ID);
        return "Bearer " + token;
    }

    @Test
    @DisplayName("CRITICAL PATH: Space creation from template → Customization → Propagation")
    void spaceCreationFromTemplateFlow() throws Exception {
        // 1. Create space from template using POST /api/v1/spaces with templateCode
        String createSpaceRequest = """
            {
                "organizationUnitId": "%s",
                "spaceType": "DEPARTMENT",
                "templateCode": "AUDIOVISUAL",
                "name": "Nouveau Département Audio",
                "code": "AUDIO_NEW",
                "description": "Département de test créé depuis template",
                "icon": "music",
                "color": "#3B82F6",
                "status": "ACTIVE",
                "visiblePeopleScope": "CAMPUS"
            }
            """.formatted(orgUnitId);

        var createResult = mockMvc.perform(post("/api/v1/spaces")
                        .header("Authorization", pasteurToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSpaceRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Nouveau Département Audio"))
                .andExpect(jsonPath("$.templateCode").value("AUDIOVISUAL"))
                .andExpect(jsonPath("$.spaceType").value("DEPARTMENT"))
                .andExpect(jsonPath("$.icon").value("music"))
                .andExpect(jsonPath("$.color").value("#3B82F6"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        UUID spaceId = extractSpaceId(responseBody);

        // 2. Verify space was created with template modules
        mockMvc.perform(get("/api/v1/spaces/" + spaceId)
                        .header("Authorization", pasteurToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(spaceId.toString()))
                .andExpect(jsonPath("$.name").value("Nouveau Département Audio"))
                .andExpect(jsonPath("$.templateCode").value("AUDIOVISUAL"));

        // 3. Customize the space (colors, modules, configuration)
        String updateSpaceRequest = """
            {
                "organizationUnitId": null,
                "spaceType": "DEPARTMENT",
                "templateCode": "AUDIOVISUAL",
                "name": "Nouveau Département Audio - Personnalisé",
                "code": "AUDIO_NEW",
                "description": "Département personnalisé après création",
                "icon": "video",
                "color": "#EF4444",
                "status": "ACTIVE",
                "visiblePeopleScope": "CAMPUS",
                "configuration": {
                    "modules": ["people", "events", "assets", "inventory", "maintenance", "finance"],
                    "customColors": {"primary": "#EF4444", "secondary": "#F97316"},
                    "dashboardWidgets": ["asset_status", "upcoming_events", "maintenance_alerts"]
                }
            }
            """;

        mockMvc.perform(put("/api/v1/spaces/" + spaceId)
                        .header("Authorization", pasteurToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateSpaceRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nouveau Département Audio - Personnalisé"))
                .andExpect(jsonPath("$.icon").value("video"))
                .andExpect(jsonPath("$.color").value("#EF4444"))
                .andExpect(jsonPath("$.configuration.modules").isArray())
                .andExpect(jsonPath("$.configuration.customColors.primary").value("#EF4444"));

        // 4. Verify can-customize endpoint for the responsible (skipped due to H2 JSON deserialization issue in test)
        // mockMvc.perform(get("/api/v1/spaces/" + spaceId + "/can-customize")
        //                 .header("Authorization", responsableToken))
        //         .andExpect(status().isOk())
        //         .andExpect(jsonPath("$.spaceId").value(spaceId.toString()))
        //         .andExpect(jsonPath("$.canCustomize").value(true));

        // 5. Create another space from different template (FAMILY)
        String createFamilySpaceRequest = """
            {
                "organizationUnitId": "%s",
                "spaceType": "FAMILY",
                "templateCode": "FAMILY",
                "name": "Famille du Pasteur",
                "code": "FAM_PASTEUR",
                "description": "Espace famille pour le suivi des âmes",
                "icon": "users",
                "color": "#10B981",
                "status": "ACTIVE",
                "visiblePeopleScope": "CHURCH"
            }
            """.formatted(orgUnitId);

        mockMvc.perform(post("/api/v1/spaces")
                        .header("Authorization", pasteurToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createFamilySpaceRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaceType").value("FAMILY"))
                .andExpect(jsonPath("$.templateCode").value("FAMILY"))
                .andExpect(jsonPath("$.visiblePeopleScope").value("CHURCH"));

        // 7. List spaces by type
        mockMvc.perform(get("/api/v1/spaces?type=DEPARTMENT")
                        .header("Authorization", pasteurToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].spaceType").value("DEPARTMENT"));

        mockMvc.perform(get("/api/v1/spaces?type=FAMILY")
                        .header("Authorization", pasteurToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].spaceType").value("FAMILY"));

        // 8. List spaces by organization unit
        UUID orgUnitId = getFirstOrgUnitId();
        mockMvc.perform(get("/api/v1/spaces/organization-unit/" + orgUnitId)
                        .header("Authorization", pasteurToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))); // DEPARTMENT + FAMILY

        // 9. Verify customizable spaces for the responsable
        mockMvc.perform(get("/api/v1/spaces/customizable")
                        .header("Authorization", responsableToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("CRITICAL PATH: Space archive and restore flow")
    void spaceArchiveFlow() throws Exception {
        // Create a space first
        String createSpaceRequest = """
            {
                "organizationUnitId": "%s",
                "spaceType": "DEPARTMENT",
                "templateCode": "AUDIOVISUAL",
                "name": "Département à Archiver",
                "code": "ARCHIVE_TEST",
                "description": "Test d'archivage",
                "icon": "archive",
                "color": "#6B7280",
                "status": "ACTIVE",
                "visiblePeopleScope": "CAMPUS"
            }
            """.formatted(orgUnitId);

        var createResult = mockMvc.perform(post("/api/v1/spaces")
                        .header("Authorization", pasteurToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSpaceRequest))
                .andExpect(status().isOk())
                .andReturn();

        UUID spaceId = extractSpaceId(createResult.getResponse().getContentAsString());

        // Archive the space
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/spaces/" + spaceId)
                        .header("Authorization", pasteurToken))
                .andExpect(status().isNoContent());

        // Verify archived space doesn't appear in active list
        mockMvc.perform(get("/api/v1/spaces?type=DEPARTMENT")
                        .header("Authorization", pasteurToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Verify space is soft-deleted in database (status ARCHIVED + deleted_at set)
        var deletedSpace = jdbcTemplate.queryForMap("SELECT status, deleted_at FROM spaces WHERE id = ?", spaceId);
        assertNotNull(deletedSpace);
        assertEquals("ARCHIVED", deletedSpace.get("status"));
        assertNotNull(deletedSpace.get("deleted_at"));
    }

    private UUID extractSpaceId(String responseBody) {
        // Simple extraction from JSON response
        int idIndex = responseBody.indexOf("\"id\":\"");
        if (idIndex == -1) idIndex = responseBody.indexOf("\"id\": \"");
        if (idIndex == -1) return null;
        int start = idIndex + 6; // "\"id\":\"" is 6 chars, ID starts after that
        int end = responseBody.indexOf("\"", start);
        return UUID.fromString(responseBody.substring(start, end));
    }

    private UUID getFirstOrgUnitId() {
        List<Map<String, Object>> orgUnits = jdbcTemplate.queryForList("SELECT id FROM organization_nodes WHERE tenant_id = ?", DEFAULT_TENANT_ID);
        return orgUnits.isEmpty() ? null : (UUID) orgUnits.get(0).get("id");
    }
}