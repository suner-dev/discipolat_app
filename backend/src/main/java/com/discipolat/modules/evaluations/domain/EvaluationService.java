package com.discipolat.modules.evaluations.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final FamilyRepository familyRepository;
    private final SoulRepository soulRepository;
    private final SoulDepartmentRepository soulDepartmentRepository;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             SecurityUtils securityUtils,
                             UserRepository userRepository,
                             DepartmentRepository departmentRepository,
                             FamilyRepository familyRepository,
                             SoulRepository soulRepository,
                             SoulDepartmentRepository soulDepartmentRepository) {
        this.evaluationRepository = evaluationRepository;
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.familyRepository = familyRepository;
        this.soulRepository = soulRepository;
        this.soulDepartmentRepository = soulDepartmentRepository;
    }

    /**
     * Crée une évaluation (sans doublon) — flux « Évaluer » de l'onglet Évaluations.
     * L'évaluation reste anonyme pour l'évalué (seul l'agrégat est exposé).
     */
    @CacheEvict(value = "evaluationScores", allEntries = true)
    public Evaluation submit(UUID evalueId, CategorieEvaluation categorie, int note, String commentaire) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        checkEligible(currentUserId, evalueId, note);
        validateEvaluationRight(currentUserId, evalueId);
        if (evaluationRepository.existsByEvaluateurIdAndEvalueIdAndCategorie(currentUserId, evalueId, categorie)) {
            throw new BusinessRuleException(
                    "Vous avez déjà évalué cette personne dans cette catégorie — utilisez la modification", "ALREADY_EVALUATED");
        }
        Evaluation evaluation = Evaluation.builder()
                .evalueId(evalueId).evaluateurId(currentUserId)
                .categorie(categorie).note(note).commentaire(commentaire)
                .build();
        return evaluationRepository.save(evaluation);
    }

    /**
     * Crée si absente, modifie si existante — la « maîtrise » de l'évaluateur
     * sur SA propre évaluation. La catégorie par défaut est dérivée du rôle
     * de l'évalué (RESPONSABLE → RESPONSABLE, CHEF_DE_FAMILLE → CHEF_FAMILLE,
     * FAISEUR → FAISEUR, sinon MEMBRE).
     */
    @CacheEvict(value = "evaluationScores", allEntries = true)
    public Evaluation submitOrUpdate(UUID evalueId, CategorieEvaluation categorie, int note, String commentaire) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        checkEligible(currentUserId, evalueId, note);
        validateEvaluationRight(currentUserId, evalueId);

        CategorieEvaluation cat = categorie != null ? categorie : deriveCategory(evalueId);
        Evaluation evaluation = evaluationRepository
                .findByEvaluateurIdAndEvalueIdAndCategorie(currentUserId, evalueId, cat)
                .orElseGet(() -> Evaluation.builder()
                        .evalueId(evalueId).evaluateurId(currentUserId)
                        .categorie(cat).note(note).commentaire(commentaire)
                        .build());
        evaluation.setNote(note);
        evaluation.setCommentaire(commentaire);
        return evaluationRepository.save(evaluation);
    }

    private void checkEligible(UUID currentUserId, UUID evalueId, int note) {
        if (currentUserId.equals(evalueId)) {
            throw new BusinessRuleException("Vous ne pouvez pas vous évaluer vous-même", "SELF_EVALUATION");
        }
        if (note < 1 || note > 5) {
            throw new BusinessRuleException("La note doit être comprise entre 1 et 5", "NOTE_OUT_OF_RANGE");
        }
    }

    /** Catégorie dérivée du rôle de l'évalué. */
    private CategorieEvaluation deriveCategory(UUID evalueId) {
        return userRepository.findById(evalueId)
                .map(u -> switch (u.getRole()) {
                    case RESPONSABLE -> CategorieEvaluation.RESPONSABLE;
                    case CHEF_DE_FAMILLE -> CategorieEvaluation.CHEF_FAMILLE;
                    case FAISEUR -> CategorieEvaluation.FAISEUR;
                    default -> CategorieEvaluation.MEMBRE;
                })
                .orElse(CategorieEvaluation.MEMBRE);
    }

    /**
     * Droit d'évaluer : l'évalué doit exister et être — soit dans MON périmètre
     * d'encadrement (mes disciples, ma famille, mes départements), soit mon
     * supérieur (mon faiseur, mon chef de famille, mon responsable), soit un
     * utilisateur quelconque si je suis pasteur/administrateur.
     */
    private void validateEvaluationRight(UUID currentUserId, UUID evalueId) {
        userRepository.findById(evalueId)
                .orElseThrow(() -> new EntityNotFoundException("User", evalueId));

        // Super-utilisateur (pasteur / admin) : peut évaluer tout le monde
        if (securityUtils.isSuperUser()) return;

        // Bottom-up : l'évalué est mon supérieur (mon faiseur, mon chef, mon responsable)
        if (isMySuperior(currentUserId, evalueId)) return;

        // Top-down : l'évalué est dans mon périmètre d'encadrement
        if (isInMyScope(currentUserId, evalueId)) return;

        throw new BusinessRuleException(
                "Vous n'êtes pas autorisé à évaluer cette personne", "EVALUATION_NOT_ALLOWED");
    }

    private boolean isMySuperior(UUID currentUserId, UUID evalueId) {
        List<Soul> mySouls = soulRepository.findAllByUserId(currentUserId);
        if (mySouls.isEmpty()) return false;

        // L'évalué est mon faiseur
        if (mySouls.stream().anyMatch(s -> evalueId.equals(s.getFaiseurId()))) return true;

        // L'évalué est le chef de famille d'une de mes âmes
        for (Soul s : mySouls) {
            if (s.getFamilleId() == null) continue;
            Optional<Family> family = familyRepository.findById(s.getFamilleId());
            if (family.isPresent() && evalueId.equals(family.get().getChefFamilleId())) return true;
        }

        // L'évalué est le responsable d'un département qui contient une de mes âmes
        List<Department> depts = departmentRepository.findByResponsableId(evalueId);
        if (!depts.isEmpty()) {
            for (Soul s : mySouls) {
                for (Department d : depts) {
                    if (soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(s.getId(), d.getId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isInMyScope(UUID currentUserId, UUID evalueId) {
        List<Soul> evalueSouls = soulRepository.findAllByUserId(evalueId);
        if (evalueSouls.isEmpty()) return false;

        boolean responsable = securityUtils.hasActiveRole("RESPONSABLE");
        List<Department> myDepts = responsable ? departmentRepository.findByResponsableId(currentUserId) : List.of();
        UUID myFamilyId = securityUtils.hasActiveRole("CHEF_DE_FAMILLE")
                ? userRepository.findById(currentUserId).map(User::getFamilleGereeId).orElse(null)
                : null;
        boolean faiseur = securityUtils.hasActiveRole("FAISEUR");

        for (Soul evalueSoul : evalueSouls) {
            if (responsable && myDepts.stream()
                    .anyMatch(d -> soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(evalueSoul.getId(), d.getId()))) {
                return true;
            }
            if (myFamilyId != null && myFamilyId.equals(evalueSoul.getFamilleId())) return true;
            if (faiseur && currentUserId.equals(evalueSoul.getFaiseurId())) return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyEvaluations() {
        return getEvaluationsForUser(securityUtils.getCurrentUserId());
    }

    /**
     * Lightweight cached method: returns simple {moyenne, total} per category for a user.
     * Used by the batch /users/evaluation-scores endpoint to avoid N+1 queries.
     */
    @Cacheable(value = "evaluationScores", key = "#userId")
    public Map<String, Object> getUserEvalScores(UUID userId) {
        Map<String, Object> scores = new LinkedHashMap<>();
        for (CategorieEvaluation cat : CategorieEvaluation.values()) {
            Double avg = evaluationRepository.averageNoteByEvalueAndCategorie(userId, cat);
            long total = evaluationRepository.countByEvalueAndCategorie(userId, cat);
            if (avg != null || total > 0) {
                Map<String, Object> catScore = new LinkedHashMap<>();
                catScore.put("moyenne", avg != null ? Math.round(avg * 10.0) / 10.0 : null);
                catScore.put("total", total);
                scores.put(cat.name(), catScore);
            }
        }
        return scores;
    }

    @Transactional(readOnly = true)
    public Page<Evaluation> getMyEvaluationsList(String categorie, Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (categorie != null && !categorie.isEmpty()) {
            return evaluationRepository.findByEvalueIdAndCategorie(
                    currentUserId, CategorieEvaluation.valueOf(categorie), pageable);
        }
        return evaluationRepository.findByEvalueId(currentUserId, pageable);
    }

    /**
     * MES évaluations d'un utilisateur donné (pour pré-remplir le formulaire
     * « donner / modifier » dans la fiche de l'utilisateur).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyEvaluationsFor(UUID evalueId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        validateEvaluationRight(currentUserId, evalueId);
        return evaluationRepository.findByEvaluateurIdAndEvalueId(currentUserId, evalueId).stream()
                .map(e -> Map.<String, Object>of(
                        "categorie", e.getCategorie().name(),
                        "note", e.getNote(),
                        "commentaire", e.getCommentaire() != null ? e.getCommentaire() : "",
                        "date", e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : e.getCreatedAt().toString()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEvaluationsForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // Lecture limitée au périmètre : seuls les utilisateurs autorisés à
        // évaluer cette personne (ou les super-utilisateurs) voient ses stats.
        // Exception : MES propres évaluations (anonymisées) — toujours autorisé,
        // sinon « /evaluations/me » échouerait en 422 (auto-évaluation interdite).
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!securityUtils.isSuperUser() && !userId.equals(currentUserId)) {
            validateEvaluationRight(currentUserId, userId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("firstName", user.getFirstName());
        result.put("lastName", user.getLastName());
        result.put("role", user.getRole().name());

        Map<String, Object> statsByCategory = new LinkedHashMap<>();
        for (CategorieEvaluation cat : CategorieEvaluation.values()) {
            List<Evaluation> evals = evaluationRepository.findByEvalueIdAndCategorie(userId, cat);
            if (!evals.isEmpty()) {
                Double avg = evaluationRepository.averageNoteByEvalueAndCategorie(userId, cat);
                Map<String, Object> catStats = new LinkedHashMap<>();
                catStats.put("moyenne", avg != null ? Math.round(avg * 10.0) / 10.0 : null);
                catStats.put("total", evals.size());
                catStats.put("repartition", getNoteRepartition(evals));
                List<Map<String, Object>> comments = new ArrayList<>();
                for (Evaluation e : evals) {
                    if (e.getCommentaire() != null && !e.getCommentaire().isEmpty()) {
                        comments.add(Map.of(
                                "note", e.getNote(),
                                "commentaire", e.getCommentaire(),
                                "date", e.getCreatedAt().toString()));
                    }
                }
                catStats.put("commentaires", comments);
                statsByCategory.put(cat.name(), catStats);
            }
        }
        result.put("statistiques", statsByCategory);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAllEvaluationsAggregated() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (CategorieEvaluation cat : CategorieEvaluation.values()) {
            List<Evaluation> evals = evaluationRepository.findByCategorie(cat, Pageable.unpaged()).getContent();
            if (evals.isEmpty()) continue;

            double sum = 0;
            for (Evaluation e : evals) sum += e.getNote();
            double avg = sum / evals.size();

            Map<String, Object> catData = new LinkedHashMap<>();
            catData.put("moyenneGlobale", Math.round(avg * 10.0) / 10.0);
            catData.put("totalEvaluations", evals.size());
            catData.put("repartitionGlobale", getNoteRepartition(evals));

            // Group by evalueId
            Map<UUID, List<Evaluation>> byEvalue = new HashMap<>();
            for (Evaluation e : evals) {
                byEvalue.computeIfAbsent(e.getEvalueId(), k -> new ArrayList<>()).add(e);
            }

            List<Map<String, Object>> perPerson = new ArrayList<>();
            for (Map.Entry<UUID, List<Evaluation>> entry : byEvalue.entrySet()) {
                User u = userRepository.findById(entry.getKey()).orElse(null);
                if (u == null) continue;
                List<Evaluation> personEvals = entry.getValue();
                double pSum = 0;
                for (Evaluation pe : personEvals) pSum += pe.getNote();
                double pAvg = pSum / personEvals.size();
                Map<String, Object> personData = new LinkedHashMap<>();
                personData.put("id", u.getId());
                personData.put("nom", u.getFirstName() + " " + u.getLastName());
                personData.put("moyenne", Math.round(pAvg * 10.0) / 10.0);
                personData.put("total", personEvals.size());
                perPerson.add(personData);
            }
            perPerson.sort((a, b) -> Double.compare((Double) b.get("moyenne"), (Double) a.get("moyenne")));
            catData.put("parPersonne", perPerson);
            result.put(cat.name(), catData);
        }
        return result;
    }

    private Map<Integer, Long> getNoteRepartition(List<Evaluation> evals) {
        Map<Integer, Long> repartition = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            final int note = i;
            repartition.put(i, evals.stream().filter(e -> e.getNote() == note).count());
        }
        return repartition;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPeopleToEvaluate() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User", currentUserId));
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> added = new HashSet<>();

        // 1. FAISEUR: disciples (linked via soul.userId) evaluating their faiseur
        List<Soul> soulsAsDisciple = soulRepository.findAllByUserId(currentUserId);
        Set<UUID> myFaiseurIds = new HashSet<>();
        for (Soul s : soulsAsDisciple) {
            if (s.getFaiseurId() != null && !s.getFaiseurId().equals(currentUserId)) {
                myFaiseurIds.add(s.getFaiseurId());
            }
        }
        for (UUID faiseurId : myFaiseurIds) {
            String key = faiseurId + "_FAISEUR";
            if (added.contains(key)) continue;
            if (evaluationRepository.existsByEvaluateurIdAndEvalueIdAndCategorie(
                    currentUserId, faiseurId, CategorieEvaluation.FAISEUR)) continue;
            User faiseur = userRepository.findById(faiseurId).orElse(null);
            if (faiseur == null) continue;
            result.add(Map.of("id", faiseur.getId(), "nom", faiseur.getFirstName() + " " + faiseur.getLastName(),
                    "role", "FAISEUR", "categorie", "FAISEUR"));
            added.add(key);
        }

        // 2. CHEF_FAMILLE: faiseurs evaluating their chef
        List<Soul> soulsManaged = soulRepository.findAllByFaiseurId(currentUserId);
        Set<UUID> chefIds = new HashSet<>();
        for (Soul s : soulsManaged) {
            if (s.getFamilleId() != null) {
                Family fam = familyRepository.findById(s.getFamilleId()).orElse(null);
                if (fam != null) chefIds.add(fam.getChefFamilleId());
            }
        }
        for (UUID chefId : chefIds) {
            if (chefId.equals(currentUserId)) continue;
            String key = chefId + "_CHEF_FAMILLE";
            if (added.contains(key)) continue;
            if (evaluationRepository.existsByEvaluateurIdAndEvalueIdAndCategorie(
                    currentUserId, chefId, CategorieEvaluation.CHEF_FAMILLE)) continue;
            User chef = userRepository.findById(chefId).orElse(null);
            if (chef == null) continue;
            result.add(Map.of("id", chef.getId(), "nom", chef.getFirstName() + " " + chef.getLastName(),
                    "role", "CHEF_FAMILLE", "categorie", "CHEF_FAMILLE"));
            added.add(key);
        }

        // 3. RESPONSABLE: faiseurs evaluating their department responsable
        Set<UUID> respIds = new HashSet<>();
        for (Department dept : departmentRepository.findByResponsableId(currentUser.getId())) {
            respIds.add(dept.getResponsableId());
        }
        for (UUID respId : respIds) {
            if (respId.equals(currentUserId)) continue;
            String key = respId + "_RESPONSABLE";
            if (added.contains(key)) continue;
            if (evaluationRepository.existsByEvaluateurIdAndEvalueIdAndCategorie(
                    currentUserId, respId, CategorieEvaluation.RESPONSABLE)) continue;
            User resp = userRepository.findById(respId).orElse(null);
            if (resp == null) continue;
            result.add(Map.of("id", resp.getId(), "nom", resp.getFirstName() + " " + resp.getLastName(),
                    "role", "RESPONSABLE", "categorie", "RESPONSABLE"));
            added.add(key);
        }

        return result;
    }
}
