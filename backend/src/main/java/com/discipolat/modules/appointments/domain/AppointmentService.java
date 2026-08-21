package com.discipolat.modules.appointments.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.appointments.api.AppointmentResponse;
import com.discipolat.modules.appointments.api.CreateAppointmentRequest;
import com.discipolat.modules.appointments.api.UpdateAppointmentStatusRequest;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Système de rendez-vous : un membre (ou tout utilisateur) demande un
 * rendez-vous à un récepteur (pasteur, chef de famille, faiseur, responsable).
 * Le récepteur confirme ou refuse. Des rappels automatiques sont envoyés
 * avant chaque rendez-vous confirmé.
 */
@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              UserRepository userRepository,
                              NotificationService notificationService,
                              EntityPropagationPublisher propagationPublisher,
                              SecurityUtils securityUtils) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    /** Prise de rendez-vous par l'utilisateur connecté. */
    public AppointmentResponse create(CreateAppointmentRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        if (!userRepository.existsById(request.recepteurId())) {
            throw new EntityNotFoundException("User", request.recepteurId());
        }
        Appointment appointment = Appointment.builder()
                .demandeurId(userId)
                .recepteurId(request.recepteurId())
                .motif(request.motif())
                .objet(request.objet())
                .datePrevue(request.datePrevue())
                .dureeMinutes(request.dureeMinutes() != null ? request.dureeMinutes() : 30)
                .statut(Appointment.Statut.EN_ATTENTE)
                .build();
        Appointment saved = appointmentRepository.save(appointment);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishCreated("APPOINTMENT", saved.getId(),
                Map.of("motif", saved.getMotif() != null ? saved.getMotif() : "",
                        "demandeurId", saved.getDemandeurId(),
                        "recepteurId", saved.getRecepteurId()),
                "Rendez-vous créé: " + (saved.getMotif() != null ? saved.getMotif() : ""));
        // Notifie le récepteur de la demande
        notificationService.create(
                saved.getRecepteurId(), TypeNotification.INFORMATION, CanalNotification.IN_APP,
                "📅 Demande de rendez-vous",
                "Vous avez reçu une demande de rendez-vous. Consultez votre espace rendez-vous.",
                saved.getId(), "APPOINTMENT");

        return toResponse(saved);
    }

    /** Rendez-vous demandés par l'utilisateur connecté. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> myRequests() {
        return appointmentRepository.findByDemandeurIdOrderByDatePrevueDesc(securityUtils.getCurrentUserId())
                .stream().map(this::toResponse).toList();
    }

    /** Rendez-vous reçus par l'utilisateur connecté (boîte de réception). */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> myInbox() {
        return appointmentRepository.findByRecepteurIdOrderByDatePrevueDesc(securityUtils.getCurrentUserId())
                .stream().map(this::toResponse).toList();
    }

    /** Validation (confirm/refuse/annule) par le récepteur ou le demandeur. */
    public AppointmentResponse updateStatus(UUID appointmentId, UpdateAppointmentStatusRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment", appointmentId));

        boolean isRecepteur = appointment.getRecepteurId().equals(userId);
        boolean isDemandeur = appointment.getDemandeurId().equals(userId);
        String role = securityUtils.getCurrentUserRole();

        if (request.statut() == Appointment.Statut.CONFIRME || request.statut() == Appointment.Statut.REFUSE) {
            // Seul le récepteur (ou admin/pasteur) peut confirmer/refuser
            if (!isRecepteur && !"ADMIN".equals(role) && !"PASTEUR".equals(role)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Seul le récepteur peut confirmer ou refuser ce rendez-vous");
            }
        } else if (request.statut() == Appointment.Statut.ANNULE || request.statut() == Appointment.Statut.TERMINE) {
            // Le demandeur peut annuler, le récepteur (ou admin/pasteur) peut annuler ou clôturer
            if (!isDemandeur && !isRecepteur && !"ADMIN".equals(role) && !"PASTEUR".equals(role)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Vous ne pouvez pas modifier ce rendez-vous");
            }
        } else {
            throw new com.discipolat.common.exception.BadRequestException(
                    "Statut invalide : utilisez CONFIRME, REFUSE, ANNULE ou TERMINE");
        }

        String oldStatut = appointment.getStatut().name();
        appointment.setStatut(request.statut());
        if (request.reponse() != null && !request.reponse().isBlank()) {
            appointment.setReponse(request.reponse());
        }
        appointment.setTraitePar(userId);
        appointment.setDateTraitement(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishStatusChanged("APPOINTMENT", saved.getId(),
                oldStatut, request.statut().name(),
                "Rendez-vous: " + oldStatut + " -> " + request.statut().name());

        // Notifie le demandeur de la décision
        if (request.statut() == Appointment.Statut.CONFIRME) {
            notificationService.create(
                    saved.getDemandeurId(), TypeNotification.INFORMATION, CanalNotification.IN_APP,
                    "✅ Rendez-vous confirmé",
                    "Votre rendez-vous du " + saved.getDatePrevue() + " a été confirmé.",
                    saved.getId(), "APPOINTMENT");
        } else if (request.statut() == Appointment.Statut.REFUSE) {
            notificationService.create(
                    saved.getDemandeurId(), TypeNotification.INFORMATION, CanalNotification.IN_APP,
                    "❌ Rendez-vous refusé",
                    "Votre demande de rendez-vous a été refusée." + (saved.getReponse() != null ? " Motif : " + saved.getReponse() : ""),
                    saved.getId(), "APPOINTMENT");
        }
        return toResponse(saved);
    }

    /**
     * Rappels automatiques : notifie le demandeur 2h avant un RDV confirmé.
     * Appelé par le scheduler.
     */
    public int sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime to = now.plusHours(2);
        List<Appointment> upcoming = appointmentRepository
                .findByStatutAndDatePrevueBetweenAndRappelEnvoyeFalse(Appointment.Statut.CONFIRME, now, to);
        int sent = 0;
        for (Appointment a : upcoming) {
            notificationService.create(
                    a.getDemandeurId(), TypeNotification.INFORMATION, CanalNotification.IN_APP,
                    "⏰ Rappel : rendez-vous dans 2h",
                    "Votre rendez-vous " + a.getMotif() + " est prévu à " + a.getDatePrevue() + ".",
                    a.getId(), "APPOINTMENT");
            a.setRappelEnvoye(true);
            appointmentRepository.save(a);
            sent++;
        }
        return sent;
    }

    private AppointmentResponse toResponse(Appointment a) {
        String demandeurNom = userRepository.findById(a.getDemandeurId())
                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null);
        String recepteurNom = userRepository.findById(a.getRecepteurId())
                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null);
        return AppointmentResponse.from(a, demandeurNom, recepteurNom);
    }
}
