package com.discipolat.common.infrastructure.scheduler;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TransferStatus;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.appointments.domain.AppointmentService;
import com.discipolat.modules.authentication.domain.ActivationTokenRepository;
import com.discipolat.modules.authentication.domain.PasswordResetTokenRepository;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.departments.domain.DepartmentSetting;
import com.discipolat.modules.departments.domain.DepartmentSettingsService;
import com.discipolat.modules.departments.domain.DepartmentTask;
import com.discipolat.modules.departments.domain.DepartmentTaskRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRegistrationRepository;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.notifications.domain.NotificationRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.transfers.domain.TransferRequest;
import com.discipolat.modules.transfers.domain.TransferRequestRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.workflow.domain.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests du job planifié qui alerte le pasteur lorsque le délai de traitement
 * d'une demande de transfert est dépassé (déduplication : une seule
 * notification par demande et par pasteur).
 */
@ExtendWith(MockitoExtension.class)
class ScheduledJobsTest {

    @Mock private SoulRepository soulRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private NotificationService notificationService;
    @Mock private NotificationRepository notificationRepository;
    @Mock private MakerReportRepository makerReportRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @Mock private ActivationTokenRepository activationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private WorkflowService workflowService;
    @Mock private AppointmentService appointmentService;
    @Mock private TransferRequestRepository transferRequestRepository;
    @Mock private DepartmentTaskRepository departmentTaskRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private DepartmentSettingsService departmentSettingsService;

    private ScheduledJobs jobs;

    private final UUID pasteurId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jobs = new ScheduledJobs(soulRepository, alertRepository, notificationService,
                makerReportRepository, userRepository, eventRepository, eventRegistrationRepository,
                activationTokenRepository, passwordResetTokenRepository, workflowService,
                appointmentService, transferRequestRepository, notificationRepository,
                departmentTaskRepository, departmentRepository, departmentSettingsService);
    }

    private User pasteur() {
        return User.builder().id(pasteurId).email("pasteur@eglise.org")
                .role(UserRole.PASTEUR).build();
    }

    private TransferRequest demandeEnRetard(TransferStatus statut) {
        return TransferRequest.builder()
                .id(UUID.randomUUID())
                .statut(statut)
                .type(com.discipolat.common.enums.TransferType.FAISEUR_FAMILLE_TRANSFERT)
                .personneId(UUID.randomUUID())
                .personneType("USER")
                .demandeurId(UUID.randomUUID())
                .justification("Test")
                .dateSoumission(LocalDateTime.now().minusDays(5))
                .delaiLimite(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    void checkTransferDelays_AucuneDemandeEnRetard_NeFaitRien() {
        when(transferRequestRepository.findByStatutInAndDelaiLimiteBefore(any(), any()))
                .thenReturn(List.of());

        jobs.checkTransferDelays();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkTransferDelays_DemandeEnRetard_NotifieLePasteur() {
        TransferRequest req = demandeEnRetard(TransferStatus.EN_ATTENTE_VALIDATION);
        when(transferRequestRepository.findByStatutInAndDelaiLimiteBefore(any(), any()))
                .thenReturn(List.of(req));
        when(userRepository.findByRolesContaining(UserRole.PASTEUR)).thenReturn(List.of(pasteur()));
        when(notificationRepository.existsByDestinataireIdAndTypeAndEntiteReferenceIdAndEntiteReferenceType(
                pasteurId, TypeNotification.TRANSFERT_DELAI_DEPASSE, req.getId(), "TRANSFER"))
                .thenReturn(false);

        jobs.checkTransferDelays();

        verify(notificationService).create(
                eq(req.getTenantId()), eq(pasteurId), eq(TypeNotification.TRANSFERT_DELAI_DEPASSE),
                eq(CanalNotification.IN_APP), any(), any(), eq(req.getId()), eq("TRANSFER"));
    }

    @Test
    void checkTransferDelays_DemandeValidationPartielleEnRetard_Notifie() {
        TransferRequest req = demandeEnRetard(TransferStatus.VALIDATION_PARTIELLE);
        when(transferRequestRepository.findByStatutInAndDelaiLimiteBefore(any(), any()))
                .thenReturn(List.of(req));
        when(userRepository.findByRolesContaining(UserRole.PASTEUR)).thenReturn(List.of(pasteur()));
        when(notificationRepository.existsByDestinataireIdAndTypeAndEntiteReferenceIdAndEntiteReferenceType(
                any(), any(), any(), any())).thenReturn(false);

        jobs.checkTransferDelays();

        verify(notificationService, times(1)).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkTransferDelays_DejaNotifie_NeRenotifiePas() {
        TransferRequest req = demandeEnRetard(TransferStatus.EN_ATTENTE_VALIDATION);
        when(transferRequestRepository.findByStatutInAndDelaiLimiteBefore(any(), any()))
                .thenReturn(List.of(req));
        when(userRepository.findByRolesContaining(UserRole.PASTEUR)).thenReturn(List.of(pasteur()));
        when(notificationRepository.existsByDestinataireIdAndTypeAndEntiteReferenceIdAndEntiteReferenceType(
                pasteurId, TypeNotification.TRANSFERT_DELAI_DEPASSE, req.getId(), "TRANSFER"))
                .thenReturn(true);

        jobs.checkTransferDelays();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkTransferDelays_PasDePasteur_NeNotifiePersonne() {
        TransferRequest req = demandeEnRetard(TransferStatus.EN_ATTENTE_VALIDATION);
        when(transferRequestRepository.findByStatutInAndDelaiLimiteBefore(any(), any()))
                .thenReturn(List.of(req));
        when(userRepository.findByRolesContaining(UserRole.PASTEUR)).thenReturn(List.of());

        jobs.checkTransferDelays();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkTransferDelays_UneNotificationParDemandeEtParPasteur() {
        TransferRequest req1 = demandeEnRetard(TransferStatus.EN_ATTENTE_VALIDATION);
        TransferRequest req2 = demandeEnRetard(TransferStatus.VALIDATION_PARTIELLE);
        User pasteur2 = User.builder().id(UUID.randomUUID()).email("pasteur2@eglise.org")
                .role(UserRole.PASTEUR).build();
        when(transferRequestRepository.findByStatutInAndDelaiLimiteBefore(any(), any()))
                .thenReturn(List.of(req1, req2));
        when(userRepository.findByRolesContaining(UserRole.PASTEUR)).thenReturn(List.of(pasteur(), pasteur2));
        when(notificationRepository.existsByDestinataireIdAndTypeAndEntiteReferenceIdAndEntiteReferenceType(
                any(), any(), any(), any())).thenReturn(false);

        jobs.checkTransferDelays();

        // 2 demandes × 2 pasteurs = 4 notifications
        verify(notificationService, times(4)).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkOverdueDepartmentTasks_NoResponsable_SkipsNotification() {
        DepartmentTask task = DepartmentTask.builder().id(UUID.randomUUID())
                .departmentId(UUID.randomUUID()).titre("Tâche en retard").build();
        when(departmentTaskRepository.findByStatutInAndEcheanceBefore(any(), any()))
                .thenReturn(List.of(task));
        when(departmentRepository.findById(task.getDepartmentId())).thenReturn(Optional.empty());

        jobs.checkOverdueDepartmentTasks();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkOverdueDepartmentTasks_NotifiesResponsable() {
        UUID responsableId = UUID.randomUUID();
        DepartmentTask task = DepartmentTask.builder().id(UUID.randomUUID())
                .departmentId(UUID.randomUUID()).titre("Tâche en retard").build();
        Department dept = new Department();
        dept.setResponsableId(responsableId);
        when(departmentTaskRepository.findByStatutInAndEcheanceBefore(any(), any()))
                .thenReturn(List.of(task));
        when(departmentRepository.findById(task.getDepartmentId())).thenReturn(Optional.of(dept));
        when(notificationRepository.existsByDestinataireIdAndTypeAndEntiteReferenceIdAndEntiteReferenceType(
                responsableId, TypeNotification.TACHE_EN_RETARD, task.getId(), "TASK"))
                .thenReturn(false);

        jobs.checkOverdueDepartmentTasks();

        verify(notificationService).create(
                eq(dept.getTenantId()), eq(responsableId), eq(TypeNotification.TACHE_EN_RETARD),
                eq(CanalNotification.IN_APP), any(), any(), eq(task.getId()), eq("TASK"));
    }

    // ==================== RAPPELS D'ÉVÉNEMENTS DE DÉPARTEMENT ====================

    @Test
    void sendEventReminders_DepartementJN_NotifieLeResponsable() {
        UUID departmentId = UUID.randomUUID();
        UUID responsableId = UUID.randomUUID();
        Event event = Event.builder()
                .id(UUID.randomUUID())
                .departmentId(departmentId)
                .titre("Convention du département")
                .lieu("Temple")
                .dateDebut(LocalDateTime.now().plusDays(3))
                .build();
        Department dept = new Department();
        dept.setResponsableId(responsableId);
        dept.setNom("Jeunesse");
        DepartmentSetting settings = DepartmentSetting.builder()
                .departmentId(departmentId).eventRappelJours(3).build();
        // Pas d'événement J-1 (démo générique vide)
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any())).thenReturn(List.of());
        when(eventRepository.findByDepartmentIdIsNotNullAndDeletedFalseAndDateDebutBetween(any(), any()))
                .thenReturn(List.of(event));
        when(departmentSettingsService.effectiveSettings(departmentId)).thenReturn(settings);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));
        when(notificationRepository.existsByDestinataireIdAndTypeAndEntiteReferenceIdAndEntiteReferenceType(
                responsableId, TypeNotification.EVENEMENT_RAPPEL, event.getId(), "EVENT"))
                .thenReturn(false);

        jobs.sendEventReminders();

        verify(notificationService).create(
                eq(event.getTenantId()), eq(responsableId), eq(TypeNotification.EVENEMENT_RAPPEL),
                eq(CanalNotification.IN_APP), any(), any(), eq(event.getId()), eq("EVENT"));
    }

    @Test
    void sendEventReminders_DepartementRappelDesactive_NeNotifiePas() {
        UUID departmentId = UUID.randomUUID();
        Event event = Event.builder()
                .id(UUID.randomUUID())
                .departmentId(departmentId)
                .titre("Événement sans rappel")
                .dateDebut(LocalDateTime.now().plusDays(2))
                .build();
        DepartmentSetting settings = DepartmentSetting.builder()
                .departmentId(departmentId).eventRappelJours(0).build();
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any())).thenReturn(List.of());
        when(eventRepository.findByDepartmentIdIsNotNullAndDeletedFalseAndDateDebutBetween(any(), any()))
                .thenReturn(List.of(event));
        when(departmentSettingsService.effectiveSettings(departmentId)).thenReturn(settings);

        jobs.sendEventReminders();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void sendEventReminders_DepartementDejaNotifie_NeRenotifiePas() {
        UUID departmentId = UUID.randomUUID();
        UUID responsableId = UUID.randomUUID();
        Event event = Event.builder()
                .id(UUID.randomUUID())
                .departmentId(departmentId)
                .titre("Convention du département")
                .dateDebut(LocalDateTime.now().plusDays(3))
                .build();
        Department dept = new Department();
        dept.setResponsableId(responsableId);
        DepartmentSetting settings = DepartmentSetting.builder()
                .departmentId(departmentId).eventRappelJours(3).build();
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any())).thenReturn(List.of());
        when(eventRepository.findByDepartmentIdIsNotNullAndDeletedFalseAndDateDebutBetween(any(), any()))
                .thenReturn(List.of(event));
        when(departmentSettingsService.effectiveSettings(departmentId)).thenReturn(settings);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));
        when(notificationRepository.existsByDestinataireIdAndTypeAndEntiteReferenceIdAndEntiteReferenceType(
                responsableId, TypeNotification.EVENEMENT_RAPPEL, event.getId(), "EVENT"))
                .thenReturn(true);

        jobs.sendEventReminders();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void sendEventReminders_DepartementSansResponsable_NeNotifiePas() {
        UUID departmentId = UUID.randomUUID();
        Event event = Event.builder()
                .id(UUID.randomUUID())
                .departmentId(departmentId)
                .titre("Événement sans responsable")
                .dateDebut(LocalDateTime.now().plusDays(1))
                .build();
        DepartmentSetting settings = DepartmentSetting.builder()
                .departmentId(departmentId).eventRappelJours(1).build();
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any())).thenReturn(List.of());
        when(eventRepository.findByDepartmentIdIsNotNullAndDeletedFalseAndDateDebutBetween(any(), any()))
                .thenReturn(List.of(event));
        when(departmentSettingsService.effectiveSettings(departmentId)).thenReturn(settings);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        jobs.sendEventReminders();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
