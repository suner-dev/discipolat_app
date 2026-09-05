package com.discipolat.modules.alerts.domain;

import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Smart Alerts — anomaly detection service.
 * Analyzes data patterns and automatically generates alerts when anomalies are detected.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmartAlertService {

    private final AlertRepository alertRepository;
    private final SoulRepository soulRepository;
    private final DepartmentRepository departmentRepository;

    private static final int NO_CONTACT_DAYS = 30;
    private static final int OVERDUE_REPORT_DAYS = 7;
    private static final int DEDUP_WINDOW_DAYS = 7;

    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    public void runAllChecks() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) return;

        log.info("[SmartAlert] Running anomaly detection for tenant {}", tenantId);

        int total = 0;
        total += detectSustainedAbsences(tenantId);
        total += detectNoRecentContact(tenantId);
        total += detectUnresolvedDiscipline(tenantId);
        total += detectInactiveDepartments(tenantId);

        log.info("[SmartAlert] Generated {} new alerts for tenant {}", total, tenantId);
    }

    /**
     * Detect souls with sustained absences (no contact in 3+ weeks).
     */
    public int detectSustainedAbsences(UUID tenantId) {
        int created = 0;
        LocalDateTime threshold3Weeks = LocalDateTime.now().minusWeeks(3);
        List<Soul> souls = soulRepository.findByDeletedFalse();

        for (Soul soul : souls) {
            if (soul.getStatut() == StatutAme.DECROCHE) continue;

            LocalDateTime lastContact = soul.getDateDernierContact();
            if (lastContact == null) lastContact = soul.getCreatedAt();

            if (lastContact != null && lastContact.isBefore(threshold3Weeks)) {
                Optional<Alert> alert = createIfNotDuplicate(
                        tenantId, "SUSTAINED_ABSENCE", "HAUTE",
                        "Absence prolongée : " + soul.getNom() + " " + soul.getPrenom(),
                        soul.getNom() + " " + soul.getPrenom() + " n'a plus été contacté depuis "
                                + ChronoUnit.DAYS.between(lastContact, LocalDateTime.now()) + " jours.",
                        soul.getId());
                if (alert.isPresent()) created++;
            }
        }
        return created;
    }

    /**
     * Detect souls that haven't been contacted in 30+ days.
     */
    public int detectNoRecentContact(UUID tenantId) {
        int created = 0;
        LocalDateTime threshold = LocalDateTime.now().minusDays(NO_CONTACT_DAYS);
        List<Soul> souls = soulRepository.findByDeletedFalse();

        for (Soul soul : souls) {
            if (soul.getStatut() == StatutAme.DECROCHE) continue;

            LocalDateTime lastContact = soul.getDateDernierContact();
            if (lastContact == null) lastContact = soul.getCreatedAt();

            if (lastContact != null && lastContact.isBefore(threshold)) {
                Optional<Alert> alert = createIfNotDuplicate(
                        tenantId, "ABSENCE_CONTACT", "MOYENNE",
                        "Pas de contact : " + soul.getNom() + " " + soul.getPrenom(),
                        "Aucun contact avec " + soul.getNom() + " " + soul.getPrenom()
                                + " depuis " + ChronoUnit.DAYS.between(lastContact, LocalDateTime.now()) + " jours.",
                        soul.getId());
                if (alert.isPresent()) created++;
            }
        }
        return created;
    }

    /**
     * Detect discipline events older than 14 days that are still unresolved.
     */
    public int detectUnresolvedDiscipline(UUID tenantId) {
        int created = 0;
        List<Soul> souls = soulRepository.findByDeletedFalse();

        for (Soul soul : souls) {
            if (soul.getStatut() == StatutAme.DECROCHE && soul.getDateDernierContact() != null) {
                long daysSinceDropout = ChronoUnit.DAYS.between(soul.getDateDernierContact(), LocalDateTime.now());
                if (daysSinceDropout > 14) {
                    Optional<Alert> alert = createIfNotDuplicate(
                            tenantId, "UNRESOLVED_DISCIPLINE", "HAUTE",
                            "Décrochage non résolu : " + soul.getNom() + " " + soul.getPrenom(),
                            soul.getNom() + " " + soul.getPrenom() + " est en décrochage depuis "
                                    + daysSinceDropout + " jours sans résolution.",
                            soul.getId());
                    if (alert.isPresent()) created++;
                }
            }
        }
        return created;
    }

    /**
     * Detect departments with no activity in the last 30 days.
     */
    public int detectInactiveDepartments(UUID tenantId) {
        int created = 0;
        List<Department> departments = departmentRepository.findByDeletedFalseOrderByNomAsc();
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        for (Department dept : departments) {
            if (dept.getUpdatedAt() != null && dept.getUpdatedAt().isBefore(threshold)) {
                Optional<Alert> alert = createIfNotDuplicate(
                        tenantId, "INACTIVE_DEPARTMENT", "MOYENNE",
                        "Département inactif : " + dept.getNom(),
                        "Le département " + dept.getNom() + " n'a eu aucune activité depuis "
                                + ChronoUnit.DAYS.between(dept.getUpdatedAt(), LocalDateTime.now()) + " jours.",
                        dept.getId());
                if (alert.isPresent()) created++;
            }
        }
        return created;
    }

    @Transactional
    public List<Map<String, Object>> predictDropoutRisk(UUID tenantId) {
        List<Map<String, Object>> atRisk = new ArrayList<>();
        List<Soul> souls = soulRepository.findByDeletedFalse();
        LocalDateTime now = LocalDateTime.now();

        for (Soul soul : souls) {
            if (soul.getStatut() == StatutAme.DECROCHE) continue;

            double riskScore = 0;
            List<String> factors = new ArrayList<>();

            LocalDateTime lastContact = soul.getDateDernierContact();
            if (lastContact == null) lastContact = soul.getCreatedAt();
            long daysSinceContact = lastContact != null ? ChronoUnit.DAYS.between(lastContact, now) : 60;

            if (daysSinceContact >= 28) { riskScore += 0.4; factors.add("NO_CONTACT"); }
            else if (daysSinceContact >= 21) { riskScore += 0.3; factors.add("NO_CONTACT"); }
            else if (daysSinceContact >= 14) { riskScore += 0.15; }

            switch (soul.getStatut()) {
                case EN_VEILLE -> { riskScore += 0.3; factors.add("ATTENDANCE_DECLINE"); }
                case NOUVEAU_CONVERTI -> riskScore += 0.15;
                case NOUVEL_ARRIVANT -> riskScore += 0.1;
                default -> {}
            }

            String etat = soul.getEtatSpirituel();
            if (etat != null && (etat.toUpperCase().contains("DIFFICULTE") || etat.toUpperCase().contains("CRISE"))) {
                riskScore += 0.2; factors.add("SPIRITUAL_DECLINE");
            }

            if (riskScore >= 0.4) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("soulId", soul.getId());
                entry.put("soulName", soul.getNom() + " " + soul.getPrenom());
                entry.put("riskScore", Math.min(1.0, riskScore));
                entry.put("riskLevel", riskScore >= 0.7 ? "HIGH" : "MEDIUM");
                entry.put("riskFactors", String.join(",", factors));
                entry.put("faiseurId", soul.getFaiseurId());
                atRisk.add(entry);
            }
        }

        atRisk.sort((a, b) -> Double.compare((double) b.get("riskScore"), (double) a.get("riskScore")));
        return atRisk;
    }

    @Transactional
    public List<Map<String, Object>> generateInterventionPlans(UUID tenantId) {
        List<Map<String, Object>> plans = new ArrayList<>();
        List<Map<String, Object>> atRiskSoul = predictDropoutRisk(tenantId);

        for (Map<String, Object> soul : atRiskSoul) {
            Map<String, Object> plan = new LinkedHashMap<>();
            plan.put("soulId", soul.get("soulId"));
            plan.put("soulName", soul.get("soulName"));
            plan.put("riskLevel", soul.get("riskLevel"));
            plan.put("riskScore", soul.get("riskScore"));

            List<String> steps = new ArrayList<>();
            String riskFactors = (String) soul.getOrDefault("riskFactors", "");
            if (riskFactors.contains("ATTENDANCE_DECLINE")) {
                steps.add("Appeler le membre pour comprendre la raison de l'absence");
                steps.add("Proposer un RDV pastoral cette semaine");
            }
            if (riskFactors.contains("NO_CONTACT")) {
                steps.add("Envoyer un message d'encouragement personnalisé");
                steps.add("Planifier une visite à domicile");
            }
            if (riskFactors.contains("SPIRITUAL_DECLINE")) {
                steps.add("Proposer un accompagnement spirituel individualisé");
                steps.add("Inviter à une rencontre de famille");
            }
            if (steps.isEmpty()) steps.add("Maintenir le suivi habituel");

            plan.put("interventionSteps", steps);
            plan.put("assignedTo", soul.get("faiseurId"));
            plan.put("createdAt", LocalDateTime.now());
            plan.put("status", "PENDING");
            plans.add(plan);
        }
        return plans;
    }

    @Transactional
    public Map<String, Object> runChecksNow() {
        UUID tenantId = TenantContext.getTenantId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sustainedAbsences", detectSustainedAbsences(tenantId));
        result.put("noRecentContact", detectNoRecentContact(tenantId));
        result.put("unresolvedDiscipline", detectUnresolvedDiscipline(tenantId));
        result.put("inactiveDepartments", detectInactiveDepartments(tenantId));
        result.put("dropoutPrediction", predictDropoutRisk(tenantId).size());
        result.put("interventionPlans", generateInterventionPlans(tenantId).size());
        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnomalySummary() {
        UUID tenantId = TenantContext.getTenantId();
        Map<String, Object> summary = new LinkedHashMap<>();
        long totalActive = alertRepository.countByStatut(StatutAlerte.ACTIVE);
        long criticalCount = alertRepository.countByStatutAndPriorite(StatutAlerte.ACTIVE, "HAUTE");
        summary.put("totalActive", totalActive);
        summary.put("criticalActive", criticalCount);
        summary.put("lastScan", LocalDateTime.now());
        return summary;
    }

    private Optional<Alert> createIfNotDuplicate(
            UUID tenantId, String type, String priority, String title,
            String message, UUID relatedEntityId) {

        List<Alert> existing = alertRepository.findByTypeAlerteAndTenantId(type, tenantId);
        LocalDateTime dedupWindow = LocalDateTime.now().minusDays(DEDUP_WINDOW_DAYS);

        boolean duplicate = existing.stream()
                .filter(a -> a.getStatut() == StatutAlerte.ACTIVE)
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(dedupWindow))
                .anyMatch(a -> relatedEntityId == null || relatedEntityId.equals(a.getAmeId()));

        if (duplicate) return Optional.empty();

        Alert alert = Alert.builder()
                .tenantId(tenantId)
                .typeAlerte(type)
                .titre(title)
                .message(message)
                .priorite(priority)
                .cible("PERSONNE")
                .dateDeclenchement(LocalDateTime.now())
                .statut(StatutAlerte.ACTIVE)
                .build();

        return Optional.of(alertRepository.save(alert));
    }
}
