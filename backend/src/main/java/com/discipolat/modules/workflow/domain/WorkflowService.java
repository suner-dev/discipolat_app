package com.discipolat.modules.workflow.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.SpiritualScoreService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

/**
 * Workflows automatiques :
 * 1. Escalade d'absentéisme : 3 semaines → faiseur, 2 mois → chef de famille, 3 mois → pasteur.
 * 2. Rappels d'anniversaires le jour J.
 * 3. Snapshot hebdomadaire du score spirituel de toutes les âmes.
 */
@Service
@Transactional
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final SoulRepository soulRepository;
    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SpiritualScoreService spiritualScoreService;

    public WorkflowService(SoulRepository soulRepository, FamilyRepository familyRepository,
                           UserRepository userRepository, NotificationService notificationService,
                           SpiritualScoreService spiritualScoreService) {
        this.soulRepository = soulRepository;
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.spiritualScoreService = spiritualScoreService;
    }

    /**
     * Escalade d'absentéisme basée sur la date du dernier contact :
     * 3+ semaines → faiseur ; 2+ mois → chef de famille ; 3+ mois → pasteur.
     */
    public int checkAbsenceEscalation() {
        int notifications = 0;
        LocalDate today = LocalDate.now();
        for (Soul soul : soulRepository.findAll()) {
            if (soul.isDeleted() || soul.getStatut() == StatutAme.DECROCHE) continue;
            if (soul.getDateDernierContact() == null) continue;

            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    soul.getDateDernierContact().toLocalDate(), today);
            String nom = soul.getNomComplet();

            // 3+ mois → pasteur
            if (days >= 90) {
                for (User pasteur : userRepository.findByRole(UserRole.PASTEUR)) {
                    notificationService.create(
                            pasteur.getId(), TypeNotification.ALERTE_ABSENCE, CanalNotification.IN_APP,
                            "🚨 Absence de 3 mois : " + nom,
                            nom + " est sans contact depuis plus de 3 mois. Action pastorale requise.",
                            soul.getId(), "SOUL");
                    notifications++;
                }
            }
            // 2+ mois → chef de famille
            else if (days >= 60) {
                UUID chefId = findChefFamille(soul);
                if (chefId != null) {
                    notificationService.create(
                            chefId, TypeNotification.ALERTE_ABSENCE, CanalNotification.IN_APP,
                            "⚠️ Absence de 2 mois : " + nom,
                            nom + " est sans contact depuis plus de 2 mois. Suivi de la famille requis.",
                            soul.getId(), "SOUL");
                    notifications++;
                }
            }
            // 3+ semaines → faiseur
            else if (days >= 21) {
                notificationService.create(
                        soul.getFaiseurId(), TypeNotification.ALERTE_ABSENCE, CanalNotification.IN_APP,
                        "⏰ Absence de 3 semaines : " + nom,
                        nom + " est sans contact depuis plus de 3 semaines. Reprendre contact.",
                        soul.getId(), "SOUL");
                notifications++;
            }
        }
        if (notifications > 0) {
            log.info("Absence escalation: {} notifications sent", notifications);
        }
        return notifications;
    }

    /** Rappels d'anniversaire le jour même pour les âmes. */
    public int checkBirthdays() {
        int notifications = 0;
        MonthDay today = MonthDay.now();
        for (Soul soul : soulRepository.findAll()) {
            if (soul.isDeleted() || soul.getDateNaissance() == null) continue;
            if (MonthDay.from(soul.getDateNaissance()).equals(today)) {
                UUID target = soul.getFaiseurId();
                notificationService.create(
                        target, TypeNotification.INFORMATION, CanalNotification.IN_APP,
                        "🎂 Anniversaire : " + soul.getNomComplet(),
                        soul.getNomComplet() + " fête son anniversaire aujourd'hui !",
                        soul.getId(), "SOUL");
                notifications++;
            }
        }
        if (notifications > 0) {
            log.info("Birthday reminders: {} sent", notifications);
        }
        return notifications;
    }

    /** Snapshot hebdomadaire du score spirituel (à appeler chaque lundi). */
    public int snapshotSpiritualScores() {
        int saved = spiritualScoreService.snapshotAll();
        log.info("Spiritual score snapshot: {} souls recorded", saved);
        return saved;
    }

    /** Lundi de la semaine courante. */
    public LocalDate currentWeekMonday() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private UUID findChefFamille(Soul soul) {
        if (soul.getFamilleId() == null) return null;
        Family family = familyRepository.findById(soul.getFamilleId()).orElse(null);
        if (family == null) return null;
        return family.getChefFamilleId();
    }
}
