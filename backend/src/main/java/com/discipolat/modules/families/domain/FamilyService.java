package com.discipolat.modules.families.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.StatutEntite;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final UserRepository userRepository;
    private final MakerReportRepository makerReportRepository;
    private final SecurityUtils securityUtils;

    public FamilyService(FamilyRepository familyRepository,
                         FamilyChiefHistoryRepository chiefHistoryRepository,
                         SoulRepository soulRepository,
                         UserRepository userRepository,
                         MakerReportRepository makerReportRepository,
                         SecurityUtils securityUtils) {
        this.familyRepository = familyRepository;
        this.chiefHistoryRepository = chiefHistoryRepository;
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.makerReportRepository = makerReportRepository;
        this.securityUtils = securityUtils;
    }

    public Family create(Family family) {
        // US-06: Check for unique family name
        if (familyRepository.findByNom(family.getNom()).isPresent()) {
            throw new BusinessRuleException("Une famille avec ce nom existe déjà: " + family.getNom(),
                    "DUPLICATE_FAMILY_NAME");
        }
        family.setDateCreation(LocalDate.now());
        family.setStatut(StatutEntite.ACTIVE);
        return familyRepository.save(family);
    }

    @Transactional(readOnly = true)
    public Family findById(UUID id) {
        return familyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Family", id));
    }

    @Transactional(readOnly = true)
    public Page<Family> findAll(Pageable pageable) {
        return familyRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Family> findByDepartement(UUID departementId, Pageable pageable) {
        return familyRepository.findByDepartementId(departementId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Family> findByChefFamille(UUID chefId) {
        return familyRepository.findByChefFamilleId(chefId);
    }

    public Page<Family> findByChefFamille(UUID chefId, Pageable pageable) {
        return familyRepository.findByChefFamilleId(chefId, pageable);
    }

    public Family update(Family updated) {
        Family existing = findById(updated.getId());
        existing.setNom(updated.getNom());
        existing.setChefFamilleId(updated.getChefFamilleId());
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

    // ======================== US-07: REASSIGN CHIEF WITH HISTORY ========================

    public void reassignChef(UUID familyId, UUID newChefId) {
        Family family = findById(familyId);
        UUID oldChefId = family.getChefFamilleId();
        UUID currentUserId = securityUtils.getCurrentUserId();

        family.setChefFamilleId(newChefId);
        familyRepository.save(family);

        // US-07: Record chief change in history
        FamilyChiefHistory history = FamilyChiefHistory.builder()
                .familleId(familyId)
                .ancienChefId(oldChefId != null && oldChefId.equals(newChefId) ? null : oldChefId)
                .nouveauChefId(newChefId)
                .changedBy(currentUserId)
                .raison("Changement de chef de famille")
                .build();
        chiefHistoryRepository.save(history);

        // Update user chef status
        if (oldChefId != null) {
            userRepository.findById(oldChefId).ifPresent(oldChef -> {
                oldChef.setEstChefDeFamille(false);
                oldChef.setFamilleGereeId(null);
                userRepository.save(oldChef);
            });
        }
        userRepository.findById(newChefId).ifPresent(newChef -> {
            newChef.setEstChefDeFamille(true);
            newChef.setFamilleGereeId(familyId);
            userRepository.save(newChef);
        });
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
    public long countByDepartement(UUID departementId) {
        return familyRepository.countByDepartementIdAndStatut(departementId, StatutEntite.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<FamilyChiefHistory> getChiefHistory(UUID familyId) {
        return chiefHistoryRepository.findByFamilleIdOrderByCreatedAtDesc(familyId);
    }

    // ======================== FAISEUR PERFORMANCE (Chef de famille view) ========================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFaiseurPerformance(UUID familyId, LocalDate semaine) {
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
