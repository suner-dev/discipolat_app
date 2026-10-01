package com.discipolat.modules.families.service;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.families.domain.FamilyVisit;
import com.discipolat.modules.families.repository.FamilyVisitRepository;
import com.discipolat.modules.notifications.domain.NotificationRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * G4.1 — « rappel quotidien des suivis » : chaque matin, les visites dont
 * next_action_date est due ou dépassée (et non clôturées) déclenchent une
 * notification IN_APP vers le faiseur responsable (et le chef de famille),
 * dédupliquée par destinataire + visite + jour.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FamilyFollowUpReminderScheduler {

    private static final List<String> CLOSED_STATUSES = List.of("COMPLETED", "CANCELLED");

    private final FamilyVisitRepository familyVisitRepository;
    private final FamilyRepository familyRepository;
    private final SoulRepository soulRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    /** Tous les jours à 7h00 (après l'escalade d'absentéisme, avant 8h). */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void sendDailyFollowUpReminders() {
        LocalDate today = LocalDate.now();
        List<FamilyVisit> due = familyVisitRepository
                .findByDeletedFalseAndNextActionDateLessThanEqualAndStatusNotIn(today, CLOSED_STATUSES);
        int sent = 0;
        for (FamilyVisit visit : due) {
            if (visit.getNextActionDate() == null) continue;
            long daysLate = today.toEpochDay() - visit.getNextActionDate().toEpochDay();
            String soulName = visit.getSoulId() != null
                    ? soulRepository.findById(visit.getSoulId()).map(Soul::getNomComplet).orElse("un membre")
                    : "un membre";
            String message = "Suivi « " + (visit.getSubject() != null ? visit.getSubject() : "visite")
                    + " » pour " + soulName
                    + (daysLate > 0 ? " en retard de " + daysLate + " jour(s)" : " dû aujourd'hui")
                    + ". Action prévue le " + visit.getNextActionDate() + ".";

            sent += notifyOnce(visit.getTenantId(), visit.getFaiseurId(), visit, message);

            // Copie au chef de famille (hors faiseur lui-même)
            Optional<Family> family = visit.getFamilyId() != null
                    ? familyRepository.findById(visit.getFamilyId())
                    : Optional.empty();
            if (family.isPresent() && family.get().getChefFamilleId() != null
                    && !family.get().getChefFamilleId().equals(visit.getFaiseurId())) {
                sent += notifyOnce(visit.getTenantId(), family.get().getChefFamilleId(), visit, message);
            }
        }
        if (sent > 0) {
            log.info("[G4.1] {} rappel(s) de suivi envoyés ({} visites dues)", sent, due.size());
        }
    }

    private int notifyOnce(UUID tenantId, UUID destinataireId, FamilyVisit visit, String message) {
        if (destinataireId == null || tenantId == null) return 0;
        try {
            // Déduplication : max 1 rappel SUIVI_RAPPEL par visite et par destinataire / jour.
            if (notificationRepository.existsByDestinataireIdAndTypeAndEntiteReferenceIdAndCreatedAtAfter(
                    destinataireId, TypeNotification.SUIVI_RAPPEL, visit.getId(),
                    LocalDate.now().atStartOfDay())) {
                return 0;
            }
            notificationService.create(tenantId, destinataireId, TypeNotification.SUIVI_RAPPEL,
                    CanalNotification.IN_APP, "📋 Rappel de suivi", message, visit.getId(), "FAMILY_VISIT");
            return 1;
        } catch (Exception e) {
            log.debug("Rappel suivi échoué pour {} (visite {}) : {}", destinataireId, visit.getId(), e.getMessage());
            return 0;
        }
    }
}
