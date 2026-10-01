package com.discipolat.modules.notifications.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * G5.4 (§55-2) — « Demander l'accès » : notification RÉELLE des responsables
 * (ADMIN/PASTEUR du tenant), anti-spam sur fenêtre, contexte résolu serveur.
 */
@ExtendWith(MockitoExtension.class)
class AccessRequestServiceTest {

    @Mock
    private AccessRequestRepository accessRequestRepository;
    @Mock
    private TenantMembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private AccessRequestService service;

    private UUID tenantId;
    private UUID requesterId;
    private UUID adminId;
    private UUID pasteurId;
    private UUID membreId;

    @BeforeEach
    void setUp() {
        service = new AccessRequestService(
                accessRequestRepository, membershipRepository, userRepository, notificationService);
        tenantId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        pasteurId = UUID.randomUUID();
        membreId = UUID.randomUUID();
    }

    private TenantMembership membership(UUID userId, String roleKey) {
        TenantMembership m = new TenantMembership();
        m.setUserId(userId);
        m.setTenantId(tenantId);
        Role role = new Role();
        role.setKey(roleKey);
        m.setRole(role);
        return m;
    }

    private AccessRequest savedWithId(AccessRequest r) {
        r.setId(UUID.randomUUID());
        return r;
    }

    @Test
    void notifiesAdminAndPasteurAndTracesRequest() {
        when(accessRequestRepository.findFirstByTenantIdAndUserIdAndPermissionKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(tenantId), eq(requesterId), eq("ORG_NODE_MOVE"), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(
                User.builder().id(requesterId).firstName("Marie").lastName("Dupont").build()));
        when(accessRequestRepository.save(any(AccessRequest.class))).thenAnswer(inv ->
                savedWithId(inv.getArgument(0)));
        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(
                        membership(adminId, "ADMIN"),
                        membership(pasteurId, "PASTEUR"),
                        membership(membreId, "MEMBRE"),
                        membership(requesterId, "ADMIN")));

        AccessRequestService.AccessRequestOutcome outcome =
                service.request(tenantId, requesterId, " org_node_move ", "Arbre d'organisation", "Besoin de réorganiser les cellules.");

        assertEquals("NOTIFIED", outcome.status());
        assertEquals(2, outcome.notifiedResponsibles(),
                "ADMIN + PASTEUR notifiés ; MEMBRE et le demandeur lui-même exclus");

        ArgumentCaptor<UUID> targets = ArgumentCaptor.forClass(UUID.class);
        verify(notificationService, times(2)).create(targets.capture(),
                eq(TypeNotification.DEMANDE_ACCES), eq(CanalNotification.IN_APP),
                anyString(), anyString(), any(UUID.class), eq("ACCESS_REQUEST"));
        assertEquals(List.of(adminId, pasteurId), targets.getAllValues());

        ArgumentCaptor<AccessRequest> traced = ArgumentCaptor.forClass(AccessRequest.class);
        verify(accessRequestRepository, times(2)).save(traced.capture());
        AccessRequest created = traced.getAllValues().get(0);
        assertEquals("ORG_NODE_MOVE", created.getPermissionKey(), "clé normalisée en majuscules");
        assertEquals(tenantId, created.getTenantId());
        assertEquals(requesterId, created.getUserId());
        assertEquals(2, created.getNotifiedCount());
    }

    @Test
    void cooldownWindowPreventsDuplicateNotifications() {
        AccessRequest recent = AccessRequest.builder()
                .id(UUID.randomUUID()).tenantId(tenantId).userId(requesterId)
                .permissionKey("FINANCE_VIEW").createdAt(Instant.now()).build();
        when(accessRequestRepository.findFirstByTenantIdAndUserIdAndPermissionKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(tenantId), eq(requesterId), eq("FINANCE_VIEW"), any(Instant.class)))
                .thenReturn(Optional.of(recent));

        AccessRequestService.AccessRequestOutcome outcome =
                service.request(tenantId, requesterId, "FINANCE_VIEW", null, null);

        assertEquals("ALREADY_SENT", outcome.status());
        assertEquals(recent.getId(), outcome.requestId());
        verify(notificationService, never()).create(any(UUID.class), any(), any(),
                anyString(), anyString(), any(), anyString());
        verify(accessRequestRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidPermissionKey() {
        assertThrows(IllegalArgumentException.class,
                () -> service.request(tenantId, requesterId, "<script>", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.request(tenantId, requesterId, "A", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.request(tenantId, requesterId, null, null, null));
    }

    @Test
    void legacyRoleStringStillIdentifiesResponsible() {
        when(accessRequestRepository.findFirstByTenantIdAndUserIdAndPermissionKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(tenantId), eq(requesterId), eq("WORKFLOW_APPROVE"), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(userRepository.findById(requesterId)).thenReturn(Optional.empty());
        when(accessRequestRepository.save(any(AccessRequest.class))).thenAnswer(inv ->
                savedWithId(inv.getArgument(0)));
        TenantMembership legacy = new TenantMembership();
        legacy.setUserId(adminId);
        legacy.setRole(null);
        legacy.setRoleLegacy("ADMIN");
        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(legacy));

        AccessRequestService.AccessRequestOutcome outcome =
                service.request(tenantId, requesterId, "WORKFLOW_APPROVE", null, null);

        assertEquals(1, outcome.notifiedResponsibles());
        verify(notificationService).create(eq(adminId), eq(TypeNotification.DEMANDE_ACCES),
                eq(CanalNotification.IN_APP), anyString(), anyString(), any(UUID.class), eq("ACCESS_REQUEST"));
    }
}
