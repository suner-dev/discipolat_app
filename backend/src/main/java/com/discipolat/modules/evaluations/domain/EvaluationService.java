package com.discipolat.modules.evaluations.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.souls.domain.Soul;
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

    public EvaluationService(EvaluationRepository evaluationRepository,
                             SecurityUtils securityUtils,
                             UserRepository userRepository,
                             DepartmentRepository departmentRepository,
                             FamilyRepository familyRepository,
                             SoulRepository soulRepository) {
        this.evaluationRepository = evaluationRepository;
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.familyRepository = familyRepository;
        this.soulRepository = soulRepository;
    }

    @CacheEvict(value = "evaluationScores", allEntries = true)
    public Evaluation submit(UUID evalueId, CategorieEvaluation categorie, int note, String commentaire) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId.equals(evalueId)) {
            throw new IllegalArgumentException("Vous ne pouvez pas vous évaluer vous-même");
        }
        if (note < 1 || note > 5) {
            throw new IllegalArgumentException("La note doit être comprise entre 1 et 5");
        }
        validateEvaluationRight(currentUserId, evalueId, categorie);
        if (evaluationRepository.existsByEvaluateurIdAndEvalueIdAndCategorie(currentUserId, evalueId, categorie)) {
            throw new IllegalArgumentException("Vous avez déjà évalué cette personne dans cette catégorie");
        }
        Evaluation evaluation = Evaluation.builder()
                .evalueId(evalueId).evaluateurId(currentUserId)
                .categorie(categorie).note(note).commentaire(commentaire)
                .build();
        return evaluationRepository.save(evaluation);
    }

    private void validateEvaluationRight(UUID currentUserId, UUID evalueId, CategorieEvaluation categorie) {
        userRepository.findById(evalueId)
                .orElseThrow(() -> new EntityNotFoundException("User", evalueId));

        switch (categorie) {
            case RESPONSABLE -> {
                List<Department> depts = departmentRepository.findByResponsableId(evalueId);
                boolean authorized = depts.stream().anyMatch(dept -> {
                    List<Family> families = familyRepository.findByDepartementId(dept.getId());
                    List<UUID> familyIds = families.stream().map(Family::getId).toList();
                    if (familyIds.isEmpty()) return false;
                    return soulRepository.findByFamilleIdIn(familyIds).stream()
                            .anyMatch(s -> s.getFaiseurId().equals(currentUserId));
                });
                if (!authorized) {
                    throw new IllegalArgumentException("Vous n'êtes pas membre de ce département");
                }
            }
            case CHEF_FAMILLE -> {
                List<Family> families = familyRepository.findByChefFamilleId(evalueId);
                boolean authorized = families.stream().anyMatch(f ->
                        soulRepository.findAllByFamilleId(f.getId()).stream()
                                .anyMatch(s -> s.getFaiseurId().equals(currentUserId)));
                if (!authorized) {
                    throw new IllegalArgumentException("Vous n'êtes pas dans la famille de ce chef");
                }
            }
            case FAISEUR -> {
                // Disciples evaluate their faiseur via soul.userId -> currentUserId link
                List<Soul> disciples = soulRepository.findAllByFaiseurId(evalueId);
                boolean authorized = disciples.stream()
                        .anyMatch(s -> currentUserId.equals(s.getUserId()));
                if (!authorized) {
                    throw new IllegalArgumentException("Ce faiseur ne vous suit pas. Seuls les disciples liés à un compte utilisateur peuvent évaluer.");
                }
            }
        }
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

    @Transactional(readOnly = true)
    public Map<String, Object> getEvaluationsForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

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

        // 2. RESPONSABLE: department members evaluating their responsable
        Set<UUID> deptIds = new HashSet<>();
        for (Soul s : soulsManaged) {
            if (s.getFamilleId() != null) {
                Family fam = familyRepository.findById(s.getFamilleId()).orElse(null);
                if (fam != null) deptIds.add(fam.getDepartementId());
            }
        }
        if (currentUser.getFamilleGereeId() != null) {
            Family fam = familyRepository.findById(currentUser.getFamilleGereeId()).orElse(null);
            if (fam != null) deptIds.add(fam.getDepartementId());
        }
        Set<UUID> respIds = new HashSet<>();
        for (UUID deptId : deptIds) {
            Department dept = departmentRepository.findById(deptId).orElse(null);
            if (dept != null) respIds.add(dept.getResponsableId());
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
