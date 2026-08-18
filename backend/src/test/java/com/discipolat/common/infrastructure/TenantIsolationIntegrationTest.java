package com.discipolat.common.infrastructure;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TESTS D'ISOLATION MULTI-TENANT (V70) — deux églises distinctes.
 *
 * Objectif : prouver qu'Église A ne peut JAMAIS lire, modifier ou supprimer
 * les données d'Église B — et réciproquement — sur la chaîne HTTP RÉELLE :
 * JWT réel (claim tenantId) → TenantInterceptor → filtre Hibernate
 * (TenantFilter) → TenantAwareSimpleJpaRepository (findById tenant-scopé).
 *
 * Couvre les deux mécanismes complémentaires :
 * - le filtre Hibernate @Filter (requêtes de liste) ;
 * - le rejet par clé primaire (findById/getReferenceById), que @Filter
 *   ne couvre PAS (EntityManager.find) et que la base de repository
 *   tenant-aware referme (IDOR : lecture/modification/suppression par ID).
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TenantIsolationIntegrationTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private SoulRepository soulRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID soulAId;
    private UUID soulBId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of(
                "soul_history", "soul_departments", "soul_notes", "soul_tags",
                "souls", "families", "users", "user_roles")) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        UUID faiseurA = saveUser("admin.a@test", TENANT_A, UserRole.ADMIN);
        UUID faiseurB = saveUser("admin.b@test", TENANT_B, UserRole.ADMIN);
        saveUser("faiseur.a@test", TENANT_A, UserRole.FAISEUR);
        saveUser("faiseur.b@test", TENANT_B, UserRole.FAISEUR);
        soulAId = saveSoul("SoulEgliseA", TENANT_A, faiseurA);
        soulBId = saveSoul("SoulEgliseB", TENANT_B, faiseurB);
    }

    private UUID saveUser(String email, UUID tenantId, UserRole role) {
        return userRepository.save(User.builder()
                .tenantId(tenantId)
                .email(email)
                .passwordHash("PLACEHOLDER")
                .firstName("Admin")
                .lastName("Test")
                .role(role)
                .roles(Set.of(role))
                .activeRole(role)
                .statut(UserStatus.ACTIVE)
                .build()).getId();
    }

    private UUID saveSoul(String nom, UUID tenantId, UUID faiseurId) {
        return soulRepository.save(Soul.builder()
                .tenantId(tenantId)
                .nom(nom).prenom("Test").email(nom.toLowerCase() + "@test")
                .typeDisciple(TypeDisciple.NOUVEL_ARRIVANT)
                .dateIntegration(LocalDate.now())
                .statut(StatutAme.ACTIF)
                .faiseurId(faiseurId)
                .etatSpirituel("EN_CROISSANCE")
                .niveauCroissance(2)
                .build()).getId();
    }

    private String bearerToken(UUID tenantId) {
        String token = jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(), "admin@test", "ADMIN", Set.of("ADMIN"), false, tenantId);
        return "Bearer " + token;
    }

    // ========================================================================
    // LISTES : chaque église ne voit que SES données
    // ========================================================================

    @Test
    @DisplayName("Église B ne voit que ses âmes dans la liste (isolation filtre Hibernate)")
    void egliseB_neVoitQueSesAmesEnListe() throws Exception {
        mockMvc.perform(get("/api/v1/souls")
                        .header("Authorization", bearerToken(TENANT_B)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(soulBId.toString()));
    }

    @Test
    @DisplayName("Église A voit uniquement ses âmes (pas celles de B)")
    void egliseA_neVoitQueSesAmesEnListe() throws Exception {
        mockMvc.perform(get("/api/v1/souls")
                        .header("Authorization", bearerToken(TENANT_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(soulAId.toString()));
    }

    // ========================================================================
    // IDOR : accès direct par ID (EntityManager.find — non couvert par @Filter)
    // ========================================================================

    @Test
    @DisplayName("Église B ne peut pas LIRE une âme d'Église A par ID → 404")
    void egliseB_nePeutPasLireUneAmeDeEgliseA_parId() throws Exception {
        mockMvc.perform(get("/api/v1/souls/" + soulAId)
                        .header("Authorization", bearerToken(TENANT_B)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Église B ne peut pas MODIFIER une âme d'Église A → 404 (anti-corruption)")
    void egliseB_nePeutPasModifierUneAmeDeEgliseA() throws Exception {
        mockMvc.perform(put("/api/v1/souls/" + soulAId)
                        .header("Authorization", bearerToken(TENANT_B))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"HACK\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Église A accède normalement à ses propres âmes par ID → 200")
    void egliseA_litSaPropreAme_parId() throws Exception {
        mockMvc.perform(get("/api/v1/souls/" + soulAId)
                        .header("Authorization", bearerToken(TENANT_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(soulAId.toString()));
    }
}
