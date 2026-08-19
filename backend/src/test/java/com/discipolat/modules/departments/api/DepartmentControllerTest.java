package com.discipolat.modules.departments.api;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.test.TestJwtConfig;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentService;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
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
 * Tests API pour DepartmentController — CRUD département avec
 * CreateDepartmentRequest et UpdateDepartmentRequest.
 */
@WebMvcTest(DepartmentController.class)
@Import({SecurityConfig.class, TestJwtConfig.class})
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private DepartmentService departmentService;

    @MockBean
    private WorkspaceScopeService workspaceScopeService;

    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String EMAIL = "admin@discipolat.com";
    private static final UUID DEPT_ID = UUID.randomUUID();
    private static final UUID RESP_ID = UUID.randomUUID();

    private Department department;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(DEPT_ID)
                .nom("Louange")
                .description("Équipe de louange")
                .responsableId(RESP_ID)
                .build();
    }

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                USER_ID, EMAIL, role, Set.of(role), false, null);
    }

    // ======================== CREATE ========================

    @Test
    @DisplayName("POST /departments avec données valides → 201")
    void create_withValidData_shouldReturn201() throws Exception {
        when(departmentService.create(any(CreateDepartmentRequest.class))).thenReturn(department);

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nom": "Louange",
                                    "description": "Équipe de louange",
                                    "responsableId": "%s"
                                }
                                """.formatted(RESP_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Louange"));

        verify(departmentService).create(any(CreateDepartmentRequest.class));
    }

    @Test
    @DisplayName("POST /departments sans nom → 400")
    void create_withoutName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Sans nom"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ======================== UPDATE avec UpdateDepartmentRequest ========================

    @Test
    @DisplayName("PUT /departments/{id} avec UpdateDepartmentRequest valide → 200")
    void update_withValidUpdateRequest_shouldReturn200() throws Exception {
        when(departmentService.findById(DEPT_ID)).thenReturn(department);
        when(departmentService.update(any(Department.class))).thenReturn(department);

        mockMvc.perform(put("/api/v1/departments/" + DEPT_ID)
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nom": "Louange Updated",
                                    "description": "Nouvelle description",
                                    "responsableId": "%s"
                                }
                                """.formatted(RESP_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Louange Updated"));
    }

    @Test
    @DisplayName("PUT /departments/{id} sans nom → 400 (UpdateDepartmentRequest)")
    void update_withoutName_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/v1/departments/" + DEPT_ID)
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Sans nom"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /departments/{id} n'accepte PAS createNewResponsable")
    void update_shouldNotAcceptCreateNewResponsable() throws Exception {
        when(departmentService.findById(DEPT_ID)).thenReturn(department);
        when(departmentService.update(any(Department.class))).thenReturn(department);

        // UpdateDepartmentRequest n'a pas de champ createNewResponsable
        // → les champs inconnus sont ignorés par Jackson
        mockMvc.perform(put("/api/v1/departments/" + DEPT_ID)
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nom": "Test",
                                    "createNewResponsable": true,
                                    "newRespFirstName": "Jean"
                                }
                                """))
                .andExpect(status().isOk());
        // Pas d'erreur mais createNewResponsable est ignoré
    }

    @Test
    @DisplayName("PUT /departments/{id} sans token → 401")
    void update_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(put("/api/v1/departments/" + DEPT_ID)
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
    @DisplayName("DELETE /departments/{id} → 204")
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/departments/" + DEPT_ID)
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNoContent());

        verify(departmentService).delete(DEPT_ID);
    }
}
