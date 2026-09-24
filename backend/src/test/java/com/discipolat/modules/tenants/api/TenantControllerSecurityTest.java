package com.discipolat.modules.tenants.api;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.test.StrictPlatformAuthzConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.security.domain.TokenRevocationService;
import com.discipolat.modules.tenants.domain.AuthzSecurityBean;
import com.discipolat.modules.tenants.domain.AuthorizationService;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * §Isolation inter-églises — l'API {@code /api/v1/tenants} n'est accessible
 * qu'au Super Admin de la PLATEFORME ({@code PLATFORM_SUPER_ADMIN}).
 *
 * <p>Régression de la faille corrigée le 2026-09-24 : les endpoints étaient
 * protégés par {@code hasAnyRole('ADMIN','PASTEUR')}, ce qui permettait à
 * n'importe quel admin (ou pasteur) d'église de lister, modifier et supprimer
 * les AUTRES églises. L'inscription auto-provisionnée attribuant le rôle ADMIN
 * rendait la faille exploitable par tout nouvel inscrit.
 *
 * <p>Configuration de test volontairement STRICTE : le bean {@code authz}
 * n'accepte que l'autorité {@code PLATFORM_SUPER_ADMIN}.
 */
@WebMvcTest(controllers = TenantController.class)
@Import({SecurityConfig.class, StrictPlatformAuthzConfig.class})
class TenantControllerSecurityTest {

    private static final UUID TENANT_ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PASTEUR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SUPER_ADMIN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        TenantResponse tenant = TenantResponse.from(Tenant.builder()
                .id(TENANT_A)
                .name("Eglise A")
                .slug("eglise-a")
                .plan("free")
                .build());
        when(tenantService.list()).thenReturn(List.of(tenant));
        when(tenantService.get(any())).thenReturn(tenant);
        when(tenantService.findBySlug(anyString())).thenReturn(Optional.of(Tenant.builder()
                .id(TENANT_A).name("Eglise A").slug("eglise-a").plan("free").build()));
    }

    private String tokenFor(UUID userId, String email, String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(userId, email, role, Set.of(role), false, null);
    }

    // ==================== Isolation : refus aux rôles d'église ====================

    @Test
    @DisplayName("§Isolation — un ADMIN d'église ne peut PAS lister les tenants (403)")
    void tenantAdminCannotListTenants() throws Exception {
        mockMvc.perform(get("/api/v1/tenants")
                        .header("Authorization", tokenFor(TENANT_ADMIN_ID, "admin@eglise-a.org", "ADMIN")))
                .andExpect(status().isForbidden());

        verify(tenantService, never()).list();
    }

    @Test
    @DisplayName("§Isolation — un PASTEUR d'église ne peut PAS lister les tenants (403)")
    void pasteurCannotListTenants() throws Exception {
        mockMvc.perform(get("/api/v1/tenants")
                        .header("Authorization", tokenFor(PASTEUR_ID, "pasteur@eglise-a.org", "PASTEUR")))
                .andExpect(status().isForbidden());

        verify(tenantService, never()).list();
    }

    @Test
    @DisplayName("§Isolation — un admin d'église ne peut PAS lire le détail d'un AUTRE tenant (403)")
    void tenantAdminCannotReadOtherTenant() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/" + TENANT_B)
                        .header("Authorization", tokenFor(TENANT_ADMIN_ID, "admin@eglise-a.org", "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("§Isolation — un admin d'église ne peut PAS modifier un AUTRE tenant (403)")
    void tenantAdminCannotUpdateOtherTenant() throws Exception {
        mockMvc.perform(put("/api/v1/tenants/" + TENANT_B)
                        .header("Authorization", tokenFor(TENANT_ADMIN_ID, "admin@eglise-a.org", "ADMIN"))
                        .contentType("application/json")
                        .content("{\"name\":\"Eglise B pirates\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("§Isolation — un admin d'église ne peut PAS supprimer un AUTRE tenant (403)")
    void tenantAdminCannotDeleteOtherTenant() throws Exception {
        mockMvc.perform(delete("/api/v1/tenants/" + TENANT_B)
                        .header("Authorization", tokenFor(TENANT_ADMIN_ID, "admin@eglise-a.org", "ADMIN")))
                .andExpect(status().isForbidden());

        verify(tenantService, never()).deactivate(any());
    }

    @Test
    @DisplayName("§Isolation — un pasteur ne peut PAS supprimer un tenant (403)")
    void pasteurCannotDeleteTenant() throws Exception {
        mockMvc.perform(delete("/api/v1/tenants/" + TENANT_B)
                        .header("Authorization", tokenFor(PASTEUR_ID, "pasteur@eglise-a.org", "PASTEUR")))
                .andExpect(status().isForbidden());

        verify(tenantService, never()).deactivate(any());
    }


    // ==================== Accès autorisé du Super Admin plateforme ====================

    @Test
    @DisplayName("§Super Admin — PLATFORM_SUPER_ADMIN peut lister les tenants (200)")
    void platformSuperAdminCanListTenants() throws Exception {
        mockMvc.perform(get("/api/v1/tenants")
                        .header("Authorization",
                                tokenFor(SUPER_ADMIN_ID, "superadmin@discipolat.com", "PLATFORM_SUPER_ADMIN")))
                .andExpect(status().isOk());

        verify(tenantService).list();
    }

    @Test
    @DisplayName("§Super Admin — PLATFORM_SUPER_ADMIN peut lire le détail d'un tenant (200)")
    void platformSuperAdminCanReadTenant() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/" + TENANT_B)
                        .header("Authorization",
                                tokenFor(SUPER_ADMIN_ID, "superadmin@discipolat.com", "PLATFORM_SUPER_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("§Super Admin — PLATFORM_SUPER_ADMIN peut créer un tenant (201)")
    void platformSuperAdminCanCreateTenant() throws Exception {
        TenantResponse created = TenantResponse.from(Tenant.builder()
                .id(UUID.fromString("cccccccc-0000-0000-0000-000000000003"))
                .name("Nouvelle Eglise")
                .slug("nouvelle-eglise")
                .plan("free")
                .build());
        when(tenantService.create(any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/tenants")
                        .header("Authorization",
                                tokenFor(SUPER_ADMIN_ID, "superadmin@discipolat.com", "PLATFORM_SUPER_ADMIN"))
                        .contentType("application/json")
                        .content("{\"name\":\"Nouvelle Eglise\",\"slug\":\"nouvelle-eglise\"}"))
                .andExpect(status().isCreated());
    }
}
