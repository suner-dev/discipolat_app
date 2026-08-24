package com.discipolat.modules.notifications.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRegistration;
import com.discipolat.modules.events.domain.EventRegistrationRepository;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * P4 — Rappels contextuels intelligents.
 *
 * Envoie des notifications push ciblées basées sur le contexte :
 *   1. 24h avant un événement auquel le membre a confirmé sa venue
 *   2. Absence répétée (3+ semaines sans contact) — alerte au faiseur
 *   3. Anniversaire de conversion
 *
 * Tourne quotidiennement via @Scheduled.
 */
@Service
public class ContextualReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ContextualReminderScheduler.class);

    private final NotificationService notificationService;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final SoulRepository soulRepository;

    public ContextualReminderScheduler(NotificationService notificationService,
                                       EventRepository eventRepository,
                                       EventRegistrationRepository eventRegistrationRepository,
                                       SoulRepository soulRepository) {
        this.notificationService = notificationService;
        this.eventRepository = eventRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.soulRepository = soulRepository;
    }

    /**
     * Runs daily at 8:00 AM — sends contextual reminders for the day.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyReminders() {
        log.info("[ContextualReminder] Starting daily reminder cycle");
        int total = 0;
        total += remindEventTomorrow();
        total += remindAbsentSouls();
        total += remindConversionAnniversaries();
        log.info("[ContextualReminder] Sent {} contextual reminders", total);
    }

    /**
     * Remind members 24h before an event they RSVP'd to.
     */
    private int remindEventTomorrow() {
        int sent = 0;
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Event> upcomingEvents = eventRepository
                .findByDateDebutBetweenAndDeletedFalse(
                        tomorrow.atStartOfDay(), tomorrow.atTime(23, 59));

        for (Event event : upcomingEvents) {
            List<EventRegistration> registrations = eventRegistrationRepository
                    .findByEventId(event.getId());
            for (EventRegistration reg : registrations) {
                try {
                    String timeStr = event.getDateDebut() != null
                            ? event.getDateDebut().toLocalTime().toString().substring(0, 5)
                            : "l'heure prévue";
                    notificationService.create(reg.getUtilisateurId(),
                            TypeNotification.RAPPEL,
                            CanalNotification.PUSH,
                            "📅 Rappel : " + event.getTitre(),
                            "N'oubliez pas : \"" + event.getTitre() + "\" demain à " +
                                    timeStr + ". Préparez-vous !",
                            event.getId(), "EVENT");
                    sent++;
                } catch (Exception e) {
                    log.debug("Event reminder failed for {}: {}", reg.getUtilisateurId(), e.getMessage());
                }
            }
        }
        return sent;
    }

    /**
     * Alert for souls absent for 3+ weeks without contact.
     */
    private int remindAbsentSouls() {
        int sent = 0;
        List<Soul> atRisk = soulRepository.findByDeletedFalse().stream()
                .filter(s -> s.getStatut() == StatutAme.ACTIF)
                .filter(s -> s.getFaiseurId() != null)
                .filter(s -> {
                    if (s.getDateDernierContact() == null) return true;
                    return ChronoUnit.DAYS.between(s.getDateDernierContact(), LocalDateTime.now()) >= 21;
                })
                .toList();

        for (Soul soul : atRisk) {
            try {
                long daysSince = soul.getDateDernierContact() != null
                        ? ChronoUnit.DAYS.between(s.getDateDernierContact(), LocalDateTime.now())
                        : 30;
                notificationService.create(soul.getFaiseurId(),
                        TypeNotification.ALARME,
                        CanalNotification.PUSH,
                        "⚠️ Absence détectée",
                        soul.getNomComplet() + " n'a plus eu de contact depuis " +
                                daysSince + " jours. Un suivi est recommandé.",
                        soul.getId(), "SOUL");
                sent++;
            } catch (Exception e) {
                log.debug("Absence alert failed for faiseur {}: {}", soul.getFaiseurId(), e.getMessage());
            }
        }
        return sent;
    }

    /**
     * Celebrate conversion anniversaries.
     */
    private int remindConversionAnniversaries() {
        int sent = 0;
        LocalDate today = LocalDate.now();
        List<Soul> souls = soulRepository.findByDeletedFalse();

        for (Soul soul : souls) {
            if (soul.getUserId() == null) continue;
            try {
                if (soul.getDateConversion() != null) {
                    LocalDate conversion = soul.getDateConversion();
                    if (conversion.getMonth() == today.getMonth()
                            && conversion.getDayOfMonth() == today.getDayOfMonth()
                            && conversion.getYear() != today.getYear()) {
                        int years = today.getYear() - conversion.getYear();
                        notificationService.create(soul.getUserId(),
                                TypeNotification.ENCOURAGEMENT,
                                CanalNotification.PUSH,
                                "🎉 Anniversaire de conversion !",
                                "Joyeux anniversaire de conversion ! Cela fait " + years +
                                        " an" + (years > 1 ? "s" : "") +
                                        " que vous avez fait le plus beau choix de votre vie.",
                                soul.getId(), "SOUL");
                        sent++;
                    }
                }
            } catch (Exception e) {
                log.debug("Anniversary reminder failed for {}: {}", soul.getUserId(), e.getMessage());
            }
        }
        return sent;
    }
}
