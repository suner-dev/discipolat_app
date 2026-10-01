package com.discipolat.modules.families.service;

import com.discipolat.modules.core.repository.PermissionVersionRepository;
import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.families.domain.PastorateAppointment;
import com.discipolat.modules.families.repository.PastorateAppointmentRepository;
import com.discipolat.modules.people.repository.RoleAssignmentRepository;
import com.discipolat.modules.people.repository.SpaceMembershipRepository;
import com.discipolat.modules.tenants.domain.Permission;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * G4.4 — Rôles vivantes : les mandats pastoraux ACTIVE en cours contribuent
 * aux permissions résolues ; bumpPermissions recalcule le cache ET publie
 * PermissionsChanged (push WS < 5 s).
 */
@ExtendWith(MockitoExtension.class)
class PermissionResolverPastorateTest {

    @Mock private RoleAssignmentRepository roleAssignmentRepository;
    @Mock private SpaceMembershipRepository spaceMembershipRepository;
    @Mock private PastorateAppointmentRepository pastorateAppointmentRepository;
    @Mock private TenantMembershipRepository tenantMembershipRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionVersionRepository permissionVersionRepository;
    @Mock private OutboxPublisher outboxPublisher;

    private PermissionResolver resolver;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        resolver = new PermissionResolver(roleAssignmentRepository, spaceMembershipRepository,
                pastorateAppointmentRepository, tenantMembershipRepository, roleRepository,
                permissionVersionRepository, outboxPublisher);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private PastorateAppointment apt(String status, LocalDate start, LocalDate end) {
        return PastorateAppointment.builder()
                .id(UUID.randomUUID()).tenantId(tenantId).pastorId(userId)
                .organizationUnitId(UUID.randomUUID()).roleCode("PASTOR_CAMPUS")
                .appointmentType("MANUAL").startDate(start).endDate(end).status(status)
                .createdBy(userId)
                .build();
    }

    private void stubEmptyOtherSources() {
        when(tenantMembershipRepository.findAllByUserIdAndTenantIdAndStatus(any(), any(), any()))
                .thenReturn(List.of());
        when(roleAssignmentRepository.findByTenantIdAndPersonIdAndStatus(any(), any(), any()))
                .thenReturn(List.of());
        when(spaceMembershipRepository.findByPersonIdAndStatus(any(), any())).thenReturn(List.of());
    }

    private void stubCampusRole() {
        Permission perm = Permission.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .key("PEOPLE_CAMPUS_READ").build();
        Role role = Role.builder().id(UUID.randomUUID()).tenantId(tenantId).key("PASTOR_CAMPUS")
                .permissions(Set.of(perm)).build();
        when(roleRepository.findByTenantIdAndKey(tenantId, "PASTOR_CAMPUS")).thenReturn(Optional.of(role));
    }

    @Test
    void activePastorateAppointment_contributesPermissions() {
        stubEmptyOtherSources();
        when(pastorateAppointmentRepository.findByTenantIdAndPastorIdAndDeletedAtIsNull(tenantId, userId))
                .thenReturn(List.of(apt("ACTIVE", LocalDate.now().minusDays(10), null)));
        stubCampusRole();

        Set<String> perms = resolver.resolvePermissions(tenantId, userId);
        assertTrue(perms.contains("PEOPLE_CAMPUS_READ"));
    }

    @Test
    void endedOrFutureAppointment_contributesNothing() {
        stubEmptyOtherSources();
        when(pastorateAppointmentRepository.findByTenantIdAndPastorIdAndDeletedAtIsNull(tenantId, userId))
                .thenReturn(List.of(
                        apt("ENDED", LocalDate.now().minusDays(30), LocalDate.now().minusDays(1)),
                        apt("ACTIVE", LocalDate.now().plusDays(5), null)));

        Set<String> perms = resolver.resolvePermissions(tenantId, userId);
        assertFalse(perms.contains("PEOPLE_CAMPUS_READ"));
    }

    @Test
    void bumpPermissions_refreshesCacheAndPublishesChanged() {
        stubEmptyOtherSources();
        when(pastorateAppointmentRepository.findByTenantIdAndPastorIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(List.of());
        when(permissionVersionRepository.findByTenantIdAndUserId(tenantId, userId)).thenReturn(Optional.empty());

        resolver.bumpPermissions(tenantId, userId, "PASTOR_APPOINTED");

        verify(permissionVersionRepository).save(any());
        verify(outboxPublisher).publish(tenantId, "PERMISSION", userId, "PermissionsChanged",
                java.util.Map.of("userId", userId.toString(), "changeType", "PASTOR_APPOINTED"));
    }
}
