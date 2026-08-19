package com.discipolat.modules.families.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.StatutEntite;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.api.CreateFamilyRequest;
import com.discipolat.modules.families.api.UpdateFamilyRequest;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyChiefHistoryRepository chiefHistoryRepository;
    private final SoulRepository soulRepository;
    private final SoulDepartmentRepository soulDepartmentRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final MakerReportRepository makerReportRepository;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;
    private final WorkspaceScopeService workspaceScopeService;

    public FamilyService(FamilyRepository familyRepository,
                         FamilyChiefHistoryRepository chiefHistoryRepository,
                         SoulRepository soulRepository,
                         SoulDepartmentRepository soulDepartmentRepository,
                         DepartmentRepository departmentRepository,
                         UserRepository userRepository,
                         MakerReportRepository makerReportRepository,
                         SecurityUtils securityUtils,
                         PasswordEncoder passwordEncoder,
                         WorkspaceScopeService workspaceScopeService) {
        this.familyRepository = familyRepository;
        this.chiefHistoryRepository = chiefHistoryRepository;
        this.soulRepository = soulRepository;
        this.soulDepartmentRepository = soulDepartmentRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.makerReportRepository = makerReportRepository;
        this.securityUtils = securityUtils;
        this.passwordEncoder = passwordEncoder;
        this.workspaceScopeService = workspaceScopeService;
    }

    /**
     * Création d'une famille avec 2 cas :
     * Cas 1 : Sélectionner un chef existant (chefFamilleId requis)
     * Cas 2 : Créer immédiatement un nouveau chef (createNewChef = true + infos)
     */
    public Family create(CreateFamilyRequest request) {
        // US-06: Check for unique family name
        if (familyRepository.findByNom(request.nom()).isPresent()) {
            throw new BusinessRuleException("Une famille avec ce nom existe déjà: " + request.nom(),
                    "DUPLICATE_FAMILY_NAME");
        }

        UUID chefFamilleId;

        // Cas 2 : Créer un nouveau chef
        if (request.shouldCreateNewChef()) {
            chefFamilleId = createNewChef(request);
        } else if (request.chefFamilleId() != null) {
            // Cas 1 : Sélectionner un chef existant
            chefFamilleId = request.chefFamilleId();
            User chef = userRepository.findById(chefFamilleId)
                    .orElseThrow(() -> new EntityNotFoundException("User", chefFamilleId));
            chef.setEstChefDeFamille(true);
            userRepository.save(chef);
        } else {
            throw new BusinessRuleException(
                    "Vous devez sélectionner un chef existant ou créer un nouveau chef.",
                    "NO_CHIEF_SELECTED");
        }

        Family family = Family.builder()
                .nom(request.nom())
                .chefFamilleId(chefFamilleId)
                .userId(chefFamilleId)
                .chefAdjointId(request.chefAdjointId())
                .dateCreation(LocalDate.now())
                .statut(StatutEntite.ACTIVE)
                .build();

        Family savedFamily = familyRepository.save(family);

        // Mettre à jour le user chef de famille
        final UUID finalChefId = chefFamilleId;
        userRepository.findById(finalChefId).ifPresent(chef -> {
            chef.setEstChefDeFamille(true);
            chef.setFamilleGereeId(savedFamily.getId());
            userRepository.save(chef);
        });

        // Enregistrer dans l'historique
        FamilyChiefHistory history = FamilyChiefHistory.builder()
                .familleId(savedFamily.getId())
                .ancienChefId(null)
                .nouveauChefId(finalChefId)
                .changedBy(securityUtils.getCurrentUserId())
                .raison("Création de la famille")
                .build();
        chiefHistoryRepository.save(history);

        return savedFamily;
    }

    /**
     * Crée un nouveau chef de famille à partir des informations fournies.
     */
    private UUID createNewChef(CreateFamilyRequest request) {
        // Vérifier que l'email n'existe pas déjà
        if (userRepository.findByEmail(request.newChefEmail()).isPresent()) {
            throw new BusinessRuleException(
                    "Un compte avec cet email existe déjà: " + request.newChefEmail(),
                    "DUPLICATE_EMAIL");
        }

        // Générer un mot de passe temporaire
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        String hashedPassword = passwordEncoder.encode(tempPassword);

        User newChef = User.builder()
                .email(request.newChefEmail())
                .passwordHash(hashedPassword)
                .firstName(request.newChefFirstName())
                .lastName(request.newChefLastName())
                .phone(request.newChefPhone())
                .role(UserRole.CHEF_DE_FAMILLE)
                .roles(new HashSet<>(Set.of(UserRole.CHEF_DE_FAMILLE)))
                .estChefDeFamille(true)
                .statut(com.discipolat.modules.users.domain.UserStatus.ACTIVE)
                .build();

        newChef = userRepository.save(newChef);
        return newChef.getId();
    }

    @Transactional(readOnly = true)
    public Family findById(UUID id) {
        Family family = familyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Family", id));
        // Espace métier : un rôle opérationnel ne peut consulter que les familles
        // de son propre espace (chef : sa famille, responsable : ses départements,
        // faiseur : ses disciples). Les super-utilisateurs (admin/pasteur) voient tout.
        if (!securityUtils.isSuperUser() && !getVisibleFamilyIds().contains(id)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé à cette famille dans l'espace métier courant");
        }
        return family;
    }

    @Transactional(readOnly = true)
    public Page<Family> findAll(Pageable pageable) {
        if (securityUtils.isSuperUser()) {
            return familyRepository.findAll(pageable);
        }
        Set<UUID> visibleIds = getVisibleFamilyIds();
        if (visibleIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        return familyRepository.findAllByIdIn(visibleIds, pageable);
    }

    /**
     * Familles accessibles dans l'espace métier du rôle ACTIF courant.
     * Union dédupliquée (un utilisateur multi-rôles n'accumule QUE les familles
     * des espaces qu'il est capable d'administrer dans le rôle actif).
     */
    private Set<UUID> getVisibleFamilyIds() {
        UUID userId = securityUtils.getCurrentUserId();
        Set<UUID> familyIds = new HashSet<>();

        // Chef de famille actif : la famille qu'il gère
        if (securityUtils.hasActiveRole("CHEF_DE_FAMILLE")) {
            userRepository.findById(userId).ifPresent(u -> {
                if (u.getFamilleGereeId() != null) familyIds.add(u.getFamilleGereeId());
            });
            familyRepository.findByChefFamilleId(userId).stream()
                    .map(Family::getId).forEach(familyIds::add);
        }

        // Responsable actif : familles des membres de SES départements
        if (securityUtils.hasActiveRole("RESPONSABLE")) {
            List<UUID> deptIds = departmentRepository.findByResponsableId(userId)
                    .stream().map(Department::getId).toList();
            if (!deptIds.isEmpty()) {
                List<UUID> soulIds = soulDepartmentRepository.findByDepartmentIdIn(deptIds).stream()
                        .filter(SoulDepartment::isActif)
                        .map(SoulDepartment::getSoulId)
                        .distinct()
                        .toList();
                if (!soulIds.isEmpty()) {
                    soulRepository.findAllById(soulIds).stream()
                            .filter(s -> !s.isDeleted() && s.getFamilleId() != null)
                            .map(Soul::getFamilleId)
                            .forEach(familyIds::add);
                }
            }
        }

        // Faiseur actif : familles de ses disciples
        if (securityUtils.hasActiveRole("FAISEUR")) {
            soulRepository.findAllByFaiseurId(userId).stream()
                    .filter(s -> !s.isDeleted() && s.getFamilleId() != null)
                    .map(Soul::getFamilleId)
                    .forEach(familyIds::add);
        }

        return familyIds;
    }

    @Transactional(readOnly = true)
    public List<Family> findByChefFamille(UUID chefId) {
        if (!securityUtils.isSuperUser() && !workspaceScopeService.canAccessFaiseur(chefId)) {
            throw new com.discipolat.common.exception.ForbiddenException("You do not have access to this chef's families");
        }
        return familyRepository.findByChefFamilleId(chefId);
    }

    public Page<Family> findByChefFamille(UUID chefId, Pageable pageable) {
        if (!securityUtils.isSuperUser() && !workspaceScopeService.canAccessFaiseur(chefId)) {
            throw new com.discipolat.common.exception.ForbiddenException("You do not have access to this chef's families");
        }
        return familyRepository.findByChefFamilleId(chefId, pageable);
    }

    public Family update(UUID id, UpdateFamilyRequest request) {
        Family existing = findById(id);
        existing.setNom(request.nom());
        if (request.chefFamilleId() != null) {
            existing.setChefFamilleId(request.chefFamilleId());
        }
        if (request.chefAdjointId() != null) {
            existing.setChefAdjointId(request.chefAdjointId());
        }
        return familyRepository.save(existing);
    }

    // ======================== US-09: DELETE/DISSOLVE ========================

    public void delete(UUID id) {
        Family family = findById(id);
        // US-09: Check for active members before dissolving
        List<Soul> activeSouls = soulRepository.findAllByFamilleId(id).stream()
                .filter(s -> !s.isDeleted())
                .filter(s -> s.getStatut() == StatutAme.ACTIF || s.getStatut() == StatutAme.EN_INTEGRATION)
                .toList();
        if (!activeSouls.isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot dissolve family with " + activeSouls.size() + " active member(s). Please reassign them first.",
                    "FAMILY_HAS_ACTIVE_MEMBERS");
        }
        family.setStatut(StatutEntite.ARCHIVED);
        familyRepository.save(family);
    }

    // ======================== US-10: FAMILY HISTORY ========================

    @Transactional(readOnly = true)
    public Map<String, Object> getFamilyHistory(UUID familyId) {
        findById(familyId); // Ensure family exists
        Map<String, Object> history = new LinkedHashMap<>();

        // Chief change history
        List<FamilyChiefHistory> chiefChanges = chiefHistoryRepository
                .findByFamilleIdOrderByCreatedAtDesc(familyId);
        List<Map<String, Object>> chiefHistoryList = new ArrayList<>();
        for (FamilyChiefHistory change : chiefChanges) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", change.getCreatedAt());
            entry.put("ancienChefId", change.getAncienChefId());
            entry.put("nouveauChefId", change.getNouveauChefId());
            entry.put("raison", change.getRaison());
            chiefHistoryList.add(entry);
        }
        history.put("chefChanges", chiefHistoryList);

        // Past souls
        List<Soul> allSouls = soulRepository.findAllByFamilleId(familyId);
        List<Map<String, Object>> soulsList = new ArrayList<>();
        for (Soul soul : allSouls) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", soul.getId());
            entry.put("nom", soul.getNomComplet());
            entry.put("dateIntegration", soul.getDateIntegration());
            entry.put("statut", soul.getStatut());
            entry.put("actif", !soul.isDeleted());
            soulsList.add(entry);
        }
        history.put("souls", soulsList);

        // Members count
        history.put("totalMembres", allSouls.size());
        history.put("membresActifs", allSouls.stream().filter(s -> !s.isDeleted()).count());

        return history;
    }

    // ======================== US-08: TREE VIEW ========================

    @Transactional(readOnly = true)
    public Map<String, Object> getFamilyTree(UUID familyId) {
        Family family = findById(familyId);
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("familyId", family.getId());
        tree.put("nom", family.getNom());
        tree.put("chefFamilleId", family.getChefFamilleId());
        tree.put("chefAdjointId", family.getChefAdjointId());

        List<Soul> souls = soulRepository.findAllByFamilleId(familyId);

        // Group souls by faiseur
        Map<UUID, List<Map<String, Object>>> soulsByFaiseur = new LinkedHashMap<>();
        for (Soul soul : souls) {
            if (soul.isDeleted()) continue;
            soulsByFaiseur.computeIfAbsent(soul.getFaiseurId(), k -> new ArrayList<>());
            Map<String, Object> soulInfo = new LinkedHashMap<>();
            soulInfo.put("id", soul.getId());
            soulInfo.put("nom", soul.getNomComplet());
            soulInfo.put("statut", soul.getStatut());
            soulInfo.put("etatSpirituel", soul.getEtatSpirituel());
            soulInfo.put("dateIntegration", soul.getDateIntegration());
            soulsByFaiseur.get(soul.getFaiseurId()).add(soulInfo);
        }

        // Build faiseur list with their souls
        List<Map<String, Object>> faiseurs = new ArrayList<>();
        for (Map.Entry<UUID, List<Map<String, Object>>> entry : soulsByFaiseur.entrySet()) {
            Map<String, Object> faiseurInfo = new LinkedHashMap<>();
            faiseurInfo.put("faiseurId", entry.getKey());
            // Get faiseur name
            userRepository.findById(entry.getKey()).ifPresent(f -> {
                faiseurInfo.put("nom", f.getFirstName() + " " + f.getLastName());
            });
            faiseurInfo.put("nombreAmes", entry.getValue().size());
            faiseurInfo.put("ames", entry.getValue());
            faiseurs.add(faiseurInfo);
        }

        tree.put("faiseurs", faiseurs);
        tree.put("nombreTotalAmes", souls.stream().filter(s -> !s.isDeleted()).count());
        tree.put("nombreFaiseurs", faiseurs.size());

        return tree;
    }

    // ======================== US-11: COMPARE FAMILIES ========================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> compareFamilies(List<UUID> familyIds) {
        List<Map<String, Object>> comparisons = new ArrayList<>();

        for (UUID familyId : familyIds) {
            Family family = findById(familyId);
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("familyId", family.getId());
            stats.put("nom", family.getNom());

            List<Soul> souls = soulRepository.findAllByFamilleId(familyId);
            long activeSouls = souls.stream().filter(s -> !s.isDeleted())
                    .filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long enDifficulte = souls.stream().filter(s -> !s.isDeleted())
                    .filter(s -> "EN_DIFFICULTE".equals(s.getEtatSpirituel())).count();
            long enVeille = souls.stream().filter(s -> !s.isDeleted())
                    .filter(s -> s.getStatut() == StatutAme.EN_VEILLE).count();

            Set<UUID> faiseurIds = new HashSet<>();
            for (Soul soul : souls) {
                if (!soul.isDeleted()) faiseurIds.add(soul.getFaiseurId());
            }

            stats.put("totalAmes", souls.stream().filter(s -> !s.isDeleted()).count());
            stats.put("amesActives", activeSouls);
            stats.put("amesEnDifficulte", enDifficulte);
            stats.put("amesEnVeille", enVeille);
            stats.put("nombreFaiseurs", faiseurIds.size());
            stats.put("tauxRetention", souls.size() > 0
                    ? Math.round((double) activeSouls / souls.stream().filter(s -> !s.isDeleted()).count() * 100.0 * 10.0) / 10.0
                    : 0.0);

            comparisons.add(stats);
        }

        return comparisons;
    }

    @Transactional(readOnly = true)
    public List<FamilyChiefHistory> getChiefHistory(UUID familyId) {
        if (!securityUtils.isSuperUser() && !workspaceScopeService.canAccessFamily(familyId)) {
            throw new com.discipolat.common.exception.ForbiddenException("You do not have access to this family");
        }
        return chiefHistoryRepository.findByFamilleIdOrderByCreatedAtDesc(familyId);
    }

    // ======================== FAISEUR PERFORMANCE (Chef de famille view) ========================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFaiseurPerformance(UUID familyId, LocalDate semaine) {
        if (!securityUtils.isSuperUser() && !workspaceScopeService.canAccessFamily(familyId)) {
            throw new com.discipolat.common.exception.ForbiddenException("You do not have access to this family");
        }
        List<Soul> souls = soulRepository.findAllByFamilleId(familyId).stream()
                .filter(s -> !s.isDeleted())
                .toList();

        Map<UUID, List<Soul>> soulsByFaiseur = new LinkedHashMap<>();
        for (Soul soul : souls) {
            soulsByFaiseur.computeIfAbsent(soul.getFaiseurId(), k -> new ArrayList<>()).add(soul);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (Map.Entry<UUID, List<Soul>> entry : soulsByFaiseur.entrySet()) {
            UUID faiseurId = entry.getKey();
            List<Soul> faiseurSouls = entry.getValue();

            Optional<User> faiseur = userRepository.findById(faiseurId);

            int totalAmes = faiseurSouls.size();
            long actifs = faiseurSouls.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long enIntegration = faiseurSouls.stream().filter(s -> s.getStatut() == StatutAme.EN_INTEGRATION).count();
            long enVeille = faiseurSouls.stream().filter(s -> s.getStatut() == StatutAme.EN_VEILLE).count();

            List<MakerReport> reports = makerReportRepository.findByFaiseurIdAndSemaine(faiseurId, semaine);
            long soumis = reports.stream().filter(MakerReport::isSoumis).count();

            int totalPresents = 0;
            int totalPresences = 0;
            for (MakerReport r : reports) {
                if (r.getPresencesParCulte() != null) {
                    for (Boolean p : r.getPresencesParCulte().values()) {
                        totalPresences++;
                        if (p) totalPresents++;
                    }
                }
            }
            BigDecimal tauxPresence = totalPresences > 0
                    ? BigDecimal.valueOf(totalPresents).multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(totalPresences), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal tauxSoumission = totalAmes > 0
                    ? BigDecimal.valueOf(soumis).multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(totalAmes), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> perf = new LinkedHashMap<>();
            perf.put("faiseurId", faiseurId);
            perf.put("faiseurNom", faiseur.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("N/A"));
            perf.put("totalAmes", totalAmes);
            perf.put("actifs", actifs);
            perf.put("enIntegration", enIntegration);
            perf.put("enVeille", enVeille);
            perf.put("rapportsSoumis", soumis);
            perf.put("tauxSoumission", tauxSoumission);
            perf.put("tauxPresence", tauxPresence);
            perf.put("totalPresents", totalPresents);

            results.add(perf);
        }

        results.sort((a, b) -> ((String) a.get("faiseurNom")).compareTo((String) b.get("faiseurNom")));
        return results;
    }

    // ======================== US-60: RESTORE FAMILY ========================

    public Family restore(UUID id) {
        Family family = familyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Family", id));
        family.setStatut(StatutEntite.ACTIVE);
        return familyRepository.save(family);
    }
}
