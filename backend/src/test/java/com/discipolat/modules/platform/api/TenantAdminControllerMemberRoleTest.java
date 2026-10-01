package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.domain.OrganizationNodeService;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.PermissionService;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleService;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantMembershipService;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantService;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * G5.5 (§58 / §G3.2) — Affectations tenant-scopées : changement de rôle et
 * révocation d'adhésion. Isolation serveur (une membership d'un autre tenant
 * est 404), protection du dernier propriétaire, traçabilité audit.
 */
@ExtendWith(MockitoExtension.class)
class TenantAdminControllerMemberRoleTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantMembershipService membershipService;
    @Mock private RoleService roleService;
    @Mock private PermissionService permissionService;
    @Mock private AuditService auditService;
    @Mock private OrganizationNodeService orgNodeService;
    @Mock private TenantService tenantService;

    private TenantAdminController controller;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID otherTenantId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID membershipId = UUID.randomUUID();
    private final UUID targetUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new TenantAdminController(tenantRepository, membershipRepository,
                userRepository, membershipService, roleService, permissionService,
                auditService, orgNodeService, tenantService);
        TenantContext.setTenantId(tenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminId, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private Role role(UUID id, String key) {
        Role r = new Role();
        r.setId(id);
        r.setKey(key);
        return r;
    }

    private TenantMembership membership(UUID mTenantId, Role role) {
        TenantMembership m = new TenantMembership();
        m.setId(membershipId);
        m.setTenantId(mTenantId);
        m.setUserId(targetUserId);
        m.setRole(role);
        m.setStatus(MembershipStatus.ACTIVE);
        return m;
    }

    @Test
    void changesRoleWithinTenantAndAudits() {
        UUID oldRoleId = UUID.randomUUID();
        UUID newRoleId = UUID.randomUUID();
        TenantMembership m = membership(tenantId, role(oldRoleId, "MEMBRE"));
        when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(m));
        when(roleService.getRoles(tenantId)).thenReturn(List.of(
                role(newRoleId, "RESPONSABLE"), role(oldRoleId, "MEMBRE")));

        Map<String, Object> result = controller.updateMemberRole(
                membershipId, Map.of("roleKey", "responsable"), null).getBody();

        assertEquals("RESPONSABLE", m.getRole().getKey());
        assertEquals(newRoleId, m.getRole().getId());
        verify(membershipRepository).save(m);
        verify(auditService).log(eq(adminId), eq(tenantId), eq("MEMBER_ROLE_CHANGED"),
                eq("TENANT_MEMBERSHIP"), eq(membershipId), eq("SUCCESS"), any(), any(), any(), any());
        assertEquals("RESPONSABLE", result.get("roleKey"));
        assertEquals(targetUserId.toString(), result.get("userId"));
    }

    @Test
    void foreignTenantMembershipIsNotFoundNeverLeaked() {
        when(membershipRepository.findById(membershipId))
                .thenReturn(Optional.of(membership(otherTenantId, role(UUID.randomUUID(), "MEMBRE"))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.updateMemberRole(membershipId, Map.of("roleKey", "ADMIN"), null));
        org.junit.jupiter.api.Assertions.assertTrue(ex.toString().contains("404"),
                "404 pour une membership d'un autre tenant (pas de fuite 403)");
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void unknownRoleKeyRejected() {
        when(membershipRepository.findById(membershipId))
                .thenReturn(Optional.of(membership(tenantId, role(UUID.randomUUID(), "MEMBRE"))));
        when(roleService.getRoles(tenantId)).thenReturn(List.of(role(UUID.randomUUID(), "MEMBRE")));

        assertThrows(IllegalArgumentException.class,
                () -> controller.updateMemberRole(membershipId, Map.of("roleKey", "SUPER_DICTATEUR"), null));
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void lastOwnerCannotBeDemoted() {
        Role owner = role(UUID.randomUUID(), "TENANT_OWNER");
        TenantMembership m = membership(tenantId, owner);
        when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(m));
        when(roleService.getRoles(tenantId)).thenReturn(List.of(
                owner, role(UUID.randomUUID(), "ADMIN")));
        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(m));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.updateMemberRole(membershipId, Map.of("roleKey", "ADMIN"), null));
        assertEquals(true, ex.getMessage().contains("dernier propriétaire"));
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void revokeMemberSetsRevokedAndAudits() {
        TenantMembership m = membership(tenantId, role(UUID.randomUUID(), "FAISEUR"));
        when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(m));

        controller.revokeMember(membershipId, null);

        assertEquals(MembershipStatus.REVOKED, m.getStatus());
        verify(membershipRepository).save(m);
        verify(auditService).log(eq(adminId), eq(tenantId), eq("MEMBER_REVOKED"),
                eq("TENANT_MEMBERSHIP"), eq(membershipId), eq("SUCCESS"), any(), any(), any(), any());
    }

    @Test
    void cannotRevokeSelf() {
        TenantMembership m = membership(tenantId, role(UUID.randomUUID(), "TENANT_OWNER"));
        m.setUserId(adminId);
        when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(m));

        assertThrows(IllegalArgumentException.class, () -> controller.revokeMember(membershipId, null));
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void toggleModuleAuditAttributesUserNotTenant() {
        // FIX G5.5 : l'auteur audité est l'utilisateur courant, pas le tenant.
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setFeaturesJson("{\"members\":true}");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        controller.toggleModule("members", Map.of("enabled", false), null);

        verify(auditService).log(eq(adminId), eq(tenantId), eq("MODULE_TOGGLED"),
                eq("TENANT"), eq(tenantId), eq("SUCCESS"), any(), any(), any(), any());
    }
}
