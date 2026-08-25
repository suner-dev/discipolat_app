package com.discipolat.modules.communications.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.communications.api.CommunicationRequest;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

@ExtendWith(MockitoExtension.class)
class CommunicationServiceTest {

    @Mock private CommunicationRepository communicationRepository;
    @Mock private NotificationService notificationService;
    @Mock private SecurityUtils securityUtils;
    @Mock private AuditService auditService;
    @Mock private com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher propagationPublisher;
    @Mock private UserRepository userRepository;
    @Mock private SoulRepository soulRepository;
    @Mock private SoulDepartmentRepository soulDepartmentRepository;
    @Mock private DepartmentRepository departmentRepository;

    private CommunicationService service;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        service = new CommunicationService(communicationRepository, notificationService, securityUtils,
                auditService, propagationPublisher, userRepository, soulRepository, soulDepartmentRepository, departmentRepository);
    }

    private User user(UUID id, UserRole role) {
        return User.builder().id(id).email(id + "@test").role(role)
                .roles(Set.of(role)).statut(UserStatus.ACTIVE).build();
    }

    @Test
    void publish_toUs_notifiesEveryActiveUser() {
        Communication c = Communication.builder().id(UUID.randomUUID())
                .titre("Rentrée").contenu("La rentrée commence dimanche.")
                .cible(Communication.Cible.TOUS).build();
        when(communicationRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(communicationRepository.save(any(Communication.class))).thenReturn(c);
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        when(userRepository.findAll()).thenReturn(List.of(user(u1, UserRole.PASTEUR), user(u2, UserRole.MEMBRE)));

        java.util.Map<String, Object> result = service.publish(c.getId());

        assertThat(result).containsEntry("statut", "PUBLIEE").containsEntry("destinataires", 2);
        verify(notificationService, times(2)).create(any(), eq(TypeNotification.INFORMATION),
                eq(CanalNotification.IN_APP), any(), any(), eq(c.getId()), eq("COMMUNICATION"));
        verify(propagationPublisher).publishStatusChanged(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void publish_byRole_notifiesOnlyUsersWithThatRole() {
        Communication c = Communication.builder().id(UUID.randomUUID())
                .titre("Conseil").contenu("Réunion du conseil.")
                .cible(Communication.Cible.ROLE).roles(List.of("RESPONSABLE")).build();
        when(communicationRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(communicationRepository.save(any(Communication.class))).thenReturn(c);
        UUID resp = UUID.randomUUID();
        when(userRepository.findByRolesContaining(UserRole.RESPONSABLE)).thenReturn(List.of(user(resp, UserRole.RESPONSABLE)));

        java.util.Map<String, Object> result = service.publish(c.getId());

        assertThat(result).containsEntry("destinataires", 1);
        verify(notificationService).create(eq(resp), any(), any(), any(), any(), eq(c.getId()), eq("COMMUNICATION"));
    }

    @Test
    void listForCurrentUser_returnsOnlyPublishedMatchingTarget() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Communication visible = Communication.builder().id(UUID.randomUUID())
                .titre("À tous").contenu("…").cible(Communication.Cible.TOUS).build();
        Communication invisible = Communication.builder().id(UUID.randomUUID())
                .titre("Autre cible").contenu("…").cible(Communication.Cible.ROLE)
                .roles(List.of("ADMIN")).build();
        SecurityTestHelper.loginAs(me);
        when(communicationRepository.findByDeletedFalseAndStatutOrderByDatePublicationDesc(Communication.Statut.PUBLIEE))
                .thenReturn(List.of(visible, invisible));
        when(userRepository.findAll()).thenReturn(List.of(user(me, UserRole.MEMBRE), user(other, UserRole.ADMIN)));
        when(userRepository.findByRolesContaining(UserRole.ADMIN)).thenReturn(List.of(user(other, UserRole.ADMIN)));

        List<java.util.Map<String, Object>> result = service.listForCurrentUser();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("titre", "À tous");
    }

    @Test
    void create_setsTargetFieldsByCible() {
        SecurityTestHelper.loginAs(USER_ID);
        when(communicationRepository.save(any(Communication.class))).thenAnswer(inv -> inv.getArgument(0));

        CommunicationRequest request = new CommunicationRequest(
                "Annonce famille", "Votre famille est invitée.",
                Communication.Cible.FAMILLE, null, UUID.randomUUID(), null);

        java.util.Map<String, Object> result = service.create(request);

        assertThat(result).containsEntry("cible", "FAMILLE").containsEntry("familleId", request.familleId());
        verify(propagationPublisher).publishCreated(any(), any(), any(), anyString());
    }

    @Test
    void publish_byDepartment_targetsResponsableAndMemberUsers() {
        UUID deptId = UUID.randomUUID();
        UUID responsableId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        UUID memberSoulId = UUID.randomUUID();
        Communication c = Communication.builder().id(UUID.randomUUID())
                .titre("Événement").contenu("…").cible(Communication.Cible.DEPARTEMENT)
                .departmentId(deptId).build();
        when(communicationRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(communicationRepository.save(any(Communication.class))).thenReturn(c);
        when(departmentRepository.findById(deptId))
                .thenReturn(Optional.of(Department.builder().id(deptId).responsableId(responsableId).build()));
        com.discipolat.modules.souls.domain.SoulDepartment link =
                com.discipolat.modules.souls.domain.SoulDepartment.builder()
                        .soulId(memberSoulId).departmentId(deptId).build();
        when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId)).thenReturn(List.of(link));
        when(soulRepository.findAllById(List.of(memberSoulId)))
                .thenReturn(List.of(Soul.builder().id(memberSoulId).userId(memberUserId).build()));

        java.util.Map<String, Object> result = service.publish(c.getId());

        assertThat(result).containsEntry("destinataires", 2);
        verify(notificationService).create(eq(responsableId), any(), any(), any(), any(), eq(c.getId()), eq("COMMUNICATION"));
        verify(notificationService).create(eq(memberUserId), any(), any(), any(), any(), eq(c.getId()), eq("COMMUNICATION"));
    }
}
