package com.discipolat.modules.departments.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.StatutEntite;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.reports.domain.FamilyReport;
import com.discipolat.modules.reports.domain.FamilyReportRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserDepartmentRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final FamilyRepository familyRepository;
    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final MakerReportRepository makerReportRepository;
    private final FamilyReportRepository familyReportRepository;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;
    private final com.discipolat.modules.souls.domain.SoulDepartmentRepository soulDepartmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                             FamilyRepository familyRepository,
                             SoulRepository soulRepository,
                             UserRepository userRepository,
                             UserDepartmentRepository userDepartmentRepository,
                             MakerReportRepository makerReportRepository,
                             FamilyReportRepository familyReportRepository,
                             SecurityUtils securityUtils,
                             PasswordEncoder passwordEncoder,
                             com.discipolat.modules.souls.domain.SoulDepartmentRepository soulDepartmentRepository) {
        this.departmentRepository = departmentRepository;
        this.familyRepository = familyRepository;
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.userDepartmentRepository = userDepartmentRepository;
        this.makerReportRepository = makerReportRepository;
        this.familyReportRepository = familyReportRepository;
        this.securityUtils = securityUtils;
        this.passwordEncoder = passwordEncoder;
        this.soulDepartmentRepository = soulDepartmentRepository;
    }

    public Department create(Department department) {
        department.setStatut(StatutEntite.ACTIVE);
        return departmentRepository.save(department);
    }

    /**
     * Création d'un département avec 2 cas :
     * Cas 1 : Sélectionner un responsable existant (responsableId requis)
     * Cas 2 : Créer immédiatement un nouveau responsable.
     */
    public Department create(com.discipolat.modules.departments.api.CreateDepartmentRequest request) {
        UUID responsableId;
        if (request.shouldCreateNewResponsable()) {
            responsableId = createNewResponsable(request);
        } else if (request.responsableId() != null) {
            responsableId = request.responsableId();
        } else {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "Vous devez sélectionner un responsable existant ou créer un nouveau responsable.",
                    "NO_RESPONSABLE_SELECTED");
        }

        Department department = Department.builder()
                .nom(request.nom())
                .description(request.description())
                .responsableId(responsableId)
                .statut(StatutEntite.ACTIVE)
                .build();
        department = departmentRepository.save(department);

        // Lier le responsable au département (table user_departments)
        userDepartmentRepository.save(com.discipolat.modules.users.domain.UserDepartment.builder()
                .userId(responsableId)
                .departmentId(department.getId())
                .roleDansDept("RESPONSABLE")
                .build());

        return department;
    }

    private UUID createNewResponsable(com.discipolat.modules.departments.api.CreateDepartmentRequest request) {
        if (userRepository.findByEmail(request.newRespEmail()).isPresent()) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "Un compte avec cet email existe déjà: " + request.newRespEmail(),
                    "DUPLICATE_EMAIL");
        }

        String tempPassword = java.util.UUID.randomUUID().toString().substring(0, 8);
        User newResp = User.builder()
                .email(request.newRespEmail())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .firstName(request.newRespFirstName())
                .lastName(request.newRespLastName())
                .phone(request.newRespPhone())
                .role(com.discipolat.common.domain.UserRole.RESPONSABLE)
                .roles(new java.util.HashSet<>(java.util.Set.of(com.discipolat.common.domain.UserRole.RESPONSABLE)))
                .statut(com.discipolat.modules.users.domain.UserStatus.ACTIVE)
                .build();
        newResp = userRepository.save(newResp);
        return newResp.getId();
    }

    @Transactional(readOnly = true)
    public Department findById(UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department", id));
        assertAccessible(department);
        return department;
    }

    /** Un responsable ne peut consulter que ses propres départements. */
    private void assertAccessible(Department department) {
        if (securityUtils.isSuperUser()) return;
        if (securityUtils.hasActiveRole("RESPONSABLE")) {
            boolean owned = departmentRepository.findByResponsableId(securityUtils.getCurrentUserId())
                    .stream().anyMatch(d -> d.getId().equals(department.getId()));
            if (!owned) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Accès refusé : ce département ne fait pas partie de votre espace métier");
            }
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "Accès refusé : les départements ne font pas partie de votre espace métier");
    }

    @Transactional(readOnly = true)
    public Page<Department> findAll(Pageable pageable) {
        if (securityUtils.isSuperUser()) return departmentRepository.findAll(pageable);
        if (securityUtils.hasActiveRole("RESPONSABLE")) {
            List<UUID> deptIds = departmentRepository.findByResponsableId(securityUtils.getCurrentUserId())
                    .stream().map(Department::getId).toList();
            if (deptIds.isEmpty()) return Page.empty(pageable);
            return departmentRepository.findAllByIdIn(deptIds, pageable);
        }
        return Page.empty(pageable);
    }

    public Department update(Department updated) {
        Department existing = findById(updated.getId());
        existing.setNom(updated.getNom());
        existing.setDescription(updated.getDescription());
        existing.setResponsableId(updated.getResponsableId());
        return departmentRepository.save(existing);
    }

    public void delete(UUID id) {
        Department department = findById(id);
        department.setStatut(StatutEntite.ARCHIVED);
        departmentRepository.save(department);
    }

    @Transactional(readOnly = true)
    public List<Department> findByResponsableId(UUID responsableId) {
        return departmentRepository.findByResponsableId(responsableId);
    }

    // ========================================================================
    // Scoping des données par département (deptId)
    // ========================================================================

    /** Ids des âmes rattachées au département (soul_departments actifs). */
    private List<UUID> departmentSoulIds(UUID deptId) {
        return soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId)
                .stream().map(com.discipolat.modules.souls.domain.SoulDepartment::getSoulId).toList();
    }

    /** Âmes du département (hors soft-deleted). */
    private List<Soul> departmentSouls(UUID deptId) {
        List<UUID> ids = departmentSoulIds(deptId);
        if (ids.isEmpty()) return List.of();
        return soulRepository.findAllById(ids).stream()
                .filter(s -> !s.isDeleted())
                .toList();
    }

    /** Familles des membres du département (distinctes). */
    private List<Family> departmentFamilies(UUID deptId) {
        return departmentSouls(deptId).stream()
                .map(Soul::getFamilleId)
                .filter(Objects::nonNull)
                .distinct()
                .map(familyId -> familyRepository.findById(familyId).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDetail(UUID id) {
        Department dept = findById(id);

        List<Family> families = departmentFamilies(id);
        List<Map<String, Object>> familyDetails = families.stream().map(family -> {
            Map<String, Object> fd = new LinkedHashMap<>();
            fd.put("id", family.getId());
            fd.put("nom", family.getNom());
            fd.put("chefFamilleId", family.getChefFamilleId());
            fd.put("statut", family.getStatut());
            fd.put("dateCreation", family.getDateCreation());

            Optional<User> chef = userRepository.findById(family.getChefFamilleId());
            fd.put("chefNom", chef.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("N/A"));

            long totalMembres = soulRepository.countByFamilleId(family.getId());
            long actifs = soulRepository.countByFamilleIdAndStatut(family.getId(), StatutAme.ACTIF);
            fd.put("totalMembres", totalMembres);
            fd.put("membresActifs", actifs);
            return fd;
        }).toList();

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", dept.getId());
        detail.put("nom", dept.getNom());
        detail.put("description", dept.getDescription());
        detail.put("responsableId", dept.getResponsableId());

        Optional<User> responsable = userRepository.findById(dept.getResponsableId());
        detail.put("responsableNom", responsable.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("N/A"));
        detail.put("responsableEmail", responsable.map(User::getEmail).orElse(""));
        detail.put("statut", dept.getStatut());
        detail.put("createdAt", dept.getCreatedAt());
        detail.put("updatedAt", dept.getUpdatedAt());

        detail.put("familles", familyDetails);
        detail.put("totalFamilles", families.size());

        long totalMembresDepartement = families.stream()
                .mapToLong(f -> soulRepository.countByFamilleId(f.getId()))
                .sum();
        detail.put("totalMembres", totalMembresDepartement);

        return detail;
    }

    // ======================== DEPARTMENT KPIs ========================

    @Transactional(readOnly = true)
    public Map<String, Object> getDepartmentKpi(UUID deptId) {
        findById(deptId);
        List<Family> families = departmentFamilies(deptId);
        List<UUID> familyIds = families.stream().map(Family::getId).toList();
        List<Soul> allSouls = departmentSouls(deptId);

        Set<UUID> faiseurIds = allSouls.stream().map(Soul::getFaiseurId).collect(Collectors.toSet());
        Set<UUID> chefsIds = families.stream().map(Family::getChefFamilleId).collect(Collectors.toSet());
        faiseurIds.addAll(chefsIds);

        long actifs = allSouls.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
        long enIntegration = allSouls.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();
        long enVeille = allSouls.stream().filter(s -> s.getStatut() == StatutAme.EN_VEILLE).count();
        long decroches = allSouls.stream().filter(s -> s.getStatut() == StatutAme.DECROCHE).count();

        long nouveauxConvertis = allSouls.stream().filter(s -> "NOUVEAU_CONVERTI".equals(s.getEtatSpirituel())).count();

        // Maker report stats
        LocalDate currentWeek = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        long soumisCetteSemaine = 0;
        long totalAttendus = 0;
        if (!faiseurIds.isEmpty()) {
            for (UUID fid : faiseurIds) {
                List<MakerReport> reports = makerReportRepository.findByFaiseurIdAndSemaine(fid, currentWeek);
                soumisCetteSemaine += reports.stream().filter(MakerReport::isSoumis).count();
                totalAttendus += soulRepository.findAllByFaiseurId(fid).stream()
                        .filter(s -> !s.isDeleted() && s.getStatut() != StatutAme.DECROCHE).count();
            }
        }

        BigDecimal tauxSoumission = totalAttendus > 0
                ? BigDecimal.valueOf(soumisCetteSemaine).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalAttendus), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int totalPresents = 0;
        int totalPresencesPossible = 0;
        for (UUID fid : faiseurIds) {
            List<MakerReport> reports = makerReportRepository.findByFaiseurIdAndSemaine(fid, currentWeek);
            for (MakerReport r : reports) {
                if (r.getPresencesParCulte() != null) {
                    for (Boolean p : r.getPresencesParCulte().values()) {
                        totalPresencesPossible++;
                        if (p) totalPresents++;
                    }
                }
            }
        }

        BigDecimal tauxPresence = totalPresencesPossible > 0
                ? BigDecimal.valueOf(totalPresents).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalPresencesPossible), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Family reports
        long familyReportsSoumis = 0;
        for (UUID famId : familyIds) {
            List<FamilyReport> frs = familyReportRepository.findByFamilleIdAndSemaine(famId, currentWeek);
            familyReportsSoumis += frs.stream().filter(fr -> !"BROUILLON".equals(fr.getStatutValidation().name())).count();
        }

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("totalFamilles", (long) families.size());
        kpi.put("totalMembres", (long) allSouls.size());
        kpi.put("membresActifs", actifs);
        kpi.put("membresEnIntegration", enIntegration);
        kpi.put("membresEnVeille", enVeille);
        kpi.put("membresDecroches", decroches);
        kpi.put("nouveauxConvertis", nouveauxConvertis);
        kpi.put("totalFaiseurs", (long) faiseurIds.size());
        kpi.put("rapportsSoumisSemaine", soumisCetteSemaine);
        kpi.put("rapportsAttendusSemaine", totalAttendus);
        kpi.put("tauxSoumission", tauxSoumission);
        kpi.put("tauxPresence", tauxPresence);
        kpi.put("totalPresents", totalPresents);
        kpi.put("familyReportsSoumis", familyReportsSoumis);
        kpi.put("semaine", currentWeek.toString());

        return kpi;
    }

    // ======================== DEPARTMENT MEMBERS ========================

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getDepartmentMembers(UUID deptId, Pageable pageable) {
        findById(deptId);
        List<Family> families = departmentFamilies(deptId);
        List<UUID> familyIds = families.stream().map(Family::getId).toList();
        Map<UUID, String> familyNames = families.stream().collect(Collectors.toMap(Family::getId, Family::getNom));

        List<UUID> soulIds = departmentSoulIds(deptId);
        if (soulIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<Soul> soulsPage = soulRepository.findAllByIdIn(soulIds, pageable);

        return soulsPage.map(soul -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", soul.getId());
            m.put("nom", soul.getNomComplet());
            m.put("prenom", soul.getPrenom());
            m.put("email", soul.getEmail());
            m.put("telephone", soul.getTelephone());
            m.put("statut", soul.getStatut().name());
            m.put("typeDisciple", soul.getTypeDisciple().name());
            m.put("etatSpirituel", soul.getEtatSpirituel());
            m.put("familleId", soul.getFamilleId());
            m.put("familleNom", soul.getFamilleId() != null ? familyNames.get(soul.getFamilleId()) : null);
            m.put("faiseurId", soul.getFaiseurId());
            m.put("dateIntegration", soul.getDateIntegration() != null ? soul.getDateIntegration().toString() : null);
            m.put("dateDernierContact", soul.getDateDernierContact() != null ? soul.getDateDernierContact().toString() : null);

            Optional<User> faiseur = userRepository.findById(soul.getFaiseurId());
            m.put("faiseurNom", faiseur.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("N/A"));

            return m;
        });
    }

    // ======================== UNASSIGNED MEMBERS ========================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUnassignedMembers(UUID deptId) {
        findById(deptId);
        List<Family> families = departmentFamilies(deptId);
        List<UUID> familyIds = families.stream().map(Family::getId).toList();
        Set<UUID> faiseurIds = new HashSet<>();
        for (Family family : families) {
            faiseurIds.add(family.getChefFamilleId());
        }
        // Also get all faiseurs from souls in the department
        List<Soul> departmentSouls = departmentSouls(deptId);
        for (Soul soul : departmentSouls) {
            faiseurIds.add(soul.getFaiseurId());
        }

        if (faiseurIds.isEmpty()) return List.of();

        List<Soul> trackedSouls = soulRepository.findByFaiseurIdIn(new ArrayList<>(faiseurIds));

        return trackedSouls.stream()
                .filter(s -> !s.isDeleted())
                .filter(s -> s.getFamilleId() == null || !familyIds.contains(s.getFamilleId()))
                .map(soul -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", soul.getId());
                    m.put("nom", soul.getNomComplet());
                    m.put("prenom", soul.getPrenom());
                    m.put("email", soul.getEmail());
                    m.put("telephone", soul.getTelephone());
                    m.put("statut", soul.getStatut().name());
                    m.put("typeDisciple", soul.getTypeDisciple().name());
                    m.put("etatSpirituel", soul.getEtatSpirituel());
                    m.put("familleId", soul.getFamilleId());
                    m.put("faiseurId", soul.getFaiseurId());
                    m.put("dateIntegration", soul.getDateIntegration() != null ? soul.getDateIntegration().toString() : null);

                    Optional<User> faiseur = userRepository.findById(soul.getFaiseurId());
                    m.put("faiseurNom", faiseur.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("N/A"));

                    return m;
                })
                .toList();
    }

    // ======================== DEPARTMENT REPORT ========================

    @Transactional(readOnly = true)
    public Map<String, Object> getDepartmentReport(UUID deptId, LocalDate semaine) {
        findById(deptId);
        List<Family> families = departmentFamilies(deptId);
        List<UUID> familyIds = families.stream().map(Family::getId).toList();

        int totalPresents = 0;
        int totalAbsents = 0;
        int totalSorties = 0;
        int totalMaintenus = 0;
        long familyReportsSoumis = 0;
        Map<String, Object> statsParFamille = new LinkedHashMap<>();

        for (Family family : families) {
            List<FamilyReport> frs = familyReportRepository.findByFamilleIdAndSemaine(family.getId(), semaine);
            boolean soumis = frs.stream().anyMatch(fr -> !"BROUILLON".equals(fr.getStatutValidation().name()));
            if (soumis) familyReportsSoumis++;

            FamilyReport latest = frs.isEmpty() ? null : frs.get(0);
            Map<String, Object> familleStats = new LinkedHashMap<>();
            familleStats.put("familleNom", family.getNom());
            familleStats.put("familleId", family.getId());
            if (latest != null) {
                totalPresents += latest.getTotalPresents() != null ? latest.getTotalPresents() : 0;
                totalAbsents += latest.getTotalAbsents() != null ? latest.getTotalAbsents() : 0;
                totalSorties += latest.getTotalSorties() != null ? latest.getTotalSorties() : 0;
                totalMaintenus += latest.getTotalMaintenus() != null ? latest.getTotalMaintenus() : 0;
                familleStats.put("presenceMoyenne", latest.getPresenceMoyenne());
                familleStats.put("totalPresents", latest.getTotalPresents());
                familleStats.put("totalAbsents", latest.getTotalAbsents());
                familleStats.put("totalSorties", latest.getTotalSorties());
                familleStats.put("totalMaintenus", latest.getTotalMaintenus());
                familleStats.put("statutValidation", latest.getStatutValidation().name());
                familleStats.put("soumis", true);
            } else {
                familleStats.put("soumis", false);
            }
            statsParFamille.put(family.getId().toString(), familleStats);
        }

        int totalPresencesPossible = totalPresents + totalAbsents;
        BigDecimal presenceMoyenne = totalPresencesPossible > 0
                ? BigDecimal.valueOf(totalPresents).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalPresencesPossible), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("departementId", deptId);
        report.put("semaine", semaine.toString());
        report.put("totalFamilles", (long) families.size());
        report.put("familyReportsSoumis", familyReportsSoumis);
        report.put("totalPresents", totalPresents);
        report.put("totalAbsents", totalAbsents);
        report.put("totalSorties", totalSorties);
        report.put("totalMaintenus", totalMaintenus);
        report.put("presenceMoyenne", presenceMoyenne);
        report.put("statsParFamille", statsParFamille);

        return report;
    }
}
