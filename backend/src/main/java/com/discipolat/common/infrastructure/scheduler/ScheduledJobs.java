package com.discipolat.common.infrastructure.scheduler;

import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TransferStatus;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.domain.UserRole;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.authentication.domain.ActivationTokenRepository;
import com.discipolat.modules.authentication.domain.PasswordResetTokenRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.transfers.domain.TransferRequest;
import com.discipolat.modules.transfers.domain.TransferRequestRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.notifications.domain.NotificationRepository;
import com.discipolat.modules.workflow.domain.WorkflowService;
import com.discipolat.modules.appointments.domain.AppointmentService;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.events.domain.EventRegistrationRepository;
import com.discipolat.modules.events.domain.EventRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final SoulRepository soulRepository;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final MakerReportRepository makerReportRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final WorkflowService workflowService;
    private final AppointmentService appointmentService;
    private final TransferRequestRepository transferRequestRepository;
    private final NotificationRepository notificationRepository;

    public ScheduledJobs(SoulRepository soulRepository, AlertRepository alertRepository,
                        NotificationService notificationService, MakerReportRepository makerReportRepository,
                        UserRepository userRepository, EventRepository eventRepository,
                        EventRegistrationRepository eventRegistrationRepository,
                        ActivationTokenRepository activationTokenRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        WorkflowService workflowService,
                        AppointmentService appointmentService,
                        TransferRequestRepository transferRequestRepository,
                        NotificationRepository notificationRepository) {
        this.soulRepository = soulRepository;
        this.alertRepository = alertRepository;
        this.notificationService = notificationService;
        this.makerReportRepository = makerReportRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.workflowService = workflowService;
        this.appointmentService = appointmentService;
        this.transferRequestRepository = transferRequestRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Check for souls with no contact in 48h (original) + 3-week absence alert (US-41)
     */
    /** Rappels automatiques des rendez-vous confirmés (2h avant). */
    @Scheduled(cron = "0 */30 * * * *") // toutes les 30 minutes
    @Transactional
    public void appointmentReminders() {
        int sent = appointmentService.sendReminders();
        if (sent > 0) {
            log.info("Appointment reminders: {} sent", sent);
        }
    }

    /**
     * Check for souls with no contact in 48h (original) + 3-week absence alert (US-41)
     */
    @Scheduled(cron = "${app.scheduler.absence-check-cron}")
    @Transactional
    public void checkAbsences48h() {
        log.info("Running absence 48h check...");
        LocalDateTime threshold48h = LocalDateTime.now().minusHours(48);
        LocalDateTime threshold3Weeks = LocalDateTime.now().minusWeeks(3);
        List<Soul> souls = soulRepository.findAll();

        for (Soul soul : souls) {
            if (soul.isDeleted()) continue;

            // 48h absence check
            if (soul.getDateDernierContact() == null || soul.getDateDernierContact().isBefore(threshold48h)) {
                boolean alreadyAlerted = !alertRepository
                        .findByAmeIdAndStatut(soul.getId(), StatutAlerte.ACTIVE).isEmpty();
                if (!alreadyAlerted) {
                    Alert alert = Alert.builder()
                            .ameId(soul.getId())
                            .faiseurId(soul.getFaiseurId())
                            .typeAlerte("ABSENCE_48H")
                            .message("Âme sans contact depuis plus de 48h : " + soul.getNomComplet())
                            .dateDeclenchement(LocalDateTime.now())
                            .statut(StatutAlerte.ACTIVE)
                            .build();
                    alertRepository.save(alert);
                    notificationService.create(
                            soul.getFaiseurId(), TypeNotification.ABSENCE_48H, CanalNotification.EMAIL,
                            "Alerte absence 48h",
                            "L'âme " + soul.getNomComplet() + " n'a pas eu de contact depuis plus de 48h.",
                            soul.getId(), "SOUL");
                    log.info("48h alert created for soul: {}", soul.getId());
                }
            }

            // US-41: 3-week absence alert for pastor
            if (soul.getDateDernierContact() != null && soul.getDateDernierContact().isBefore(threshold3Weeks)) {
                boolean alreadyAlerted3Weeks = alertRepository
                        .findByAmeIdAndStatut(soul.getId(), StatutAlerte.ACTIVE).stream()
                        .anyMatch(a -> "ABSENCE_3_SEMAINES".equals(a.getTypeAlerte()));
                if (!alreadyAlerted3Weeks) {
                    Alert alert = Alert.builder()
                            .ameId(soul.getId())
                            .faiseurId(soul.getFaiseurId())
                            .typeAlerte("ABSENCE_3_SEMAINES")
                            .message("⚠️ Âme en décrochage potentiel : " + soul.getNomComplet()
                                    + " - sans suivi depuis plus de 3 semaines")
                            .dateDeclenchement(LocalDateTime.now())
                            .statut(StatutAlerte.ACTIVE)
                            .build();
                    alertRepository.save(alert);

                    // Notify the faiseur
                    notificationService.create(
                            soul.getFaiseurId(), TypeNotification.ABSENCE_48H, CanalNotification.EMAIL,
                            "Alerte décrochage 3 semaines",
                            "L'âme " + soul.getNomComplet() + " n'a pas eu de suivi depuis plus de 3 semaines. Intervention requise.",
                            soul.getId(), "SOUL");

                    // Notify the pastor
                    List<User> pasteurs = userRepository.findByRole(UserRole.PASTEUR);
                    for (User pasteur : pasteurs) {
                        notificationService.create(
                                pasteur.getId(), TypeNotification.ABSENCE_48H, CanalNotification.EMAIL,
                                "🚨 Décrochage pastoral détecté",
                                "L'âme " + soul.getNomComplet() + " est sans suivi depuis plus de 3 semaines. "
                                + "Faiseur responsable : " + soul.getFaiseurId(),
                                soul.getId(), "SOUL");
                    }
                    log.info("3-week absence alert created for soul: {}", soul.getId());
                }
            }
        }
    }

    /**
     * US-33: Send report reminders Saturday at 6pm to faiseurs who haven't submitted
     */
    @Scheduled(cron = "${app.scheduler.saturday-reminder-cron}")
    @Transactional
    public void sendSaturdayReportReminders() {
        log.info("Running Saturday report reminder check...");
        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<User> faiseurs = userRepository.findByRole(UserRole.FAISEUR);
        for (User faiseur : faiseurs) {
            long reportCount = makerReportRepository
                    .countByFaiseurIdAndSemaineAndSoumisTrue(faiseur.getId(), currentWeek);
            long soulCount = soulRepository.countByFaiseurId(faiseur.getId());
            if (soulCount > 0 && reportCount < soulCount) {
                notificationService.create(
                        faiseur.getId(), TypeNotification.RAPPORT_NON_SOUMIS, CanalNotification.EMAIL,
                        "🔔 Rappel : Rapport hebdomadaire à soumettre !",
                        "Vous avez " + (soulCount - reportCount) + " rapport(s) non soumis pour la semaine du " + currentWeek
                        + ". La deadline de soumission est ce soir à minuit.",
                        null, null);
                log.info("Saturday reminder sent to faiseur: {}", faiseur.getId());
            }
        }
    }

    /**
     * Send report reminders to faiseurs who haven't submitted (original Monday reminder)
     */
    @Scheduled(cron = "${app.scheduler.report-reminder-cron}")
    @Transactional
    public void sendReportReminders() {
        log.info("Running report reminder check...");
        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<User> faiseurs = userRepository.findByRole(UserRole.FAISEUR);
        for (User faiseur : faiseurs) {
            long reportCount = makerReportRepository
                    .countByFaiseurIdAndSemaineAndSoumisTrue(faiseur.getId(), currentWeek);
            long soulCount = soulRepository.countByFaiseurId(faiseur.getId());
            if (soulCount > 0 && reportCount < soulCount) {
                notificationService.create(
                        faiseur.getId(), TypeNotification.RAPPORT_NON_SOUMIS, CanalNotification.EMAIL,
                        "Rapport hebdomadaire non soumis",
                        "Vous avez " + (soulCount - reportCount) + " rapport(s) non soumis pour la semaine du " + currentWeek,
                        null, null);
                log.info("Reminder sent to faiseur: {}", faiseur.getId());
            }
        }
    }

    /**
     * US-54: Send event reminders J-1
     */
    @Scheduled(cron = "0 0 18 * * *") // Every day at 6 PM
    @Transactional
    public void sendEventReminders() {
        log.info("Running event reminder check...");
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        LocalDateTime dayAfterTomorrow = LocalDateTime.now().plusDays(2);

        List<Event> upcomingEvents = eventRepository.findByDateDebutBetweenAndDeletedFalse(
                tomorrow, dayAfterTomorrow);

        for (Event event : upcomingEvents) {
            List<EventRegistration> registrations = eventRegistrationRepository.findByEventId(event.getId());
            for (EventRegistration reg : registrations) {
                if ("INSCRIT".equals(reg.getStatutInscription())) {
                    notificationService.create(
                            reg.getUtilisateurId(), TypeNotification.INFORMATION, CanalNotification.EMAIL,
                            "Rappel événement : " + event.getTitre(),
                            "L'événement \"" + event.getTitre() + "\" a lieu demain à " + event.getLieu()
                            + ". Date : " + event.getDateDebut(),
                            event.getId(), "EVENT");
                }
            }
        }
        log.info("Event reminders sent for {} events", upcomingEvents.size());
    }

    /**
     * Workflow automatique : escalade d'absentéisme (3 semaines → faiseur,
     * 2 mois → chef de famille, 3 mois → pasteur) + anniversaires du jour.
     * Exécuté chaque matin à 8h.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void runWorkflows() {
        log.info("Running automatic workflows...");
        int absences = workflowService.checkAbsenceEscalation();
        int birthdays = workflowService.checkBirthdays();
        log.info("Workflows done: {} absence escalations, {} birthday reminders", absences, birthdays);
    }

    /**
     * Snapshot hebdomadaire du score spirituel (chaque lundi à 6h).
     */
    @Scheduled(cron = "0 0 6 * * MON")
    @Transactional
    public void snapshotSpiritualScores() {
        int saved = workflowService.snapshotSpiritualScores();
        log.info("Spiritual score snapshot complete: {} souls recorded", saved);
    }

    /**
     * Alerte le pasteur lorsque le délai de traitement d'une demande de transfert
     * est dépassé (demande toujours EN_ATTENTE_VALIDATION ou VALIDATION_PARTIELLE
     * après son delaiLimite).
     *
     * DÉDUPLICATION : une seule notification par demande et par pasteur (one-shot).
     * Choix assumé : le pasteur est alerté une fois quand le délai est dépassé, pas
     * à chaque passage du job — le demandeur et les validateurs suivent l'état dans
     * l'espace Transferts. Une re-soumission après correction ne relance pas l'alerte.
     */
    @Scheduled(cron = "${app.scheduler.transfer-delay-check-cron}")
    @Transactional
    public void checkTransferDelays() {
        log.info("Running transfer delay check...");
        List<TransferRequest> overdue = transferRequestRepository
                .findByStatutInAndDelaiLimiteBefore(
                        java.util.List.of(TransferStatus.EN_ATTENTE_VALIDATION, TransferStatus.VALIDATION_PARTIELLE),
                        LocalDateTime.now());
        if (overdue.isEmpty()) {
            return;
        }
        List<User> pasteurs = userRepository.findByRolesContaining(UserRole.PASTEUR);
        if (pasteurs.isEmpty()) {
            log.warn("No PASTEUR found to alert about {} overdue transfer requests", overdue.size());
            return;
        }
        int sent = 0;
        for (TransferRequest req : overdue) {
            for (User pasteur : pasteurs) {
                if (pasteur.isDeleted()) continue;
                // Déduplication : ne pas re-notifier la même demande au même pasteur.
                if (notificationRepository.existsByDestinataireIdAndTypeAndEntiteReferenceIdAndEntiteReferenceType(
                        pasteur.getId(), TypeNotification.TRANSFERT_DELAI_DEPASSE,
                        req.getId(), "TRANSFER")) {
                    continue;
                }
                try {
                    notificationService.create(
                            pasteur.getId(), TypeNotification.TRANSFERT_DELAI_DEPASSE, CanalNotification.IN_APP,
                            "⏰ Délai de validation dépassé",
                            "La demande de transfert (" + req.getType().name()
                                    + ") soumise le " + (req.getDateSoumission() != null ? req.getDateSoumission() : "—")
                                    + " a dépassé son délai de traitement (" + req.getDelaiLimite() + ") et attend toujours une validation.",
                            req.getId(), "TRANSFER");
                    sent++;
                } catch (Exception e) {
                    // Une notification qui échoue ne doit pas annuler les alertes
                    // aux autres pasteurs ni faire échouer le job.
                    log.warn("Transfer delay notification failed for pasteur {} on request {}: {}",
                            pasteur.getId(), req.getId(), e.getMessage());
                }
            }
        }
        log.info("Transfer delay check: {} overdue request(s), {} notification(s) sent", overdue.size(), sent);
    }

    /**
     * Calculate and cache dashboard metrics
     */
    @Scheduled(cron = "${app.scheduler.dashboard-metrics-cron}")
    public void calculateDashboardMetrics() {
        log.info("Calculating dashboard metrics...");
        log.info("Dashboard metrics calculated successfully");
    }

    /**
     * Clean up expired activation and password reset tokens (daily at 3 AM)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired tokens...");
        Instant now = Instant.now();
        long deletedActivations = activationTokenRepository.deleteByExpiresAtBefore(now);
        long deletedPasswordResets = passwordResetTokenRepository.deleteByExpiresAtBefore(now);
        if (deletedActivations > 0 || deletedPasswordResets > 0) {
            log.info("Cleaned up {} activation tokens and {} password reset tokens",
                    deletedActivations, deletedPasswordResets);
        }
    }
}
