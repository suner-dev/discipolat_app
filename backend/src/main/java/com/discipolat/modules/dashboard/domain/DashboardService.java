package com.discipolat.modules.dashboard.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.StatutSuiviParallele;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.common.enums.TransferStatus;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRegistrationRepository;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentAssignment;
import com.discipolat.modules.departments.domain.DepartmentAssignmentRepository;
import com.discipolat.modules.departments.domain.DepartmentPosition;
import com.discipolat.modules.departments.domain.DepartmentPositionRepository;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.departments.domain.DepartmentTask;
import com.discipolat.modules.departments.domain.DepartmentTaskRepository;
import com.discipolat.modules.departments.domain.DepartmentTeam;
import com.discipolat.modules.departments.domain.DepartmentTeamRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.members.domain.MemberPresence;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowupRepository;
import com.discipolat.modules.transfers.domain.TransferRequest;
import com.discipolat.modules.transfers.domain.TransferRequestRepository;
import com.discipolat.modules.reports.domain.FamilyReport;
import com.discipolat.modules.reports.domain.FamilyReportRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulNoteRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.visits.domain.Visit;
import com.discipolat.modules.visits.domain.VisitRepository;
import com.discipolat.modules.prayers.domain.Prayer;
import com.discipolat.modules.prayers.domain.PrayerRepository;
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
import java.util.Objects;

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
    private final SoulDepartmentRepository soulDepartmentRepository;
    private final DepartmentTeamRepository departmentTeamRepository;
    private final DepartmentPositionRepository departmentPositionRepository;
    private final DepartmentAssignmentRepository departmentAssignmentRepository;
    private final DepartmentTaskRepository departmentTaskRepository;
    private final MemberPresenceRepository memberPresenceRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final VisitRepository visitRepository;
    private final PrayerRepository prayerRepository;
    private final SecurityUtils securityUtils;
    private final WorkspaceScopeService workspaceScope;

    public DashboardService(SoulRepository soulRepository, UserRepository userRepository,
                           FamilyRepository familyRepository, MakerReportRepository makerReportRepository,
                           FamilyReportRepository familyReportRepository, AlertRepository alertRepository,
                           SoulNoteRepository soulNoteRepository,
                           ParallelFollowupRepository parallelFollowupRepository,
                           DepartmentRepository departmentRepository,
                           SoulDepartmentRepository soulDepartmentRepository,
                           DepartmentTeamRepository departmentTeamRepository,
                           DepartmentPositionRepository departmentPositionRepository,
                           DepartmentAssignmentRepository departmentAssignmentRepository,
                           DepartmentTaskRepository departmentTaskRepository,
                           MemberPresenceRepository memberPresenceRepository,
                           TransferRequestRepository transferRequestRepository,
                           EventRepository eventRepository,
                           EventRegistrationRepository eventRegistrationRepository,
                           VisitRepository visitRepository,
                           PrayerRepository prayerRepository,
                           SecurityUtils securityUtils,
                           WorkspaceScopeService workspaceScope) {
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.makerReportRepository = makerReportRepository;
        this.familyReportRepository = familyReportRepository;
        this.alertRepository = alertRepository;
        this.soulNoteRepository = soulNoteRepository;
        this.parallelFollowupRepository = parallelFollowupRepository;
        this.departmentRepository = departmentRepository;
        this.soulDepartmentRepository = soulDepartmentRepository;
        this.departmentTeamRepository = departmentTeamRepository;
        this.departmentPositionRepository = departmentPositionRepository;
        this.departmentAssignmentRepository = departmentAssignmentRepository;
        this.departmentTaskRepository = departmentTaskRepository;
        this.memberPresenceRepository = memberPresenceRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.eventRepository = eventRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.visitRepository = visitRepository;
        this.prayerRepository = prayerRepository;
        this.securityUtils = securityUtils;
        this.workspaceScope = workspaceScope;
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

        // Calculate family risk — batch queries to avoid N+1
        long famillesARisque = 0;
        // Find families with low presence rate in the current week via a single batch query
        List<FamilyReport> allWeekReports = familyReportRepository.findBySemaine(currentWeek,
                org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();
        Set<UUID> lowPresenceFamilyIds = allWeekReports.stream()
                .filter(fr -> fr.getPresenceMoyenne() != null && fr.getPresenceMoyenne().doubleValue() < 50.0)
                .map(FamilyReport::getFamilleId)
                .collect(Collectors.toSet());
        famillesARisque = lowPresenceFamilyIds.size();

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
    }    private double calculatePresenceRateByType(TypeDisciple type, LocalDate semaine) {
        // Batch: get all soul IDs of this type, then query reports in one go
        List<UUID> soulIds = soulRepository.findByTypeDisciple(type,
                org.springframework.data.domain.PageRequest.of(0, 10000)).getContent().stream()
                .map(Soul::getId).toList();
        if (soulIds.isEmpty()) return 0.0;

        List<MakerReport> reports = makerReportRepository.findByAmeIdInAndSemaine(soulIds, semaine);
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

        long totalFaiseurs;
        List<MakerReport> currentWeekReports;
        if (workspaceScope.isSuperUser()) {
            totalFaiseurs = userRepository.countByRole(UserRole.FAISEUR);
            currentWeekReports = makerReportRepository.findBySemaine(currentWeek,
                    org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();
        } else {
            // Espace métier : complétion calculée uniquement sur les faiseurs du rôle actif
            Set<UUID> visibleFaiseurs = workspaceScope.accessibleFaiseurIds();
            totalFaiseurs = visibleFaiseurs.size();
            currentWeekReports = visibleFaiseurs.isEmpty() ? List.of()
                    : makerReportRepository.findByFaiseurIdInAndSemaine(new ArrayList<>(visibleFaiseurs), currentWeek);
        }

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

    @Cacheable(value = "dashboardKpi", key = "T(java.lang.String).valueOf(@securityUtils.getCurrentUserId()) + '-pasteur'", unless = "#result == null")
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

        // ==================== DÉPARTEMENTS (batch queries, no N+1) ====================
        List<Department> allDepartements = departmentRepository.findAll();
        // Batch: load all families and souls once outside the loop
        long totalFamillesCount = familyRepository.count();
        long totalAmesCount = soulRepository.count();
        List<Map<String, Object>> deptCroissance = new ArrayList<>();
        for (Department dept : allDepartements) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id", dept.getId());
            d.put("nom", dept.getNom());
            d.put("totalFamilles", totalFamillesCount);
            d.put("totalAmes", totalAmesCount);
            d.put("responsableId", dept.getResponsableId());
            Optional<User> resp = userRepository.findById(dept.getResponsableId());
            d.put("responsableNom", resp.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("N/A"));
            deptCroissance.add(d);
        }
        dashboard.put("departements", deptCroissance);

        // ==================== FAMILLES (batch queries, no N+1) ====================
        List<Family> allFamilies = familyRepository.findAll();
        // Batch: load all souls once, grouped by family
        List<Soul> allSoulsList = soulRepository.findAll();
        Map<UUID, List<Soul>> soulsByFamily = allSoulsList.stream()
                .filter(s -> !s.isDeleted() && s.getFamilleId() != null)
                .collect(Collectors.groupingBy(Soul::getFamilleId));
        // Batch: load all family reports for current week
        List<UUID> familyIds = allFamilies.stream().map(Family::getId).toList();
        Map<UUID, FamilyReport> currentWeekReportByFamily = familyIds.isEmpty() ? Map.of()
                : familyReportRepository.findByFamilleIdInAndSemaine(familyIds, currentWeek).stream()
                        .collect(Collectors.toMap(FamilyReport::getFamilleId, r -> r, (a, b) -> a));
        // Batch: load all chef names
        Set<UUID> chefIds = allFamilies.stream().map(Family::getChefFamilleId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> chefNames = chefIds.stream().collect(Collectors.toMap(id -> id, id -> {
            Optional<User> u = userRepository.findById(id);
            return u.map(user -> user.getFirstName() + " " + user.getLastName()).orElse("N/A");
        }));

        List<Map<String, Object>> familleCroissance = new ArrayList<>();
        for (Family fam : allFamilies) {
            List<Soul> ames = soulsByFamily.getOrDefault(fam.getId(), List.of());
            long actifsFam = ames.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long enIntFam = ames.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();

            FamilyReport fr = currentWeekReportByFamily.get(fam.getId());
            BigDecimal presenceMoyenne = (fr != null && fr.getPresenceMoyenne() != null)
                    ? fr.getPresenceMoyenne() : BigDecimal.ZERO;

            String chefNom = fam.getChefFamilleId() != null
                    ? chefNames.getOrDefault(fam.getChefFamilleId(), "N/A") : "N/A";

            Map<String, Object> f = new LinkedHashMap<>();
            f.put("id", fam.getId());
            f.put("nom", fam.getNom());
            f.put("departementId", null);
            f.put("totalAmes", (long) ames.size());
            f.put("actifs", actifsFam);
            f.put("enIntegration", enIntFam);
            f.put("tauxPresence", presenceMoyenne);
            f.put("chefFamilleId", fam.getChefFamilleId());
            f.put("chefNom", chefNom);
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
        // Combine : niveau marqué manuellement par le pasteur (niveauRisque) +
        // taux de présence hebdomadaire sous 50%.
        List<Map<String, Object>> famillesRisque = new ArrayList<>();
        for (Family fam : allFamilies) {
            boolean niveauMarque = fam.getNiveauRisque() != null
                    && fam.getNiveauRisque() != com.discipolat.common.enums.NiveauRisque.NORMAL;
            List<FamilyReport> frs = familyReportRepository.findByFamilleIdAndSemaine(fam.getId(), currentWeek);
            Double tauxPresence = (!frs.isEmpty() && frs.get(0).getPresenceMoyenne() != null)
                    ? frs.get(0).getPresenceMoyenne().doubleValue() : null;
            if (niveauMarque || (tauxPresence != null && tauxPresence < 50.0)) {
                Map<String, Object> fr = new LinkedHashMap<>();
                fr.put("id", fam.getId());
                fr.put("nom", fam.getNom());
                fr.put("tauxPresence", tauxPresence != null ? tauxPresence : 0.0);
                fr.put("niveauRisque", fam.getNiveauRisque() != null
                        ? fam.getNiveauRisque().name() : "NORMAL");
                famillesRisque.add(fr);
            }
        }
        dashboard.put("famillesARisque", famillesRisque);

        // ==================== TRANSFERTS À TRAITER ====================
        // Demandes en attente de validation (EN_ATTENTE_VALIDATION ou
        // VALIDATION_PARTIELLE) — le Pasteur est le validateur final : il doit
        // voir immédiatement ce qui requiert son action.
        List<TransferRequest> transfertsEnValidation = transferRequestRepository.findByStatutIn(
                        List.of(TransferStatus.EN_ATTENTE_VALIDATION, TransferStatus.VALIDATION_PARTIELLE)).stream()
                .sorted(Comparator.comparing(TransferRequest::getDateSoumission,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        List<Map<String, Object>> transfertsList = new ArrayList<>();
        for (TransferRequest t : transfertsEnValidation) {
            Map<String, Object> tf = new LinkedHashMap<>();
            tf.put("id", t.getId());
            tf.put("type", t.getType().name());
            tf.put("statut", t.getStatut().name());
            tf.put("priorite", t.getPriorite().name());
            tf.put("dateSoumission", t.getDateSoumission());
            // Personne concernée (âme ou utilisateur) : nom résolu.
            if ("USER".equals(t.getPersonneType())) {
                tf.put("personneNom", userRepository.findById(t.getPersonneId())
                        .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null));
            } else {
                tf.put("personneNom", soulRepository.findById(t.getPersonneId())
                        .map(Soul::getNomComplet).orElse(null));
            }
            // Cible lisible : nouvelle affectation {type, id, nom}.
            Object cible = t.getNouvelleAffectation() != null ? t.getNouvelleAffectation().get("nom") : null;
            tf.put("cible", cible);
            transfertsList.add(tf);
        }
        dashboard.put("transfertsEnAttente", transfertsList);

        dashboard.put("semaine", currentWeek.toString());
        return dashboard;
    }

    // ========================================================================
    // PHASE 2: CHEF DE FAMILLE DASHBOARD
    // ========================================================================

    @Cacheable(value = "dashboardKpi", key = "T(java.lang.String).valueOf(@securityUtils.getCurrentUserId()) + '-chef-' + T(java.util.Optional).ofNullable(#familleIdInput).orElse('default')", unless = "#result == null")
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
        // IDOR FIX: Verify the current user has access to this family
        if (!securityUtils.isSuperUser() && !workspaceScope.canAccessFamily(resolvedFamilleId)) {
            throw new com.discipolat.common.exception.ForbiddenException("You do not have access to this family");
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
        infos.put("departementId", null); // Departments linked via soul_departments
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

    @Cacheable(value = "dashboardKpi", key = "T(java.lang.String).valueOf(@securityUtils.getCurrentUserId()) + '-resp-' + T(java.util.Optional).ofNullable(#deptIdInput).orElse('default')", unless = "#result == null")
    public Map<String, Object> getResponsableDashboard(UUID deptIdInput) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Map<String, Object> dashboard = new LinkedHashMap<>();
        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // ==================== DÉPARTEMENTS DU RESPONSABLE ====================
        List<Department> depts = departmentRepository.findByResponsableId(currentUserId);
        if (depts.isEmpty()) {
            dashboard.put("message", "Aucun département assigné");
            return dashboard;
        }

        // Si plusieurs départements : le responsable choisit le département à administrer
        List<Map<String, Object>> deptList = new ArrayList<>();
        Department selectedDept = null;
        for (Department dept : depts) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id", dept.getId());
            d.put("nom", dept.getNom());
            d.put("description", dept.getDescription());
            d.put("statut", dept.getStatut().name());
            deptList.add(d);
            if (deptIdInput != null && deptIdInput.equals(dept.getId())) {
                selectedDept = dept;
            }
        }
        // Par défaut : premier département
        if (selectedDept == null) {
            selectedDept = depts.get(0);
        }

        dashboard.put("departements", deptList);
        dashboard.put("selectedDeptId", selectedDept.getId());
        dashboard.put("selectedDeptNom", selectedDept.getNom());

        // ==================== MEMBRES DU DÉPARTEMENT SÉLECTIONNÉ ====================
        // Les membres sont liés au département via soul_departments (les âmes associées)
        List<Soul> allSouls = soulDepartmentRepository.findByDepartmentIdAndActifTrue(selectedDept.getId())
                .stream()
                .map(sd -> soulRepository.findById(sd.getSoulId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(s -> !s.isDeleted())
                .toList();

        long actifs = allSouls.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
        long enInt = allSouls.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();
        long veille = allSouls.stream().filter(s -> s.getStatut() == StatutAme.EN_VEILLE).count();
        long decroche = allSouls.stream().filter(s -> s.getStatut() == StatutAme.DECROCHE).count();

        // Nouveaux membres (intégrés dans les 30 derniers jours)
        LocalDate cutoff = LocalDate.now().minusDays(30);
        long nouveauxMembres = allSouls.stream()
                .filter(s -> s.getDateIntegration() != null && s.getDateIntegration().isAfter(cutoff))
                .count();

        // Anniversaires du mois
        int currentMonth = LocalDate.now().getMonthValue();
        List<Map<String, Object>> anniversaires = allSouls.stream()
                .filter(s -> s.getDateNaissance() != null && s.getDateNaissance().getMonthValue() == currentMonth)
                .map(s -> Map.<String, Object>of(
                        "id", s.getId(), "nom", s.getNomComplet(),
                        "dateNaissance", s.getDateNaissance().toString()))
                .toList();

        // Membres (avec détail)
        List<Map<String, Object>> members = allSouls.stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", s.getId());
                    m.put("nom", s.getNomComplet());
                    m.put("statut", s.getStatut().name());
                    m.put("dateIntegration", s.getDateIntegration() != null ? s.getDateIntegration().toString() : null);
                    m.put("familleId", s.getFamilleId());
                    m.put("familleNom", s.getFamilleId() != null
                            ? familyRepository.findById(s.getFamilleId()).map(Family::getNom).orElse(null)
                            : null);
                    m.put("faiseurId", s.getFaiseurId());
                    m.put("faiseurNom", s.getFaiseurId() != null
                            ? userRepository.findById(s.getFaiseurId()).map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null)
                            : null);
                    m.put("chefFamilleId", s.getFamilleId() != null
                            ? familyRepository.findById(s.getFamilleId()).map(Family::getChefFamilleId).orElse(null)
                            : null);
                    return m;
                })
                .toList();

        // Présences (via les rapports des faiseurs des membres)
        int presents = 0, totalPossibles = 0;
        for (Soul s : allSouls) {
            List<MakerReport> reports = makerReportRepository.findByAmeIdAndSemaine(s.getId(), currentWeek);
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

        // Rapports reçus
        long rapportsSoumis = 0, rapportsAttendus = 0;
        for (Soul s : allSouls) {
            List<MakerReport> reports = makerReportRepository.findByAmeIdAndSemaine(s.getId(), currentWeek);
            rapportsSoumis += reports.stream().filter(MakerReport::isSoumis).count();
            rapportsAttendus += reports.size();
        }

        // ==================== ORGANISATION & GESTION (Department Management) ====================
        UUID deptId = selectedDept.getId();
        long equipesActives = departmentTeamRepository.countByDepartmentIdAndStatut(deptId, DepartmentTeam.TeamStatus.ACTIVE);
        long postesActifs = departmentPositionRepository.countByDepartmentIdAndStatut(deptId, DepartmentPosition.PositionStatus.ACTIVE);
        List<DepartmentTask> deptTasks = departmentTaskRepository.findByDepartmentIdOrderByEcheanceAsc(deptId);
        long tachesOuvertes = deptTasks.stream().filter(DepartmentTask::isOpen).count();
        long tachesEnRetard = deptTasks.stream().filter(DepartmentTask::isOverdue).count();
        long tachesTerminees = deptTasks.stream()
                .filter(t -> t.getStatut() == DepartmentTask.TaskStatus.TERMINEE || t.getStatut() == DepartmentTask.TaskStatus.VALIDEE)
                .count();
        long membresAffectes = departmentAssignmentRepository.findByDepartmentIdAndActifTrue(deptId).stream()
                .map(DepartmentAssignment::getMemberId).distinct().count();

        Map<String, Object> deptDetail = new LinkedHashMap<>();
        deptDetail.put("totalMembres", (long) allSouls.size());
        deptDetail.put("nouveauxMembres", nouveauxMembres);
        deptDetail.put("actifs", actifs);
        deptDetail.put("enIntegration", enInt);
        deptDetail.put("enVeille", veille);
        deptDetail.put("decroches", decroche);
        deptDetail.put("tauxPresence", tauxPresence);
        deptDetail.put("presents", presents);
        deptDetail.put("absents", totalPossibles - presents);
        deptDetail.put("rapportsSoumis", rapportsSoumis);
        deptDetail.put("rapportsAttendus", rapportsAttendus);
        deptDetail.put("anniversaires", anniversaires);
        deptDetail.put("membres", members);
        deptDetail.put("equipesActives", equipesActives);
        deptDetail.put("postesActifs", postesActifs);
        deptDetail.put("tachesOuvertes", tachesOuvertes);
        deptDetail.put("tachesEnRetard", tachesEnRetard);
        deptDetail.put("tachesTerminees", tachesTerminees);
        deptDetail.put("membresAffectes", membresAffectes);

        // ==================== VUE RESPONSABLE ENRICHIE ====================

        // Nouveaux membres récents (30 jours), les plus récents d'abord
        List<Map<String, Object>> nouveauxRecents = allSouls.stream()
                .filter(s -> s.getDateIntegration() != null && s.getDateIntegration().isAfter(cutoff))
                .sorted(Comparator.comparing(Soul::getDateIntegration).reversed())
                .limit(5)
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", s.getId());
                    m.put("nom", s.getNomComplet());
                    m.put("statut", s.getStatut().name());
                    m.put("dateIntegration", s.getDateIntegration().toString());
                    m.put("dateAffectation", soulDepartmentRepository.findBySoulIdAndDepartmentId(s.getId(), deptId)
                            .stream().findFirst().map(sd -> sd.getDateAffectation() != null ? sd.getDateAffectation().toLocalDate().toString() : null)
                            .orElse(null));
                    m.put("origine", soulDepartmentRepository.findBySoulIdAndDepartmentId(s.getId(), deptId)
                            .stream().findFirst().map(SoulDepartment::getOrigine).orElse(null));
                    return m;
                })
                .toList();
        deptDetail.put("nouveauxRecents", nouveauxRecents);

        // Présence de la semaine : fiches de présence (saisie responsable)
        List<UUID> soulIds = allSouls.stream().map(Soul::getId).toList();
        List<MemberPresence> weekRecords = soulIds.isEmpty() ? List.of()
                : memberPresenceRepository.findBySoulIdInOrderBySemaineDesc(soulIds).stream()
                        .filter(r -> r.getSemaine().equals(currentWeek))
                        .toList();
        Set<UUID> pointedIds = weekRecords.stream().map(MemberPresence::getSoulId).collect(Collectors.toSet());
        long absentsSemaine = weekRecords.stream().filter(r -> !Boolean.TRUE.equals(r.getPresent())).count();
        long nonPointes = allSouls.stream().map(Soul::getId).filter(id -> !pointedIds.contains(id)).count();
        deptDetail.put("absentsSemaine", absentsSemaine);
        deptDetail.put("nonPointes", nonPointes);
        deptDetail.put("pointesSemaine", (long) weekRecords.size());

        // Membres à suivre : sans rapport soumis cette semaine
        List<Map<String, Object>> membresSuivi = allSouls.stream()
                .filter(s -> makerReportRepository.findByAmeIdAndSemaine(s.getId(), currentWeek).stream()
                        .noneMatch(MakerReport::isSoumis))
                .limit(8)
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", s.getId());
                    m.put("nom", s.getNomComplet());
                    m.put("statut", s.getStatut().name());
                    return m;
                })
                .toList();
        deptDetail.put("membresSuivi", membresSuivi);

        // Transferts en attente concernant les membres du département
        // (requête count en base — ne charge plus toute la table des transferts).
        List<UUID> deptSoulIds = allSouls.stream().map(Soul::getId).toList();
        long transfertsEnAttente = deptSoulIds.isEmpty() ? 0L
                : transferRequestRepository.countByStatutInAndPersonneIdIn(
                        List.of(TransferStatus.EN_ATTENTE_VALIDATION, TransferStatus.VALIDATION_PARTIELLE),
                        deptSoulIds);
        deptDetail.put("transfertsEnAttente", transfertsEnAttente);

        // Événements à venir (30 jours) liés au département
        LocalDateTime now = LocalDateTime.now();
        UUID responsableId = selectedDept.getResponsableId();
        List<Event> upcoming = eventRepository.findByDateDebutBetweenAndDeletedFalse(now, now.plusDays(30));
        Set<UUID> memberUserIds = allSouls.stream().map(Soul::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Map<String, Object>> evenementsAvenir = upcoming.stream()
                .filter(ev -> (responsableId != null && responsableId.equals(ev.getOrganisateurId()))
                        || eventRegistrationRepository.findByEventId(ev.getId()).stream()
                                .anyMatch(r -> memberUserIds.contains(r.getUtilisateurId())))
                .map(ev -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", ev.getId());
                    m.put("titre", ev.getTitre());
                    m.put("lieu", ev.getLieu());
                    m.put("dateDebut", ev.getDateDebut() != null ? ev.getDateDebut().toString() : null);
                    m.put("statut", ev.getStatut());
                    return m;
                })
                .limit(8)
                .toList();
        deptDetail.put("evenementsAvenir", evenementsAvenir);

        // Alertes actives du département (manuel + intelligentes) — deux
        // requêtes ciblées (par département / par âmes) au lieu d'un findAll.
        Map<UUID, Alert> alertsById = new LinkedHashMap<>();
        alertRepository.findByDepartmentIdAndStatut(deptId, StatutAlerte.ACTIVE)
                .forEach(a -> alertsById.put(a.getId(), a));
        if (!deptSoulIds.isEmpty()) {
            alertRepository.findByStatutAndAmeIdIn(StatutAlerte.ACTIVE, deptSoulIds)
                    .forEach(a -> alertsById.putIfAbsent(a.getId(), a));
        }
        List<Alert> activeAlerts = alertsById.values().stream()
                .sorted(Comparator.comparing(Alert::getDateDeclenchement).reversed())
                .toList();
        List<Map<String, Object>> alertesList = activeAlerts.stream().limit(5).map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("typeAlerte", a.getTypeAlerte());
            m.put("titre", a.getTitre());
            m.put("message", a.getMessage());
            m.put("priorite", a.getPriorite());
            m.put("dateDeclenchement", a.getDateDeclenchement().toString());
            return m;
        }).toList();
        deptDetail.put("alertes", alertesList);
        dashboard.put("departement", deptDetail);

        // ==================== STATISTIQUES GLOBALES ====================
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDepartements", (long) depts.size());
        stats.put("totalMembres", (long) allSouls.size());
        stats.put("totalActifs", actifs);
        stats.put("nouveauxMembres", nouveauxMembres);
        stats.put("tauxPresence", tauxPresence);
        stats.put("rapportsSoumis", rapportsSoumis);
        stats.put("rapportsAttendus", rapportsAttendus);
        stats.put("equipesActives", equipesActives);
        stats.put("postesActifs", postesActifs);
        stats.put("tachesOuvertes", tachesOuvertes);
        stats.put("tachesEnRetard", tachesEnRetard);
        stats.put("tachesTerminees", tachesTerminees);
        stats.put("membresAffectes", membresAffectes);
        stats.put("absentsSemaine", absentsSemaine);
        stats.put("nonPointes", nonPointes);
        stats.put("transfertsEnAttente", transfertsEnAttente);
        stats.put("evenementsAvenir", (long) evenementsAvenir.size());
        stats.put("alertesActives", (long) activeAlerts.size());
        stats.put("membresSuivi", (long) membresSuivi.size());
        double tauxCompletion = rapportsAttendus > 0
                ? Math.round((double) rapportsSoumis / rapportsAttendus * 1000.0) / 10.0
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

        // ==================== PRÉSENCE (agrégat des rapports de la semaine) ====================
        long presentsTotal = 0, cultesTotal = 0;
        for (Map<String, Object> d : disciples) {
            Object p = d.get("presences");
            Object t = d.get("totalCultes");
            if (p instanceof Number && t instanceof Number) {
                presentsTotal += ((Number) p).longValue();
                cultesTotal += ((Number) t).longValue();
            }
        }
        Map<String, Object> presence = new LinkedHashMap<>();
        presence.put("presents", presentsTotal);
        presence.put("cultes", cultesTotal);
        presence.put("taux", cultesTotal > 0 ? Math.round(presentsTotal * 1000.0 / cultesTotal) / 10.0 : 0.0);
        crm.put("presence", presence);

        // ==================== VISITES (prochaines & récentes de mes disciples) ====================
        List<Map<String, Object>> visites = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Soul soul : mesAmes) {
            List<Visit> soulVisits = visitRepository.findBySoulIdOrderByDatePrevueDesc(soul.getId());
            for (Visit v : soulVisits) {
                if (v.getStatut() == Visit.StatutVisite.PLANIFIEE || v.getStatut() == Visit.StatutVisite.REPORTEE) {
                    Map<String, Object> vm = new LinkedHashMap<>();
                    vm.put("id", v.getId());
                    vm.put("soulId", soul.getId());
                    vm.put("soulNom", soul.getNomComplet());
                    vm.put("datePrevue", v.getDatePrevue() != null ? v.getDatePrevue().toString() : null);
                    vm.put("statut", v.getStatut().name());
                    vm.put("motif", v.getMotif());
                    vm.put("objectif", v.getObjectif());
                    visites.add(vm);
                }
            }
        }
        visites.sort(Comparator.comparing(m -> (String) m.get("datePrevue")));
        crm.put("visites", visites);

        // ==================== PRIÈRES (liées à mes disciples) ====================
        List<Map<String, Object>> prieres = new ArrayList<>();
        for (Soul soul : mesAmes) {
            List<Prayer> soulPrayers = prayerRepository.findByAmeIdAndDeletedFalse(soul.getId());
            for (Prayer p : soulPrayers) {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("id", p.getId());
                pm.put("soulId", soul.getId());
                pm.put("soulNom", soul.getNomComplet());
                pm.put("titre", p.getTitre());
                pm.put("categorie", p.getCategorie());
                pm.put("priorite", p.getPriorite());
                pm.put("statut", p.getStatut());
                pm.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
                prieres.add(pm);
            }
        }
        prieres.sort(Comparator.comparing(m -> String.valueOf(m.get("createdAt")), Comparator.reverseOrder()));
        crm.put("prieres", prieres);
        long prieresEnCours = prieres.stream().filter(p -> "EN_COURS".equals(p.get("statut"))).count();

        // ==================== ÉVÉNEMENTS À VENIR (familles de mes disciples) ====================
        Set<UUID> familleIds = mesAmes.stream()
                .map(Soul::getFamilleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Map<String, Object>> evenements = new ArrayList<>();
        for (UUID familleId : familleIds) {
            List<Event> familleEvents = eventRepository.findAllByFamilleIdAndDeletedFalse(familleId);
            for (Event ev : familleEvents) {
                if (ev.getDateDebut() == null || !ev.getDateDebut().isAfter(LocalDateTime.now())) continue;
                Map<String, Object> em = new LinkedHashMap<>();
                em.put("id", ev.getId());
                em.put("titre", ev.getTitre());
                em.put("typeEvenement", ev.getTypeEvenement());
                em.put("lieu", ev.getLieu());
                em.put("dateDebut", ev.getDateDebut().toString());
                em.put("dateFin", ev.getDateFin() != null ? ev.getDateFin().toString() : null);
                em.put("statut", ev.getStatut());
                evenements.add(em);
            }
        }
        evenements.sort(Comparator.comparing(m -> (String) m.get("dateDebut")));
        crm.put("evenements", evenements);

        // ==================== PROGRESSION (répartition par niveau de croissance) ====================
        Map<String, Long> progression = new LinkedHashMap<>();
        for (Map<String, Object> d : disciples) {
            String niveau = String.valueOf(d.getOrDefault("niveauCroissance", 0));
            progression.merge(niveau, 1L, Long::sum);
        }
        crm.put("progression", progression);

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

            // Confidentiel : ne compter que les membres des départements du responsable
            // (et non toutes les familles de l'église).
            List<UUID> soulIds = deptIds.isEmpty() ? List.of()
                    : soulDepartmentRepository.findByDepartmentIdIn(deptIds).stream()
                            .filter(SoulDepartment::isActif)
                            .map(SoulDepartment::getSoulId)
                            .distinct()
                            .toList();
            List<Soul> allSouls = soulIds.isEmpty() ? List.of() : soulRepository.findAllById(soulIds);
            long actifs = allSouls.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long enIntegration = allSouls.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();

            Set<UUID> faiseurIds = allSouls.stream().map(Soul::getFaiseurId).collect(Collectors.toSet());
            // Chefs des familles auxquelles appartiennent les membres du département
            Set<UUID> chefIds = allSouls.stream()
                    .map(Soul::getFamilleId)
                    .filter(Objects::nonNull)
                    .map(fid -> familyRepository.findById(fid).map(Family::getChefFamilleId).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            faiseurIds.addAll(chefIds);

            metrics.put("departements", depts.stream().map(d -> Map.<String, Object>of("id", d.getId(), "nom", d.getNom())).toList());
            metrics.put("totalDepartements", (long) depts.size());
            metrics.put("totalFamilles", (long) allSouls.stream().map(Soul::getFamilleId).filter(Objects::nonNull).distinct().count());
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

    // ========================================================================
    // PASTEUR: KPIs ENRICHIS (health score, retention, growth, workload)
    // ========================================================================

    public Map<String, Object> getPasteurKpis() {
        Map<String, Object> kpis = new LinkedHashMap<>();
        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthAgo = LocalDate.now().minusDays(30);
        LocalDate twoMonthsAgo = LocalDate.now().minusDays(60);

        // ==================== HEALTH SCORE (composite) ====================
        // Score sur 100 basé sur : présence (40%), rapports (30%), croissance (20%), alertes (10%)
        long totalSouls = soulRepository.count();
        long actifs = soulRepository.countByStatut(StatutAme.ACTIF);
        long enIntegration = soulRepository.countByStatut(StatutAme.EN_INTEGRATION);
        long enVeille = soulRepository.countByStatut(StatutAme.EN_VEILLE);
        long decroches = soulRepository.countByStatut(StatutAme.DECROCHE);
        long alertesActives = alertRepository.countByStatut(StatutAlerte.ACTIVE);
        long totalFamilles = familyRepository.count();
        long totalFaiseurs = userRepository.countByRole(UserRole.FAISEUR);

        // Présence globale
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
        double tauxPresence = totalPossibles > 0 ? (double) presents / totalPossibles * 100.0 : 0.0;

        // Rapports soumis
        int rapportsSoumis = 0, rapportsEnAttente = 0;
        for (MakerReport r : weekReports) {
            if (r.isSoumis()) rapportsSoumis++; else rapportsEnAttente++;
        }
        double tauxRapports = (rapportsSoumis + rapportsEnAttente) > 0
                ? (double) rapportsSoumis / (rapportsSoumis + rapportsEnAttente) * 100.0 : 0.0;

        // Croissance mensuelle
        long nouveauxMois = soulRepository.countByDateIntegrationBetween(monthAgo, LocalDate.now());
        long nouveauxMoisPrecedent = soulRepository.countByDateIntegrationBetween(twoMonthsAgo, monthAgo);
        long croissanceBrute = nouveauxMois;
        double tauxCroissance = totalSouls > 0 ? (double) nouveauxMois / totalSouls * 100.0 : 0.0;

        // Fidélisation
        double tauxFidelisation = totalSouls > 0 ? (double) actifs / totalSouls * 100.0 : 0.0;

        // Score composite santé (0-100)
        double scorePresence = Math.min(tauxPresence, 100) * 0.40;
        double scoreRapports = Math.min(tauxRapports, 100) * 0.30;
        double scoreCroissance = Math.min(tauxCroissance * 10, 100) * 0.20; // Amplifié pour que 10% = 100
        double scoreAlertes = Math.max(0, 100 - alertesActives * 10) * 0.10;
        int healthScore = (int) Math.round(scorePresence + scoreRapports + scoreCroissance + scoreAlertes);

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("score", Math.min(healthScore, 100));
        health.put("tauxPresence", Math.round(tauxPresence * 10.0) / 10.0);
        health.put("tauxRapports", Math.round(tauxRapports * 10.0) / 10.0);
        health.put("tauxFidelisation", Math.round(tauxFidelisation * 10.0) / 10.0);
        health.put("tauxCroissance", Math.round(tauxCroissance * 10.0) / 10.0);
        health.put("nouveauxMois", nouveauxMois);
        health.put("croissanceNette", croissanceBrute);
        kpis.put("health", health);

        // ==================== WORKLOAD PER FAISEUR ====================
        List<User> tousFaiseurs = userRepository.findByRolesContaining(UserRole.FAISEUR);
        List<Map<String, Object>> workload = new ArrayList<>();
        for (User faiseur : tousFaiseurs) {
            List<Soul> amesF = soulRepository.findAllByFaiseurId(faiseur.getId());
            long actifsF = amesF.stream().filter(s -> s.getStatut() == StatutAme.ACTIF && !s.isDeleted()).count();
            long totalF = amesF.stream().filter(s -> !s.isDeleted()).count();
            List<MakerReport> rapportsF = makerReportRepository.findByFaiseurIdAndSemaine(faiseur.getId(), currentWeek);
            boolean aRapporte = rapportsF.stream().anyMatch(MakerReport::isSoumis);

            Map<String, Object> w = new LinkedHashMap<>();
            w.put("id", faiseur.getId());
            w.put("nom", faiseur.getFirstName() + " " + faiseur.getLastName());
            w.put("totalAmes", totalF);
            w.put("actifs", actifsF);
            w.put("rapportSoumis", aRapporte);
            // Charge de travail : ratio actifs/total — plus le ratio est élevé, plus le faiseur est chargé
            double charge = totalF > 0 ? (double) actifsF / totalF * 100.0 : 0.0;
            w.put("charge", Math.round(charge * 10.0) / 10.0);
            workload.add(w);
        }
        // Triés par charge décroissante
        workload.sort((a, b) -> Double.compare((double) b.get("charge"), (double) a.get("charge")));
        kpis.put("workload", workload);

        // ==================== ÉVÉNEMENTS À VENIR ====================
        List<Event> upcomingEvents = eventRepository
                .findTop10ByDeletedFalseAndDateDebutAfterOrderByDateDebutAsc(LocalDateTime.now());
        List<Map<String, Object>> eventsList = new ArrayList<>();
        for (Event event : upcomingEvents) {
            long inscrits = eventRegistrationRepository.findByEventId(event.getId()).size();
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("id", event.getId());
            ev.put("titre", event.getTitre());
            ev.put("dateDebut", event.getDateDebut());
            ev.put("type", event.getTypeEvenement());
            ev.put("lieu", event.getLieu());
            ev.put("inscrits", inscrits);
            eventsList.add(ev);
        }
        kpis.put("upcomingEvents", eventsList);

        // ==================== RAPPORTS EN RETARD ====================
        List<Map<String, Object>> overdueReports = new ArrayList<>();
        for (User faiseur : tousFaiseurs) {
            List<MakerReport> rapportsF = makerReportRepository.findByFaiseurIdAndSemaine(faiseur.getId(), currentWeek);
            boolean aRapporte = rapportsF.stream().anyMatch(MakerReport::isSoumis);
            if (!aRapporte) {
                List<Soul> amesF = soulRepository.findAllByFaiseurId(faiseur.getId());
                long nbAmes = amesF.stream().filter(s -> !s.isDeleted()).count();
                Map<String, Object> or = new LinkedHashMap<>();
                or.put("faiseurId", faiseur.getId());
                or.put("faiseurNom", faiseur.getFirstName() + " " + faiseur.getLastName());
                or.put("nbAmes", nbAmes);
                or.put("semaine", currentWeek.toString());
                overdueReports.add(or);
            }
        }
        kpis.put("overdueReports", overdueReports);
        kpis.put("overdueReportsCount", overdueReports.size());

        // ==================== RÉSUMÉ RAPIDE ====================
        Map<String, Object> resume = new LinkedHashMap<>();
        resume.put("totalAmes", totalSouls);
        resume.put("actifs", actifs);
        resume.put("enIntegration", enIntegration);
        resume.put("enVeille", enVeille);
        resume.put("decroches", decroches);
        resume.put("totalFamilles", totalFamilles);
        resume.put("totalFaiseurs", totalFaiseurs);
        resume.put("totalDepartements", departmentRepository.count());
        resume.put("alertesActives", alertesActives);
        kpis.put("resume", resume);

        return kpis;
    }
}
