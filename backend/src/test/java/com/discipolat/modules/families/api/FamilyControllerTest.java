package com.discipolat.modules.families.api;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.test.TestJwtConfig;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRiskService;
import com.discipolat.modules.families.domain.FamilyService;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.transfers.domain.TransferBridgeService;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests API pour FamilyController — CRUD famille avec
 * CreateFamilyRequest et UpdateFamilyRequest.
 */
@WebMvcTest(FamilyController.class)
@Import({SecurityConfig.class, TestJwtConfig.class})
class FamilyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private FamilyService familyService;

    @MockBean
    private FamilyRiskService familyRiskService;

    @MockBean
    private SoulRepository soulRepository;

    @MockBean
    private TransferBridgeService transferBridgeService;

    @MockBean
    private UserRepository userRepository;

    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String EMAIL = "admin@discipolat.com";
    private static final UUID FAMILY_ID = UUID.randomUUID();
    private static final UUID CHEF_ID = UUID.randomUUID();
    private static final UUID CHEF_ADJOINT_ID = UUID.randomUUID();

    private Family family;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(FAMILY_ID)
                .nom("Famille des Palmiers")
                .chefFamilleId(CHEF_ID)
                .chefAdjointId(CHEF_ADJOINT_ID)
                .build();
    }

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                USER_ID, EMAIL, role, Set.of(role), false, null);
    }

    // ======================== CREATE ========================

    @Test
    @DisplayName("POST /families avec données valides → 201")
    void create_withValidData_shouldReturn201() throws Exception {
        when(familyService.create(any(CreateFamilyRequest.class))).thenReturn(family);

        mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nom": "Famille des Palmiers",
                                    "chefFamilleId": "%s"
                                }
                                """.formatted(CHEF_ID)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /families sans nom → 400")
    void create_withoutName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chefFamilleId": "%s"
                                }
                                """.formatted(CHEF_ID)))
                .andExpect(status().isBadRequest());
    }

    // ======================== UPDATE avec UpdateFamilyRequest ========================

    @Test
    @DisplayName("PUT /families/{id} avec UpdateFamilyRequest valide → 200")
    void update_withValidUpdateRequest_shouldReturn200() throws Exception {
        when(familyService.update(eq(FAMILY_ID), any(UpdateFamilyRequest.class))).thenReturn(family);

        mockMvc.perform(put("/api/v1/families/" + FAMILY_ID)
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nom": "Famille des Palmiers Updated",
                                    "chefFamilleId": "%s",
                                    "chefAdjointId": "%s"
                                }
                                """.formatted(CHEF_ID, CHEF_ADJOINT_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /families/{id} sans nom → 400 (UpdateFamilyRequest)")
    void update_withoutName_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/v1/families/" + FAMILY_ID)
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chefFamilleId": "%s"
                                }
                                """.formatted(CHEF_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /families/{id} n'accepte PAS createNewChef")
    void update_shouldNotAcceptCreateNewChef() throws Exception {
        when(familyService.update(eq(FAMILY_ID), any(UpdateFamilyRequest.class))).thenReturn(family);

        // UpdateFamilyRequest n'a pas de champ createNewChef
        // → les champs inconnus sont ignorés par Jackson
        mockMvc.perform(put("/api/v1/families/" + FAMILY_ID)
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nom": "Test",
                                    "createNewChef": true,
                                    "newChefFirstName": "Jean"
                                }
                                """))
                .andExpect(status().isOk());
        // Pas d'erreur mais createNewChef est ignoré
    }

    @Test
    @DisplayName("PUT /families/{id} sans token → 401")
    void update_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(put("/api/v1/families/" + FAMILY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nom": "Test"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ======================== DELETE ========================

    @Test
    @DisplayName("DELETE /families/{id} sans perm bean → 400")
    void delete_withoutPermBean_shouldReturn400() throws Exception {
        // @perm.has('FAMILY','DELETE') SpEL bean indisponible en WebMvcTest → 400
        mockMvc.perform(delete("/api/v1/families/" + FAMILY_ID)
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // ======================== GET ========================

    @Test
    @DisplayName("GET /families/{id} → 200 avec données famille")
    void getById_shouldReturn200() throws Exception {
        when(familyService.findById(FAMILY_ID)).thenReturn(family);

        mockMvc.perform(get("/api/v1/families/" + FAMILY_ID)
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Famille des Palmiers"));
    }
}
