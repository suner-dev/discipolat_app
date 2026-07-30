package com.discipolat.modules.dashboard.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.StatutSuiviParallele;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowupRepository;
import com.discipolat.modules.reports.domain.FamilyReport;
import com.discipolat.modules.reports.domain.FamilyReportRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulNoteRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final MakerReportRepository makerReportRepository;
    private final FamilyReportRepository familyReportRepository;
    private final AlertRepository alertRepository;
    private final SoulNoteRepository soulNoteRepository;
    private final ParallelFollowupRepository parallelFollowupRepository;
    private final DepartmentRepository departmentRepository;
    private final SecurityUtils securityUtils;

    public DashboardService(SoulRepository soulRepository, UserRepository userRepository,
                           FamilyRepository familyRepository, MakerReportRepository makerReportRepository,
                           FamilyReportRepository familyReportRepository, AlertRepository alertRepository,
                           SoulNoteRepository soulNoteRepository,
                           ParallelFollowupRepository parallelFollowupRepository,
                           DepartmentRepository departmentRepository,
                           SecurityUtils securityUtils) {
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.makerReportRepository = makerReportRepository;
        this.familyReportRepository = familyReportRepository;
        this.alertRepository = alertRepository;
        this.soulNoteRepository = soulNoteRepository;
        this.parallelFollowupRepository = parallelFollowupRepository;
        this.departmentRepository = departmentRepository;
        this.securityUtils = securityUtils;
    }

    @Cacheable(value = "dashboardKpi", unless = "#result == null")
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

    @Cacheable(value = "dashboardKpi", unless = "#result == null")
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

    @Cacheable(value = "dashboardTrends", unless = "#result == null")
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

    @Cacheable(value = "dashboardTrends", unless = "#result == null")
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

    @Cacheable(value = "dashboardKpi", unless = "#result == null")
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

    /**
     * Scheduled cache eviction — refreshes dashboard data periodically.
     * Runs at 2 AM daily (matches the dashboard-metrics-cron in application.yml)
     * plus every 30 minutes during peak hours.
     */
    @Scheduled(cron = "0 0/30 * * * *")
    @CacheEvict(value = {"dashboardKpi", "dashboardTrends"}, allEntries = true)
    public void evictDashboardCache() {
        // Cache will be repopulated on next request
    }

    // ========================================================================
    // PHASE 2: PASTEUR COMMAND CENTER
    // ========================================================================

    @Cacheable(value = "dashboardKpi", unless = "#result == null")
    public Map<String, Object> getPasteurDashboard() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> dashboard = new LinkedHashMap<>();
        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // ==================== CROISSANCE GLOBALE ====================
        Map<String, Object> croissance = new LinkedHashMap<>();
        long totalSouls = soulRepository.count();
        long nouveauxArrivants = soulRepository.countByTypeDisciple(TypeDisciple.NOUVEL_ARRIVANT);
        long nouveauxConvertis = soulRepository.countByTypeDisciple(TypeDisciple.NOUVEAU_CONVERTI);
        long actifs = soulRepository.countByStatut(StatutAme.ACTIF);
        long enIntegration = soulRepository.countByStatut(StatutAme.EN_INTEGRATION);
        long enVeille = soulRepository.countByStatut(StatutAme.EN_VEILLE);
        long decroches = soulRepository.countByStatut(StatutAme.DECROCHE);

        croissance.put("totalAmes", totalSouls);
        croissance.put("nouveauxArrivants", nouveauxArrivants);
        croissance.put("nouveauxConvertis", nouveauxConvertis);
        croissance.put("actifs", actifs);
        croissance.put("enIntegration", enIntegration);
        croissance.put("enVeille", enVeille);
        croissance.put("decroches", decroches);

        // Taux de conversion (nouveaux convertis / total)
        double tauxConversion = totalSouls > 0
                ? Math.round((double) nouveauxConvertis / totalSouls * 1000.0) / 10.0
                : 0.0;
        croissance.put("tauxConversion", tauxConversion);

        dashboard.put("croissance", croissance);

        // ==================== DÉPARTEMENTS ====================
        List<Department> allDepartements = departmentRepository.findAll();
        List<Map<String, Object>> deptCroissance = new ArrayList<>();
        for (Department dept : allDepartements) {
            List<Family> familles = familyRepository.findByDepartementId(dept.getId());
            List<UUID> famIds = familles.stream().map(Family::getId).toList();
            long totalAmesDept = famIds.isEmpty() ? 0
                    : soulRepository.findByFamilleIdIn(famIds).stream().filter(s -> !s.isDeleted()).count();

            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id", dept.getId());
            d.put("nom", dept.getNom());
            d.put("totalFamilles", (long) familles.size());
            d.put("totalAmes", totalAmesDept);
            d.put("responsableId", dept.getResponsableId());
            Optional<User> resp = userRepository.findById(dept.getResponsableId());
            d.put("responsableNom", resp.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("N/A"));
            deptCroissance.add(d);
        }
        dashboard.put("departements", deptCroissance);

        // ==================== FAMILLES ====================
        List<Family> allFamilies = familyRepository.findAll();
        List<Map<String, Object>> familleCroissance = new ArrayList<>();
        for (Family fam : allFamilies) {
            List<Soul> ames = soulRepository.findAllByFamilleId(fam.getId());
            long actifsFam = ames.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long enIntFam = ames.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();

            // Average presence rate for this family
            List<FamilyReport> frs = familyReportRepository.findByFamilleIdAndSemaine(fam.getId(), currentWeek);
            BigDecimal presenceMoyenne = frs.isEmpty() || frs.get(0).getPresenceMoyenne() == null
                    ? BigDecimal.ZERO : frs.get(0).getPresenceMoyenne();

            Optional<User> chef = userRepository.findById(fam.getChefFamilleId());

            Map<String, Object> f = new LinkedHashMap<>();
            f.put("id", fam.getId());
            f.put("nom", fam.getNom());
            f.put("departementId", fam.getDepartementId());
            f.put("totalAmes", (long) ames.size());
            f.put("actifs", actifsFam);
            f.put("enIntegration", enIntFam);
            f.put("tauxPresence", presenceMoyenne);
            f.put("chefFamilleId", fam.getChefFamilleId());
            f.put("chefNom", chef.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("N/A"));
            f.put("aRisque", presenceMoyenne.compareTo(BigDecimal.valueOf(50)) < 0);
            familleCroissance.add(f);
        }
        dashboard.put("familles", familleCroissance);

        // ==================== FAISEURS ====================
        List<User> tousFaiseurs = userRepository.findByRolesContaining(UserRole.FAISEUR);
        List<Map<String, Object>> faiseurData = new ArrayList<>();
        for (User faiseur : tousFaiseurs) {
            List<Soul> amesFaiseur = soulRepository.findAllByFaiseurId(faiseur.getId());
            long actifsFai = amesFaiseur.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long enIntFai = amesFaiseur.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();

            Map<String, Object> f = new LinkedHashMap<>();
            f.put("id", faiseur.getId());
            f.put("nom", faiseur.getFirstName() + " " + faiseur.getLastName());
            f.put("email", faiseur.getEmail());
            f.put("totalAmes", (long) amesFaiseur.size());
            f.put("actifs", actifsFai);
            f.put("enIntegration", enIntFai);
            f.put("estChef", faiseur.isEstChefDeFamille());
            f.put("statut", faiseur.getStatut().name());
            faiseurData.add(f);
        }
        dashboard.put("faiseurs", faiseurData);

        // ==================== PRÉSENCES ====================
        Map<String, Object> presences = new LinkedHashMap<>();
        Page<MakerReport> reportsPage = makerReportRepository.findBySemaine(currentWeek,
                org.springframework.data.domain.PageRequest.of(0, 10000));
        List<MakerReport> weekReports = reportsPage.getContent();
        int presents = 0, totalPossibles = 0;
        for (MakerReport r : weekReports) {
            if (r.getPresencesParCulte() != null) {
                for (Boolean p : r.getPresencesParCulte().values()) {
                    totalPossibles++;
                    if (p) presents++;
                }
            }
        }
        double tauxGlobal = totalPossibles > 0 ? Math.round((double) presents / totalPossibles * 1000.0) / 10.0 : 0.0;
        presences.put("tauxGlobal", tauxGlobal);
        presences.put("presents", presents);
        presences.put("totalPossibles", totalPossibles);

        // Présence par type
        double tauxNA = calculatePresenceRateByType(TypeDisciple.NOUVEL_ARRIVANT, currentWeek);
        double tauxNC = calculatePresenceRateByType(TypeDisciple.NOUVEAU_CONVERTI, currentWeek);
        presences.put("tauxNouveauxArrivants", tauxNA);
        presences.put("tauxNouveauxConvertis", tauxNC);
        dashboard.put("presences", presences);

        // ==================== ALERTES ====================
        long alertesActives = alertRepository.countByStatut(StatutAlerte.ACTIVE);
        dashboard.put("alertesActives", alertesActives);

        // ==================== RAPPORTS ====================
        Map<String, Object> rapports = new LinkedHashMap<>();
        long totalFaiseurs = tousFaiseurs.size();
        Set<UUID> faiseursAvecRapport = new HashSet<>();
        int soumis = 0, enAttente = 0;
        for (MakerReport r : weekReports) {
            faiseursAvecRapport.add(r.getFaiseurId());
            if (r.isSoumis()) soumis++; else enAttente++;
        }
        double tauxCompletion = totalFaiseurs > 0
                ? Math.round((double) faiseursAvecRapport.size() / totalFaiseurs * 1000.0) / 10.0
                : 0.0;
        rapports.put("tauxCompletion", tauxCompletion);
        rapports.put("soumis", soumis);
        rapports.put("enAttente", enAttente);
        rapports.put("faiseursAyantRapporte", faiseursAvecRapport.size());
        rapports.put("totalFaiseurs", totalFaiseurs);
        dashboard.put("rapports", rapports);

        // ==================== SUIVIS PARALLÈLES ====================
        long suivisActifs = parallelFollowupRepository.countByStatut(StatutSuiviParallele.EN_COURS);
        dashboard.put("suivisParallelesActifs", suivisActifs);

        // ==================== FAMILLES À RISQUE ====================
        List<Map<String, Object>> famillesRisque = new ArrayList<>();
        for (Family fam : allFamilies) {
            List<FamilyReport> frs = familyReportRepository.findByFamilleIdAndSemaine(fam.getId(), currentWeek);
            if (!frs.isEmpty() && frs.get(0).getPresenceMoyenne() != null
                    && frs.get(0).getPresenceMoyenne().doubleValue() < 50.0) {
                Map<String, Object> fr = new LinkedHashMap<>();
                fr.put("id", fam.getId());
                fr.put("nom", fam.getNom());
                fr.put("tauxPresence", frs.get(0).getPresenceMoyenne());
                famillesRisque.add(fr);
            }
        }
        dashboard.put("famillesARisque", famillesRisque);

        dashboard.put("semaine", currentWeek.toString());
        return dashboard;
    }

    // ========================================================================
    // PHASE 2: CHEF DE FAMILLE DASHBOARD
    // ========================================================================

    @Cacheable(value = "dashboardKpi", unless = "#result == null")
    public Map<String, Object> getChefFamilleDashboard(UUID familleIdInput) {
        UUID resolvedFamilleId = familleIdInput;
        if (resolvedFamilleId == null) {
            UUID currentUser = securityUtils.getCurrentUserId();
            User user = userRepository.findById(currentUser)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (user.getFamilleGereeId() == null) {
                return Map.of("message", "Aucune famille assignée");
            }
            resolvedFamilleId = user.getFamilleGereeId();
        }
        final UUID finalFamilleId = resolvedFamilleId;
        UUID currentUserId = securityUtils.getCurrentUserId();
        Map<String, Object> dashboard = new LinkedHashMap<>();
        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // ==================== INFOS FAMILLE ====================
        Family family = familyRepository.findById(finalFamilleId)
                .orElseThrow(() -> new RuntimeException("Family not found: " + finalFamilleId));

        Map<String, Object> infos = new LinkedHashMap<>();
        infos.put("id", family.getId());
        infos.put("nom", family.getNom());
        infos.put("departementId", family.getDepartementId());
        infos.put("dateCreation", family.getDateCreation().toString());
        Optional<User> chef = userRepository.findById(family.getChefFamilleId());
        infos.put("chefNom", chef.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("N/A"));
        dashboard.put("famille", infos);

        // ==================== FAISEURS DE LA FAMILLE ====================
        List<User> faiseurs = userRepository.findByFamilleGereeId(finalFamilleId);
        List<Map<String, Object>> faiseurList = new ArrayList<>();
        for (User faiseur : faiseurs) {
            List<Soul> ames = soulRepository.findAllByFaiseurId(faiseur.getId());
            long actifs = ames.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long enInt = ames.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();
            long veille = ames.stream().filter(s -> s.getStatut() == StatutAme.EN_VEILLE).count();
            long decroche = ames.stream().filter(s -> s.getStatut() == StatutAme.DECROCHE).count();

            List<MakerReport> reports = makerReportRepository.findByFaiseurIdAndSemaine(faiseur.getId(), currentWeek);
            boolean rapportSoumis = reports.stream().anyMatch(MakerReport::isSoumis);

            Map<String, Object> f = new LinkedHashMap<>();
            f.put("id", faiseur.getId());
            f.put("nom", faiseur.getFirstName() + " " + faiseur.getLastName());
            f.put("email", faiseur.getEmail());
            f.put("telephone", faiseur.getPhone());
            f.put("totalAmes", (long) ames.size());
            f.put("actifs", actifs);
            f.put("enIntegration", enInt);
            f.put("enVeille", veille);
            f.put("decroches", decroche);
            f.put("rapportSoumis", rapportSoumis);
            faiseurList.add(f);
        }
        dashboard.put("faiseurs", faiseurList);

        // ==================== TOUS LES DISCIPLES ====================
        List<Soul> tousDisciples = soulRepository.findAllByFamilleId(finalFamilleId);
        List<Map<String, Object>> discipleList = new ArrayList<>();
        for (Soul soul : tousDisciples) {
            if (soul.isDeleted()) continue;
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("id", soul.getId());
            s.put("nom", soul.getNomComplet());
            s.put("statut", soul.getStatut().name());
            s.put("type", soul.getTypeDisciple().name());
            s.put("etatSpirituel", soul.getEtatSpirituel());
            s.put("niveauCroissance", soul.getNiveauCroissance());
            s.put("faiseurId", soul.getFaiseurId());
            s.put("dateIntegration", soul.getDateIntegration() != null ? soul.getDateIntegration().toString() : null);
            s.put("dateDernierContact", soul.getDateDernierContact() != null ? soul.getDateDernierContact().toString() : null);

            Optional<User> faiseur = userRepository.findById(soul.getFaiseurId());
            s.put("faiseurNom", faiseur.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("N/A"));

            // Dernier rapport
            List<MakerReport> lastReports = makerReportRepository.findByAmeIdAndSemaine(soul.getId(), currentWeek);
            s.put("rapportSemaine", lastReports.stream().anyMatch(MakerReport::isSoumis));

            discipleList.add(s);
        }
        dashboard.put("disciples", discipleList);

        // ==================== STATISTIQUES ====================
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDisciples", (long) discipleList.size());
        stats.put("totalFaiseurs", (long) faiseurs.size());
        long actifsTotal = discipleList.stream().filter(s -> "ACTIF".equals(s.get("statut"))).count();
        long enIntTotal = discipleList.stream().filter(s -> "EN_INTEGRATION".equals(s.get("statut"))).count();
        long veilleTotal = discipleList.stream().filter(s -> "EN_VEILLE".equals(s.get("statut"))).count();
        long decrocheTotal = discipleList.stream().filter(s -> "DECROCHE".equals(s.get("statut"))).count();
        long rapportsSoumis = discipleList.stream().filter(s -> Boolean.TRUE.equals(s.get("rapportSemaine"))).count();

        stats.put("actifs", actifsTotal);
        stats.put("enIntegration", enIntTotal);
        stats.put("enVeille", veilleTotal);
        stats.put("decroches", decrocheTotal);
        stats.put("rapportsSoumisSemaine", rapportsSoumis);
        stats.put("rapportsEnAttente", discipleList.size() - rapportsSoumis);
        dashboard.put("statistiques", stats);

        dashboard.put("semaine", currentWeek.toString());
        return dashboard;
    }

    // ========================================================================
    // PHASE 2: RESPONSABLE DASHBOARD
    // ========================================================================

    @Cacheable(value = "dashboardKpi", unless = "#result == null")
    public Map<String, Object> getResponsableDashboard() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Map<String, Object> dashboard = new LinkedHashMap<>();
        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // ==================== DÉPARTEMENTS DU RESPONSABLE ====================
        List<Department> depts = departmentRepository.findByResponsableId(currentUserId);
        if (depts.isEmpty()) {
            dashboard.put("message", "Aucun département assigné");
            return dashboard;
        }

        List<Map<String, Object>> deptList = new ArrayList<>();
        for (Department dept : depts) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id", dept.getId());
            d.put("nom", dept.getNom());
            d.put("description", dept.getDescription());
            d.put("statut", dept.getStatut().name());

            // Familles du département
            List<Family> familles = familyRepository.findByDepartementId(dept.getId());
            List<UUID> famIds = familles.stream().map(Family::getId).toList();

            // Âmes
            List<Soul> allSouls = famIds.isEmpty() ? List.of() : soulRepository.findByFamilleIdIn(famIds);
            long actifs = allSouls.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long enInt = allSouls.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();
            long veille = allSouls.stream().filter(s -> s.getStatut() == StatutAme.EN_VEILLE).count();
            long decroche = allSouls.stream().filter(s -> s.getStatut() == StatutAme.DECROCHE).count();

            // Faiseurs
            Set<UUID> faiseurIds = allSouls.stream().map(Soul::getFaiseurId).collect(Collectors.toSet());
            Set<UUID> chefIds = familles.stream().map(Family::getChefFamilleId).collect(Collectors.toSet());
            faiseurIds.addAll(chefIds);

            // Présence
            int presents = 0, totalPossibles = 0;
            for (UUID fid : faiseurIds) {
                List<MakerReport> reports = makerReportRepository.findByFaiseurIdAndSemaine(fid, currentWeek);
                for (MakerReport r : reports) {
                    if (r.getPresencesParCulte() != null) {
                        for (Boolean p : r.getPresencesParCulte().values()) {
                            totalPossibles++;
                            if (p) presents++;
                        }
                    }
                }
            }
            double tauxPresence = totalPossibles > 0 ? Math.round((double) presents / totalPossibles * 1000.0) / 10.0 : 0.0;

            // Rapports
            long rapportsSoumis = 0, rapportsAttendus = 0;
            for (UUID fid : faiseurIds) {
                List<MakerReport> reports = makerReportRepository.findByFaiseurIdAndSemaine(fid, currentWeek);
                rapportsSoumis += reports.stream().filter(MakerReport::isSoumis).count();
                rapportsAttendus += soulRepository.findAllByFaiseurId(fid).stream()
                        .filter(s -> !s.isDeleted() && s.getStatut() != StatutAme.DECROCHE).count();
            }

            d.put("totalFamilles", (long) familles.size());
            d.put("totalAmes", (long) allSouls.size());
            d.put("actifs", actifs);
            d.put("enIntegration", enInt);
            d.put("enVeille", veille);
            d.put("decroches", decroche);
            d.put("totalFaiseurs", (long) faiseurIds.size());
            d.put("tauxPresence", tauxPresence);
            d.put("rapportsSoumis", rapportsSoumis);
            d.put("rapportsAttendus", rapportsAttendus);
            deptList.add(d);
        }
        dashboard.put("departements", deptList);

        // ==================== STATISTIQUES GLOBALES ====================
        Map<String, Object> stats = new LinkedHashMap<>();
        long totalFamilles = 0, totalAmes = 0, totalActifs = 0, totalFaiseurs = 0;
        int totalPresents = 0, totalPossibles = 0;
        long totalRapportsSoumis = 0, totalRapportsAttendus = 0;

        for (Map<String, Object> d : deptList) {
            totalFamilles += (Long) d.getOrDefault("totalFamilles", 0L);
            totalAmes += (Long) d.getOrDefault("totalAmes", 0L);
            totalActifs += (Long) d.getOrDefault("actifs", 0L);
            totalFaiseurs += (Long) d.getOrDefault("totalFaiseurs", 0L);
            totalRapportsSoumis += (Long) d.getOrDefault("rapportsSoumis", 0L);
            totalRapportsAttendus += (Long) d.getOrDefault("rapportsAttendus", 0L);
        }

        stats.put("totalFamilles", totalFamilles);
        stats.put("totalAmes", totalAmes);
        stats.put("totalActifs", totalActifs);
        stats.put("totalFaiseurs", totalFaiseurs);
        stats.put("rapportsSoumis", totalRapportsSoumis);
        stats.put("rapportsAttendus", totalRapportsAttendus);
        double tauxCompletion = totalRapportsAttendus > 0
                ? Math.round((double) totalRapportsSoumis / totalRapportsAttendus * 1000.0) / 10.0
                : 0.0;
        stats.put("tauxCompletion", tauxCompletion);
        dashboard.put("statistiques", stats);

        dashboard.put("semaine", currentWeek.toString());
        return dashboard;
    }

    // ========================================================================
    // PHASE 3: CRM FAISEUR
    // ========================================================================

    public Map<String, Object> getCrmFaiseurDashboard() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> crm = new LinkedHashMap<>();
        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // ==================== MES DISCIPLES ====================
        List<Soul> mesAmes = soulRepository.findAllByFaiseurId(currentUserId);
        List<Map<String, Object>> disciples = new ArrayList<>();
        for (Soul soul : mesAmes) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id", soul.getId());
            d.put("nom", soul.getNomComplet());
            d.put("statut", soul.getStatut().name());
            d.put("typeDisciple", soul.getTypeDisciple().name());
            d.put("etatSpirituel", soul.getEtatSpirituel());
            d.put("niveauCroissance", soul.getNiveauCroissance());
            d.put("dateIntegration", soul.getDateIntegration() != null ? soul.getDateIntegration().toString() : null);
            d.put("dateDernierContact", soul.getDateDernierContact() != null ? soul.getDateDernierContact().toString() : null);
            d.put("telephone", soul.getTelephone());
            d.put("ville", soul.getAdresse() != null ? soul.getAdresse().split(",")[0].trim() : null);

            // Dernier rapport
            List<MakerReport> rapports = makerReportRepository.findByAmeIdAndSemaine(soul.getId(), currentWeek);
            MakerReport dernierRapport = rapports.isEmpty() ? null : rapports.get(0);
            if (dernierRapport != null) {
                d.put("rapportSoumis", dernierRapport.isSoumis());
                d.put("rapportSemaine", currentWeek.toString());
                d.put("difficultes", dernierRapport.getDifficultes());
                if (dernierRapport.getPresencesParCulte() != null) {
                    long presents = dernierRapport.getPresencesParCulte().values().stream().filter(b -> b).count();
                    long total = dernierRapport.getPresencesParCulte().size();
                    d.put("presences", presents);
                    d.put("totalCultes", total);
                }
            } else {
                d.put("rapportSoumis", false);
            }

            // Notes récentes
            d.put("nbNotes", soulNoteRepository.countByAmeIdAndDeletedFalse(soul.getId()));

            disciples.add(d);
        }
        crm.put("disciples", disciples);

        // ==================== STATISTIQUES ====================
        Map<String, Object> stats = new LinkedHashMap<>();
        long actifs = disciples.stream().filter(d -> "ACTIF".equals(d.get("statut"))).count();
        long enInt = disciples.stream().filter(d -> "EN_INTEGRATION".equals(d.get("statut"))).count();
        long veille = disciples.stream().filter(d -> "EN_VEILLE".equals(d.get("statut"))).count();
        long decroche = disciples.stream().filter(d -> "DECROCHE".equals(d.get("statut"))).count();
        long rapportsSoumis = disciples.stream().filter(d -> Boolean.TRUE.equals(d.get("rapportSoumis"))).count();
        long enDifficulte = disciples.stream()
                .filter(d -> "EN_DIFFICULTE".equals(d.get("etatSpirituel")) 
                        || "DECROCHE".equals(d.get("statut")))
                .count();

        stats.put("totalDisciples", (long) disciples.size());
        stats.put("actifs", actifs);
        stats.put("enIntegration", enInt);
        stats.put("enVeille", veille);
        stats.put("decroches", decroche);
        stats.put("rapportsSoumisSemaine", rapportsSoumis);
        stats.put("enDifficulte", enDifficulte);
        stats.put("semaine", currentWeek.toString());
        crm.put("statistiques", stats);

        // ==================== ALERTES ====================
        List<Map<String, Object>> alertes = new ArrayList<>();
        for (Soul soul : mesAmes) {
            if ("EN_DIFFICULTE".equals(soul.getEtatSpirituel())) {
                alertes.add(Map.of(
                    "type", "DIFFICULTE",
                    "soulId", soul.getId().toString(),
                    "soulNom", soul.getNomComplet(),
                    "message", "En difficulté spirituelle",
                    "priorite", "HAUTE"
                ));
            }
            if (soul.getDateDernierContact() != null
                    && soul.getDateDernierContact().plusDays(14).isBefore(LocalDateTime.now())) {
                alertes.add(Map.of(
                    "type", "ABSENCE_CONTACT",
                    "soulId", soul.getId().toString(),
                    "soulNom", soul.getNomComplet(),
                    "message", "Pas de contact depuis 14+ jours",
                    "priorite", "MOYENNE"
                ));
            }
        }
        crm.put("alertes", alertes);

        return crm;
    }

    public Map<String, Object> getCurrentUserMetrics() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("userId", currentUserId);
        metrics.put("role", currentUser.getActiveRole() != null ? currentUser.getActiveRole().name() : currentUser.getRole().name());
        metrics.put("estChefDeFamille", currentUser.isEstChefDeFamille());

        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // ======================== FAISEUR ========================
        if (currentUser.getRoles().contains(UserRole.FAISEUR)) {
            List<Soul> mySouls = soulRepository.findAllByFaiseurId(currentUserId);
            long actifs = mySouls.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long enIntegration = mySouls.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();
            long enVeille = mySouls.stream().filter(s -> s.getStatut() == StatutAme.EN_VEILLE).count();
            long decroche = mySouls.stream().filter(s -> s.getStatut() == StatutAme.DECROCHE).count();

            List<MakerReport> myReports = makerReportRepository.findByFaiseurIdAndSemaine(currentUserId, currentWeek);
            boolean reportSoumis = myReports.stream().anyMatch(MakerReport::isSoumis);

            metrics.put("totalAmes", mySouls.size());
            metrics.put("amesActives", actifs);
            metrics.put("amesEnIntegration", enIntegration);
            metrics.put("amesEnVeille", enVeille);
            metrics.put("amesDecrochees", decroche);
            metrics.put("rapportSoumisCetteSemaine", reportSoumis);
            metrics.put("totalRapportsSoumis", makerReportRepository.countByFaiseurIdAndSoumisTrue(currentUserId));
        }

        // ======================== CHEF DE FAMILLE ========================
        if (currentUser.isEstChefDeFamille() && currentUser.getFamilleGereeId() != null) {
            UUID familleId = currentUser.getFamilleGereeId();
            List<Soul> familySouls = soulRepository.findAllByFamilleId(familleId);
            List<User> faiseurs = userRepository.findByFamilleGereeId(familleId);
            List<UUID> faiseurIds = faiseurs.stream().map(User::getId).toList();

            long actifs = familySouls.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long enIntegration = familySouls.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();

            metrics.put("familleId", familleId);
            metrics.put("totalAmesFamille", familySouls.size());
            metrics.put("amesActivesFamille", actifs);
            metrics.put("amesEnIntegrationFamille", enIntegration);
            metrics.put("totalFaiseursFamille", faiseurs.size());

            List<MakerReport> familyReports = makerReportRepository.findByFaiseurIdInAndSemaine(faiseurIds, currentWeek);
            long soumis = familyReports.stream().filter(MakerReport::isSoumis).count();
            metrics.put("rapportsSoumisFamille", soumis);
            metrics.put("rapportsEnAttenteFamille", familyReports.size() - soumis);
        }

        // ======================== RESPONSABLE ========================
        if (currentUser.getRoles().contains(UserRole.RESPONSABLE)) {
            List<Department> depts = departmentRepository.findByResponsableId(currentUserId);
            List<UUID> deptIds = depts.stream().map(Department::getId).toList();

            List<Family> familles = deptIds.isEmpty() ? List.of() : familyRepository.findByDepartementIdIn(deptIds);
            List<UUID> familyIds = familles.stream().map(Family::getId).toList();

            List<Soul> allSouls = familyIds.isEmpty() ? List.of() : soulRepository.findByFamilleIdIn(familyIds);
            long actifs = allSouls.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long enIntegration = allSouls.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();

            Set<UUID> faiseurIds = allSouls.stream().map(Soul::getFaiseurId).collect(Collectors.toSet());
            Set<UUID> chefIds = familles.stream().map(Family::getChefFamilleId).collect(Collectors.toSet());
            faiseurIds.addAll(chefIds);

            metrics.put("departements", depts.stream().map(d -> Map.<String, Object>of("id", d.getId(), "nom", d.getNom())).toList());
            metrics.put("totalDepartements", (long) depts.size());
            metrics.put("totalFamilles", (long) familles.size());
            metrics.put("totalAmes", (long) allSouls.size());
            metrics.put("amesActives", actifs);
            metrics.put("amesEnIntegration", enIntegration);
            metrics.put("totalFaiseurs", (long) faiseurIds.size());

            // Rapports soumis cette semaine
            long rapportsSoumis = 0;
            long rapportsAttendus = 0;
            if (!faiseurIds.isEmpty()) {
                for (UUID fid : faiseurIds) {
                    List<MakerReport> reports = makerReportRepository.findByFaiseurIdAndSemaine(fid, currentWeek);
                    rapportsSoumis += reports.stream().filter(MakerReport::isSoumis).count();
                    rapportsAttendus += soulRepository.findAllByFaiseurId(fid).stream()
                            .filter(s -> !s.isDeleted() && s.getStatut() != StatutAme.DECROCHE).count();
                }
            }
            metrics.put("rapportsSoumis", rapportsSoumis);
            metrics.put("rapportsAttendus", rapportsAttendus);
        }

        return metrics;
    }
}
