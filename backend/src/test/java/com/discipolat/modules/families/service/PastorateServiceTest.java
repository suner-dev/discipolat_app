package com.discipolat.modules.families.service;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.families.domain.PastorateAppointment;
import com.discipolat.modules.families.repository.PastorateAppointmentRepository;
import com.discipolat.modules.families.repository.PastorateTransferRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
 * G4.3/G4.4 — Règles du pastorat : un seul mandat actif par berger et par unité,
 * nomination qui notifie + publie PastorAppointed + bump des permissions,
 * et correction du parse endDate (chaîne ISO depuis JSON).
 */
@ExtendWith(MockitoExtension.class)
class PastorateServiceTest {

    @Mock private PastorateAppointmentRepository appointmentRepository;
    @Mock private PastorateTransferRepository transferRepository;
    @Mock private OrganizationNodeRepository orgNodeRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private OutboxPublisher outboxPublisher;
    @Mock private NotificationService notificationService;
    @Mock private PermissionResolver permissionResolver;

    private PastorateService service;

    private UUID tenantId;
    private UUID pastorId;
    private UUID unitA;
    private UUID unitB;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        service = new PastorateService(appointmentRepository, transferRepository, orgNodeRepository,
                userRepository, securityUtils, outboxPublisher, notificationService, permissionResolver);
        tenantId = UUID.randomUUID();
        pastorId = UUID.randomUUID();
        unitA = UUID.randomUUID();
        unitB = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    private PastorateAppointment appointment(UUID id, UUID pastor, UUID unit, String status) {
        return PastorateAppointment.builder()
                .id(id).tenantId(tenantId).pastorId(pastor).organizationUnitId(unit)
                .roleCode("PASTOR_CAMPUS").appointmentType("MANUAL")
                .startDate(LocalDate.of(2025, 1, 1)).status(status)
                .createdBy(actorId)
                .build();
    }

    private void stubBasics() {
        OrganizationNode unit = new OrganizationNode();
        unit.setId(unitA);
        unit.setTenantId(tenantId);
        unit.setName("Campus Centre");
        when(orgNodeRepository.findById(unitA)).thenReturn(Optional.of(unit));
        User pastor = new User();
        pastor.setId(pastorId);
        pastor.setTenantId(tenantId);
        when(userRepository.findById(pastorId)).thenReturn(Optional.of(pastor));
    }

    @Test
    void createAppointment_notifiesPastor_publishesEvent_andBumpsPermissions() {
        stubBasics();
        when(appointmentRepository.findByTenantIdAndPastorIdAndDeletedAtIsNull(tenantId, pastorId))
                .thenReturn(List.of());
        when(appointmentRepository.findByTenantIdAndOrganizationUnitIdAndDeletedAtIsNull(tenantId, unitA))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(PastorateAppointment.class))).thenAnswer(inv -> {
            PastorateAppointment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        PastorateAppointment result = service.createAppointment(tenantId, actorId,
                appointment(null, pastorId, unitA, "ACTIVE"));

        assertEquals("ACTIVE", result.getStatus());
        verify(outboxPublisher).publish(eq(tenantId), eq("PASTORATE"), any(UUID.class),
                eq("PastorAppointed"), any(Map.class));
        verify(notificationService).create(eq(tenantId), eq(pastorId), eq(TypeNotification.PASTORAT_NOMINATION),
                eq(CanalNotification.IN_APP), anyString(), anyString(), any(UUID.class), eq("PASTORATE_APPOINTMENT"));
        verify(permissionResolver).bumpPermissions(tenantId, pastorId, "PASTOR_APPOINTED");
    }

    @Test
    void createAppointment_rejectsDoubleActiveMandateOnAnotherUnit() {
        stubBasics();
        when(appointmentRepository.findByTenantIdAndPastorIdAndDeletedAtIsNull(tenantId, pastorId))
                .thenReturn(List.of(appointment(UUID.randomUUID(), pastorId, unitB, "ACTIVE")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.createAppointment(tenantId, actorId,
                        appointment(null, pastorId, unitA, "ACTIVE")));
        assertEquals(true, ex.getMessage().contains("mandat actif"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createAppointment_rejectsSecondActivePastorOnSameUnitAndRole() {
        stubBasics();
        UUID otherPastor = UUID.randomUUID();
        when(appointmentRepository.findByTenantIdAndPastorIdAndDeletedAtIsNull(tenantId, pastorId))
                .thenReturn(List.of());
        when(appointmentRepository.findByTenantIdAndOrganizationUnitIdAndDeletedAtIsNull(tenantId, unitA))
                .thenReturn(List.of(appointment(UUID.randomUUID(), otherPastor, unitA, "ACTIVE")));

        assertThrows(IllegalStateException.class,
                () -> service.createAppointment(tenantId, actorId,
                        appointment(null, pastorId, unitA, "ACTIVE")));
    }

    @Test
    void updateAppointment_parsesIsoStringEndDate() {
        UUID aptId = UUID.randomUUID();
        PastorateAppointment existing = appointment(aptId, pastorId, unitA, "ACTIVE");
        when(appointmentRepository.findByIdAndTenantIdAndDeletedAtIsNull(aptId, tenantId))
                .thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PastorateAppointment updated = service.updateAppointment(tenantId, actorId, aptId,
                Map.of("endDate", "2026-12-31"));

        assertEquals(LocalDate.of(2026, 12, 31), updated.getEndDate());
    }

    @Test
    void endAppointment_bumpsPermissionsAndPublishes() {
        UUID aptId = UUID.randomUUID();
        when(appointmentRepository.findByIdAndTenantIdAndDeletedAtIsNull(aptId, tenantId))
                .thenReturn(Optional.of(appointment(aptId, pastorId, unitA, "ACTIVE")));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.endAppointment(tenantId, actorId, aptId, "Fin de service");

        verify(outboxPublisher).publish(eq(tenantId), eq("PASTORATE"), eq(aptId),
                eq("PastorEnded"), any(Map.class));
        verify(permissionResolver).bumpPermissions(tenantId, pastorId, "PASTOR_ENDED");
    }
}
