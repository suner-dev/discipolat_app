package com.discipolat.modules.dashboard.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.StatutSuiviParallele;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowupRepository;
import com.discipolat.modules.reports.domain.FamilyReport;
import com.discipolat.modules.reports.domain.FamilyReportRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class DashboardService {

    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final MakerReportRepository makerReportRepository;
    private final FamilyReportRepository familyReportRepository;
    private final AlertRepository alertRepository;
    private final ParallelFollowupRepository parallelFollowupRepository;
    private final DepartmentRepository departmentRepository;

    public DashboardService(SoulRepository soulRepository, UserRepository userRepository,
                           FamilyRepository familyRepository, MakerReportRepository makerReportRepository,
                           FamilyReportRepository familyReportRepository, AlertRepository alertRepository,
                           ParallelFollowupRepository parallelFollowupRepository,
                           DepartmentRepository departmentRepository) {
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.makerReportRepository = makerReportRepository;
        this.familyReportRepository = familyReportRepository;
        this.alertRepository = alertRepository;
        this.parallelFollowupRepository = parallelFollowupRepository;
        this.departmentRepository = departmentRepository;
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        long totalSouls = soulRepository.count();
        long nouveauxArrivants = soulRepository.countByTypeDisciple(TypeDisciple.NOUVEL_ARRIVANT);
        long nouveauxConvertis = soulRepository.countByTypeDisciple(TypeDisciple.NOUVEAU_CONVERTI);
        long soulsActives = soulRepository.countByStatut(StatutAme.ACTIF);
        long soulsEnIntegration = soulRepository.countByStatut(StatutAme.EN_INTEGRATION);
        long totalFaiseurs = userRepository.countByRole(UserRole.FAISEUR);
        long totalChefsDeFamille = userRepository.findByEstChefDeFamilleTrue().size();
        long activeAlerts = alertRepository.countByStatut(StatutAlerte.ACTIVE);
        long suivisParalleles = parallelFollowupRepository.countByStatut(StatutSuiviParallele.EN_COURS);
        long totalFamilles = familyRepository.count();

        summary.put("totalSouls", totalSouls);
        summary.put("nouveauxArrivants", nouveauxArrivants);
        summary.put("nouveauxConvertis", nouveauxConvertis);
        summary.put("soulsActives", soulsActives);
        summary.put("soulsEnIntegration", soulsEnIntegration);
        summary.put("totalFaiseurs", totalFaiseurs);
        summary.put("totalChefsDeFamille", totalChefsDeFamille);
        summary.put("totalFamilles", totalFamilles);
        summary.put("activeAlerts", activeAlerts);
        summary.put("suivisParallelesActifs", suivisParalleles);
        return summary;
    }

    public Map<String, Object> getKPI(LocalDate periodeDebut, LocalDate periodeFin,
                                      UUID departementId, UUID familleId) {
        if (periodeDebut == null) {
            periodeDebut = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(4);
        }
        if (periodeFin == null) {
            periodeFin = LocalDate.now();
        }

        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        long totalSouls = soulRepository.count();
        long nouveauxArrivants = soulRepository.countByTypeDisciple(TypeDisciple.NOUVEL_ARRIVANT);
        long nouveauxConvertis = soulRepository.countByTypeDisciple(TypeDisciple.NOUVEAU_CONVERTI);
        long activeAlerts = alertRepository.countByStatut(StatutAlerte.ACTIVE);
        long suivisParalleles = parallelFollowupRepository.countByStatut(StatutSuiviParallele.EN_COURS);
        long totalFamilles = familyRepository.count();
        long totalDepartements = departmentRepository.count();
        long totalFaiseurs = userRepository.countByRole(UserRole.FAISEUR);

        // Calculate presence rate from current week maker reports
        Page<MakerReport> currentWeekReportPage = makerReportRepository.findBySemaine(currentWeek,
                org.springframework.data.domain.PageRequest.of(0, 1000));
        List<MakerReport> currentWeekReports = currentWeekReportPage.getContent();
        int totalPresents = 0;
        int totalPresencesPossible = 0;
        int totalSorties = 0;
        int totalMaintenus = 0;
        int rapportsSoumis = 0;
        int rapportsEnAttente = 0;

        for (MakerReport report : currentWeekReports) {
            if (report.isSoumis()) {
                rapportsSoumis++;
            } else {
                rapportsEnAttente++;
            }
            if (report.getPresencesParCulte() != null) {
                for (Boolean present : report.getPresencesParCulte().values()) {
                    totalPresencesPossible++;
                    if (present) totalPresents++;
                }
            }
            if (report.getNbSorties() != null) totalSorties += report.getNbSorties();
            if (report.getNbMaintenus() != null) totalMaintenus += report.getNbMaintenus();
        }

        double tauxPresenceGlobal = totalPresencesPossible > 0
                ? (double) totalPresents / totalPresencesPossible * 100.0 : 0.0;

        // Calculate family risk (families with presence below 50%)
        long famillesARisque = 0;
        List<Family> allFamilies = familyRepository.findAll();
        for (Family family : allFamilies) {
            List<Soul> familySouls = soulRepository.findAllByFamilleId(family.getId());
            if (!familySouls.isEmpty()) {
                long actifsInFamily = familySouls.stream()
                        .filter(s -> s.getStatut() == StatutAme.ACTIF || s.getStatut() == StatutAme.EN_INTEGRATION)
                        .count();
                if (actifsInFamily > 0) {
                    // Check if this family has reports with low presence
                    List<FamilyReport> familyReports = familyReportRepository.findByFamilleIdAndSemaine(
                            family.getId(), currentWeek);
                    if (!familyReports.isEmpty()) {
                        FamilyReport fr = familyReports.get(0);
                        if (fr.getPresenceMoyenne() != null && fr.getPresenceMoyenne().doubleValue() < 50.0) {
                            famillesARisque++;
                        }
                    }
                }
            }
        }

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("periodeDebut", periodeDebut.toString());
        kpi.put("periodeFin", periodeFin.toString());
        kpi.put("totalAmes", totalSouls);
        kpi.put("totalFaiseurs", totalFaiseurs);
        kpi.put("totalFamilles", totalFamilles);
        kpi.put("totalDepartements", totalDepartements);
        kpi.put("activeAlerts", activeAlerts);
        kpi.put("suivisParallelesActifs", suivisParalleles);
        kpi.put("tauxPresenceGlobal", Math.round(tauxPresenceGlobal * 10.0) / 10.0);
        kpi.put("tauxPresenceNouveauxArrivants", calculatePresenceRateByType(TypeDisciple.NOUVEL_ARRIVANT, currentWeek));
        kpi.put("tauxPresenceNouveauxConvertis", calculatePresenceRateByType(TypeDisciple.NOUVEAU_CONVERTI, currentWeek));
        kpi.put("totalSorties", totalSorties);
        kpi.put("totalMaintenus", totalMaintenus);
        kpi.put("rapportsSoumis", rapportsSoumis);
        kpi.put("rapportsEnAttente", rapportsEnAttente);
        kpi.put("famillesARisque", famillesARisque);
        kpi.put("tendancePresence", 0.0);

        Map<String, Long> typeRepartition = new LinkedHashMap<>();
        typeRepartition.put("nouveauxArrivants", nouveauxArrivants);
        typeRepartition.put("nouveauxConvertis", nouveauxConvertis);
        kpi.put("typeRepartition", typeRepartition);

        Map<String, Long> statutRepartition = new LinkedHashMap<>();
        statutRepartition.put("enIntegration", soulRepository.countByStatut(StatutAme.EN_INTEGRATION));
        statutRepartition.put("actif", soulRepository.countByStatut(StatutAme.ACTIF));
        statutRepartition.put("enVeille", soulRepository.countByStatut(StatutAme.EN_VEILLE));
        statutRepartition.put("decroche", soulRepository.countByStatut(StatutAme.DECROCHE));
        kpi.put("statutRepartition", statutRepartition);

        return kpi;
    }

    private double calculatePresenceRateByType(TypeDisciple type, LocalDate semaine) {
        List<Soul> souls = soulRepository.findByTypeDisciple(type,
                org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();

        int totalPresents = 0;
        int totalPossible = 0;

        for (Soul soul : souls) {
            List<MakerReport> reports = makerReportRepository.findByAmeIdAndSemaine(soul.getId(), semaine);
            for (MakerReport report : reports) {
                if (report.getPresencesParCulte() != null) {
                    for (Boolean present : report.getPresencesParCulte().values()) {
                        totalPossible++;
                        if (present) totalPresents++;
                    }
                }
            }
        }

        return totalPossible > 0 ? Math.round((double) totalPresents / totalPossible * 1000.0) / 10.0 : 0.0;
    }

    public Map<String, Object> getPresenceTrend(int mois) {
        Map<String, Object> trend = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        for (int i = mois - 1; i >= 0; i--) {
            LocalDate weekStart = now.minusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            double taux = calculatePresenceRateForWeek(weekStart);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("semaine", weekStart.toString());
            point.put("taux", taux);
            dataPoints.add(point);
        }

        trend.put("mois", mois);
        trend.put("data", dataPoints);
        return trend;
    }

    private double calculatePresenceRateForWeek(LocalDate semaine) {
        List<MakerReport> reports = makerReportRepository.findBySemaine(semaine,
                org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();
        int totalPresents = 0;
        int totalPossible = 0;
        for (MakerReport report : reports) {
            if (report.getPresencesParCulte() != null) {
                for (Boolean present : report.getPresencesParCulte().values()) {
                    totalPossible++;
                    if (present) totalPresents++;
                }
            }
        }
        return totalPossible > 0 ? Math.round((double) totalPresents / totalPossible * 1000.0) / 10.0 : 0.0;
    }

    public Map<String, Object> getFamilyRisk(double seuil) {
        Map<String, Object> risk = new LinkedHashMap<>();
        risk.put("seuil", seuil);

        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Map<String, Object>> familiesAtRisk = new ArrayList<>();

        List<Family> allFamilies = familyRepository.findAll();
        for (Family family : allFamilies) {
            List<FamilyReport> reports = familyReportRepository.findByFamilleIdAndSemaine(
                    family.getId(), currentWeek);
            if (!reports.isEmpty()) {
                FamilyReport fr = reports.get(0);
                if (fr.getPresenceMoyenne() != null && fr.getPresenceMoyenne().doubleValue() < seuil) {
                    Map<String, Object> familyInfo = new LinkedHashMap<>();
                    familyInfo.put("familleId", family.getId());
                    familyInfo.put("nom", family.getNom());
                    familyInfo.put("tauxPresence", fr.getPresenceMoyenne());
                    familiesAtRisk.add(familyInfo);
                }
            }
        }

        risk.put("familles", familiesAtRisk);
        risk.put("nbFamillesARisque", familiesAtRisk.size());
        return risk;
    }

    public Map<String, Object> getReportCompletion() {
        Map<String, Object> completion = new LinkedHashMap<>();
        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        long totalFaiseurs = userRepository.countByRole(UserRole.FAISEUR);
        List<MakerReport> currentWeekReports = makerReportRepository.findBySemaine(currentWeek,
                org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();

        Set<UUID> faiseursWithReports = new HashSet<>();
        int totalReports = 0;
        int submittedReports = 0;

        for (MakerReport report : currentWeekReports) {
            faiseursWithReports.add(report.getFaiseurId());
            totalReports++;
            if (report.isSoumis()) submittedReports++;
        }

        double tauxCompletion = totalFaiseurs > 0
                ? (double) faiseursWithReports.size() / totalFaiseurs * 100.0 : 0.0;

        completion.put("semaineCourante", currentWeek.toString());
        completion.put("totalFaiseurs", totalFaiseurs);
        completion.put("faiseursAyantRapporte", faiseursWithReports.size());
        completion.put("totalRapports", totalReports);
        completion.put("rapportsSoumis", submittedReports);
        completion.put("tauxCompletion", Math.round(tauxCompletion * 10.0) / 10.0);

        return completion;
    }
}
