package com.discipolat.modules.users.api;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.test.TestSecurityConfig;
import com.discipolat.modules.authentication.domain.AuthService;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.evaluations.domain.EvaluationService;
import com.discipolat.modules.families.service.PermissionResolver;
import com.discipolat.modules.relations.domain.MemberRelationService;
import com.discipolat.modules.relations.domain.UserHierarchyService;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.transfers.domain.TransferBridgeService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P7b — contrat élargi de la création d'utilisateur : la clé facultative
 * {@code customRole} (rôle custom du catalogue SERVI du tenant, au-delà de
 * l'enum socle UserRole) matérialise la tenant_membership ACTIVE de portée
 * TENANT puis rafraîchit les permissions servies. Additif : sans customRole,
 * le comportement est strictement identique à l'état précédent (aucune
 * interaction avec les memberships).
 */
@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerCustomRoleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean private UserService userService;
    @MockBean private AuthService authService;
    @MockBean private SecurityUtils securityUtils;
    @MockBean private EvaluationService evaluationService;
    @MockBean private WorkspaceScopeService workspaceScopeService;
    @MockBean private TransferBridgeService transferBridgeService;
    @MockBean private AuditService auditService;
    @MockBean private PermissionResolver permissionResolver;
    @MockBean private TenantMembershipRepository tenantMembershipRepository;
    @MockBean private RoleRepository roleRepository;
    @MockBean private MemberRelationService memberRelationService;
    @MockBean private UserHierarchyService userHierarchyService;

    private static final UUID USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID TENANT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID CALLER_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final String EMAIL = "admin@discipolat.com";

    private String bearer() {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                CALLER_ID, EMAIL, "ADMIN", Set.of("ADMIN"), false, TENANT_ID);
    }

    /** userService.create renvoie le compte avec id + tenant maison (pose du listener). */
    private void stubCreate() {
        // SecurityUtils.getCurrentUserId() est STATIQUE (lit le SecurityContext
        // renseigné par la chaîne JwtAuthenticationFilter du slice) — rien à
        // stubber ici ; le bearer() ci-dessus porte l'identité appelante.
        when(userService.create(any(User.class), anyString())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(USER_ID);
            u.setTenantId(TENANT_ID);
            return u;
        });
    }

    private static final String BODY_BASE = """
            {
              "email": "jean.dubois@example.org",
              "firstName": "Jean",
              "lastName": "Dubois",
              "password": "Passw0rd!",
              "role": "MEMBRE"
            }
            """;

    @Test
    @DisplayName("POST /users + customRole du catalogue servi → membership ACTIVE TENANT + bump permissions")
    void create_withCatalogCustomRole_createsActiveMembership() throws Exception {
        stubCreate();
        Role diacre = new Role();
        diacre.setId(UUID.randomUUID());
        diacre.setKey("DIACRE");
        when(roleRepository.findByTenantIdAndKey(TENANT_ID, "DIACRE")).thenReturn(Optional.of(diacre));
        when(tenantMembershipRepository.findAllByUserIdAndTenantIdAndStatus(
                USER_ID, TENANT_ID, MembershipStatus.ACTIVE)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "jean.dubois@example.org",
                                  "firstName": "Jean",
                                  "lastName": "Dubois",
                                  "password": "Passw0rd!",
                                  "role": "MEMBRE",
                                  "customRole": "diacre"
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<TenantMembership> captor = ArgumentCaptor.forClass(TenantMembership.class);
        verify(tenantMembershipRepository).save(captor.capture());
        TenantMembership tm = captor.getValue();
        assertThat(tm.getUserId()).isEqualTo(USER_ID);
        assertThat(tm.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(tm.getRole()).isSameAs(diacre);
        assertThat(tm.getScopeType()).isEqualTo(MembershipScopeType.TENANT);
        assertThat(tm.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        // Permissions effectives via la source #1 du PermissionResolver (membership → clé).
        verify(permissionResolver).bumpPermissions(TENANT_ID, USER_ID, "USER_CREATE_CUSTOM_ROLE");
    }

    @Test
    @DisplayName("POST /users + customRole absent du catalogue → 400 UNKNOWN_CUSTOM_ROLE, aucune membership")
    void create_withUnknownCustomRole_rejected400() throws Exception {
        stubCreate();
        when(roleRepository.findByTenantIdAndKey(TENANT_ID, "MAGICIEN")).thenReturn(Optional.empty());
        when(roleRepository.findByTenantIdIsNullAndKey("MAGICIEN")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "jean.dubois@example.org",
                                  "firstName": "Jean",
                                  "lastName": "Dubois",
                                  "password": "Passw0rd!",
                                  "role": "MEMBRE",
                                  "customRole": "MAGICIEN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("UNKNOWN_CUSTOM_ROLE"));

        verify(tenantMembershipRepository, never()).save(any(TenantMembership.class));
        verifyNoInteractions(permissionResolver);
    }

    @Test
    @DisplayName("POST /users sans customRole → comportement historique strict (additif, aucune membership)")
    void create_withoutCustomRole_behavesAsBefore() throws Exception {
        stubCreate();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_BASE))
                .andExpect(status().isCreated());

        verifyNoInteractions(tenantMembershipRepository);
        verifyNoInteractions(permissionResolver);
        verify(roleRepository, never()).findByTenantIdAndKey(any(), anyString());
    }
}
