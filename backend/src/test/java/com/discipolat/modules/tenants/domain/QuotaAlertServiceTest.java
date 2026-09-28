package com.discipolat.modules.tenants.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.notifications.domain.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Constat M3 — l'alerte de quota ne doit atteindre que les administrateurs du
 * tenant, et ne doit jamais être bloquante.
 */
@ExtendWith(MockitoExtension.class)
class QuotaAlertServiceTest {

    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private NotificationService notificationService;

    private QuotaAlertService service;
    private UUID tenantId;
    private UUID ownerId;
    private UUID adminId;
    private UUID pasteurId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        service = new QuotaAlertService(membershipRepository, notificationService);
        tenantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        pasteurId = UUID.randomUUID();
        memberId = UUID.randomUUID();
    }

    private TenantMembership membership(UUID userId, String roleKey) {
        return TenantMembership.builder()
                .tenantId(tenantId)
                .userId(userId)
                .roleLegacy(roleKey)
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Seuls TENANT_OWNER et TENANT_ADMIN sont alertés")
    void onlyTenantAdminsAreNotified() {
        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(
                        membership(ownerId, "TENANT_OWNER"),
                        membership(adminId, "tenant_admin"),
                        membership(pasteurId, "PASTEUR"),
                        membership(memberId, "MEMBRE")));

        service.alertQuotaExceeded(tenantId, "spaces", 25, 25);

        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(notificationService, times(2)).create(
                eq(tenantId), captor.capture(),
                eq(TypeNotification.INFORMATION), eq(CanalNotification.IN_APP),
                anyString(), anyString(), eq(tenantId), eq("QUOTA"));
        assertThat(captor.getAllValues()).containsExactlyInAnyOrder(ownerId, adminId);
    }

    @Test
    @DisplayName("Le titre et le message sont lisibles par un humain")
    void titleAndMessageAreHumanReadable() {
        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership(ownerId, "TENANT_OWNER")));

        service.alertQuotaExceeded(tenantId, "events", 200, 200);

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).create(eq(tenantId), eq(ownerId),
                eq(TypeNotification.INFORMATION), eq(CanalNotification.IN_APP),
                title.capture(), message.capture(), eq(tenantId), eq("QUOTA"));

        assertThat(title.getValue()).isEqualTo("Quota atteint : événements");
        assertThat(message.getValue()).contains("200").contains("200").contains("abonnement");
    }

    @Test
    @DisplayName("Une notification en échec n'interrompt pas les autres administrateurs")
    void oneFailedNotificationDoesNotStopTheOthers() {
        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(
                        membership(ownerId, "TENANT_OWNER"),
                        membership(adminId, "TENANT_ADMIN")));
        doThrow(new IllegalStateException("table notifications indisponible"))
                .when(notificationService).create(eq(tenantId), eq(ownerId),
                        any(TypeNotification.class), any(CanalNotification.class),
                        anyString(), anyString(), any(), anyString());

        service.alertQuotaExceeded(tenantId, "spaces", 25, 25);

        // L'administrateur suivant a bien été notifié malgré l'échec du premier.
        verify(notificationService).create(eq(tenantId), eq(adminId),
                eq(TypeNotification.INFORMATION), eq(CanalNotification.IN_APP),
                anyString(), anyString(), eq(tenantId), eq("QUOTA"));
    }

    @Test
    @DisplayName("Aucun administrateur : aucune notification, aucune exception")
    void noAdminMeansNoNotification() {
        when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of());

        service.alertQuotaExceeded(tenantId, "spaces", 25, 25);

        verify(notificationService, never()).create(
                any(), any(), any(TypeNotification.class), any(CanalNotification.class),
                anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Tenant nul : aucune requête, aucune notification")
    void nullTenantIsIgnored() {
        service.alertQuotaExceeded(null, "spaces", 25, 25);

        verify(membershipRepository, never()).findByTenantIdAndStatus(any(), any(MembershipStatus.class));
        verifyNoNotification();
    }

    private void verifyNoNotification() {
        verify(notificationService, never()).create(
                any(), any(), any(TypeNotification.class), any(CanalNotification.class),
                anyString(), anyString(), any(), anyString());
    }
}
