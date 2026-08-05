package com.discipolat.modules.appointments.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.appointments.api.AppointmentResponse;
import com.discipolat.modules.appointments.api.CreateAppointmentRequest;
import com.discipolat.modules.appointments.api.UpdateAppointmentStatusRequest;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private AppointmentService appointmentService;

    private Appointment appointment(UUID demandeur, UUID recepteur, Appointment.Statut statut) {
        return Appointment.builder()
                .id(UUID.randomUUID())
                .demandeurId(demandeur)
                .recepteurId(recepteur)
                .motif(Appointment.Motif.CONSEIL)
                .datePrevue(LocalDateTime.now().plusDays(2))
                .dureeMinutes(30)
                .statut(statut)
                .build();
    }

    @Test
    void create_shouldSaveAndNotifyRecepteur() {
        UUID demandeur = UUID.randomUUID();
        UUID recepteur = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(demandeur);
        when(userRepository.existsById(recepteur)).thenReturn(true);
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse result = appointmentService.create(new CreateAppointmentRequest(
                recepteur, Appointment.Motif.SUIVI, "Suivi spirituel",
                LocalDateTime.now().plusDays(1), 30));

        assertEquals(demandeur, result.demandeurId());
        assertEquals(recepteur, result.recepteurId());
        assertEquals(Appointment.Statut.EN_ATTENTE, result.statut());
        verify(notificationService).create(eq(recepteur), any(), any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void updateStatus_confirm_shouldRequireRecepteur() {
        UUID demandeur = UUID.randomUUID();
        UUID recepteur = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(demandeur);
        when(securityUtils.getCurrentUserRole()).thenReturn("MEMBRE");

        Appointment a = appointment(demandeur, recepteur, Appointment.Statut.EN_ATTENTE);
        when(appointmentRepository.findById(a.getId())).thenReturn(Optional.of(a));

        // Le demandeur (membre) ne peut PAS confirmer
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> appointmentService.updateStatus(a.getId(),
                        new UpdateAppointmentStatusRequest(Appointment.Statut.CONFIRME, null)));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void updateStatus_confirm_asRecepteur_shouldNotifyDemandeur() {
        UUID demandeur = UUID.randomUUID();
        UUID recepteur = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(recepteur);
        when(securityUtils.getCurrentUserRole()).thenReturn("PASTEUR");

        Appointment a = appointment(demandeur, recepteur, Appointment.Statut.EN_ATTENTE);
        when(appointmentRepository.findById(a.getId())).thenReturn(Optional.of(a));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse result = appointmentService.updateStatus(a.getId(),
                new UpdateAppointmentStatusRequest(Appointment.Statut.CONFIRME, "Je vous attends"));

        assertEquals(Appointment.Statut.CONFIRME, result.statut());
        assertEquals("Je vous attends", result.reponse());
        assertNotNull(result.dateTraitement());
        verify(notificationService).create(eq(demandeur), any(), any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void sendReminders_shouldNotifyOnlyConfirmedUpcoming() {
        UUID demandeur = UUID.randomUUID();
        UUID recepteur = UUID.randomUUID();
        Appointment a = appointment(demandeur, recepteur, Appointment.Statut.CONFIRME);
        when(appointmentRepository.findByStatutAndDatePrevueBetweenAndRappelEnvoyeFalse(
                eq(Appointment.Statut.CONFIRME), any(), any())).thenReturn(List.of(a));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        int sent = appointmentService.sendReminders();

        assertEquals(1, sent);
        assertTrue(a.isRappelEnvoye());
        verify(notificationService).create(eq(demandeur), any(), any(), anyString(), anyString(), any(), anyString());
    }
}
