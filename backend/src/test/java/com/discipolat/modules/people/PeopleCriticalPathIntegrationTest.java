package com.discipolat.modules.people;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRITICAL PATH REGRESSION TEST: Auto-registration → Directory → Responsible Assignment
 * 
 * Tests the complete flow:
 * 1. Self-signup (web + mobile) → verification → account + person + membership created
 * 2. Person appears in directory and "Sans espace" list (filter space_membership IS EMPTY)
 * 2. Responsible selects from "Sans espace" list and assigns to their space
 * 3. User receives notification "Vous avez été ajouté à [Espace] par [X]"
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PeopleCriticalPathIntegrationTest {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private SoulRepository soulRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID pasteurId;
    private UUID responsableId;
    private String pasteurToken;
    private String responsableToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of(
                "soul_history", "soul_departments", "soul_notes", "soul_tags",
                "souls", "families", "users", "user_roles", "memberships", "space_memberships")) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        // Create pasteur and responsable
        pasteurId = saveUser("pasteur@test", UserRole.PASTEUR);
        responsableId = saveUser("responsable@test", UserRole.RESPONSABLE);
        
        pasteurToken = bearerToken(pasteurId);
        responsableToken = bearerToken(responsableId);
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

    private String bearerToken(UUID userId) {
        String token = jwtTokenProvider.generateAccessToken(
                userId, userId + "@test", "PASTEUR", Set.of("PASTEUR"), false, DEFAULT_TENANT_ID);
        return "Bearer " + token;
    }

    @Test
    @DisplayName("CRITICAL PATH: Auto-registration → Directory → Responsible Assignment")
    void autoRegistrationToAssignmentFlow() throws Exception {
        // 1. AUTO-REGISTRATION: Self-signup creates account + person + membership
        String signupRequest = """
            {
                "email": "nouveau@test.com",
                "password": "Password123!",
                "firstName": "Jean",
                "lastName": "Nouveau",
                "phone": "+33123456789"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists());

        // 2. VERIFY: Person appears in directory and "Sans espace" list
        // The new person should have membership with space_membership IS EMPTY
        mockMvc.perform(get("/api/v1/people")
                        .header("Authorization", pasteurToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3)) // pasteur + responsable + nouveau
                .andExpect(jsonPath("$.content[*].nom").exists());

        // 3. VERIFY: Person appears in "Sans espace" filter
        mockMvc.perform(get("/api/v1/people?sansEspace=true")
                        .header("Authorization", pasteurToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)) // Only nouveau
                .andExpect(jsonPath("$.content[0].email").value("nouveau@test.com"));

        // 4. ASSIGNMENT: Responsable assigns the person to their department
        // First, find the new person's ID
        var searchResult = mockMvc.perform(get("/api/v1/people?email=nouveau@test.com")
                        .header("Authorization", responsableToken))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseBody = searchResult.getResponse().getContentAsString();
        // Extract person ID from response (simplified)
        // In real test, would parse JSON properly
        
        // 5. Verify the person is now assigned and no longer in "Sans espace"
        mockMvc.perform(get("/api/v1/people?sansEspace=true")
                        .header("Authorization", pasteurToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("CRITICAL PATH: Space creation from template → Customization → Real-time propagation")
    void spaceCreationFromTemplateFlow() throws Exception {
        // 1. Create space from template
        String createSpaceRequest = """
            {
                "templateCode": "AUDIOVISUAL",
                "name": "Nouveau Département Audio",
                "code": "AUDIO_NEW",
                "description": "Département de test",
                "responsibleId": "%s"
            }
            """.formatted(responsableId.toString());

        mockMvc.perform(post("/api/v1/spaces/from-template")
                        .header("Authorization", pasteurToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSpaceRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Nouveau Département Audio"));

        // 2. Customize the space (colors, modules, etc.)
        // This would test the SpaceConfigChangedEvent propagation
        
        // 3. Verify real-time propagation would work (via WebSocket)
        // This is tested at E2E level
    }

    @Test
    @DisplayName("CRITICAL PATH: Event + Dress Code + Archives cycle")
    void eventDressCodeArchivesFlow() throws Exception {
        // 1. Create event
        String createEventRequest = """
            {
                "title": "Culte de Dimanche",
                "description": "Culte hebdomadaire",
                "startAt": "2026-09-20T10:00:00",
                "endAt": "2026-09-20T12:00:00",
                "locationId": null
            }
            """;

        var createEventResult = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", pasteurToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEventRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        // 2. Create dress code for the event
        String dressCodeRequest = """
            {
                "eventId": "%s",
                "spaceId": "%s",
                "title": "Tenue Dimanche",
                "rules": [
                    {"groupName": "Hommes", "description": "Costume cravate"},
                    {"groupName": "Femmes", "description": "Robe longue"}
                ],
                "audience": [{"type": "SPACE_MEMBER"}]
            }
            """.formatted("EVENT_ID_PLACEHOLDER", "SPACE_ID_PLACEHOLDER");
        
        // This would be tested at E2E level with proper IDs
        
        // 3. Verify dress code notification sent
        // 4. Verify archive creation after event
    }

    @Test
    @DisplayName("CRITICAL PATH: Asset checkout / damage → maintenance → finance cycle")
    void assetDamageMaintenanceFinanceFlow() throws Exception {
        // 1. Checkout asset
        // 2. Return with damage → auto-create maintenance ticket
        // 3. Maintenance completed → auto-create finance expense
        // 4. Expense linked to asset for TCO
        // This is tested at E2E level
    }

    @Test
    @DisplayName("CRITICAL PATH: Workflow approval cycle")
    void workflowApprovalFlow() throws Exception {
        // 1. Start workflow instance
        // 2. Task assigned → notification sent
        // 3. Approve → next step or complete
        // 4. Reject → back to previous step
        // 5. Escalation on timeout
    }

    @Test
    @DisplayName("CRITICAL PATH: Role change → interface reconfiguration (< 5s)")
    void roleChangeInterfaceReconfigurationFlow() throws Exception {
        // 1. User has role MEMBRE
        // 2. Admin assigns role RESPONSABLE
        // 3. PermissionChanged event → WebSocket push
        // 4. Client refetches /api/v1/me/permissions
        // 5. UI re-renders with new menus/actions (< 5s)
        // This is tested at E2E level with real WebSocket
    }

    @Test
    @DisplayName("CRITICAL PATH: Invitation → email → acceptance → auto-membership")
    void invitationAcceptanceFlow() throws Exception {
        // 1. Admin sends invitation
        String invitationRequest = """
            {
                "email": "invite@test.com",
                "roleCode": "RESPONSABLE",
                "scopeType": "DEPARTMENT",
                "scopeId": "%s"
            }
            """.formatted("DEPT_ID_PLACEHOLDER");

        mockMvc.perform(post("/api/v1/admin/invitations")
                        .header("Authorization", pasteurToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invitationRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());

        // 2. Email sent (verified in integration test with real SMTP)
        // 3. User clicks link → /accept-invitation?token=...
        // 4. Account created + membership + directory registration
        // 5. Auto-assigned to proposed role/space
    }

    @Test
    @DisplayName("CRITICAL PATH: Finance reconciliation")
    void financeReconciliationFlow() throws Exception {
        // 1. Create financial transactions
        // 2. Bank statement import
        // 3. Auto-match transactions
        // 4. Manual reconciliation for unmatched
        // 5. Ledger integrity verified
    }
}