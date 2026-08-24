package com.discipolat.modules.skillsMatrix.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkillsMatrixService {

    private final SkillEvaluationRepository repository;

    public SkillsMatrixService(SkillEvaluationRepository repository) {
        this.repository = repository;
    }

    public List<SkillEvaluation> listByMember(UUID membreId) {
        return repository.findByMembreId(membreId);
    }

    public List<SkillEvaluation> listAll() {
        return repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    public SkillEvaluation evaluate(UUID membreId, String compétence, String niveau, UUID evaluateurId, String commentaire) {
        // Find existing or create new
        List<SkillEvaluation> existing = repository.findByMembreId(membreId);
        Optional<SkillEvaluation> found = existing.stream()
                .filter(e -> e.getCompétence().equals(compétence))
                .findFirst();

        SkillEvaluation eval = found.orElse(new SkillEvaluation());
        if (found.isEmpty()) {
            eval.setTenantId(TenantContext.getCurrentTenantId());
            eval.setMembreId(membreId);
            eval.setCompétence(compétence);
        }
        eval.setNiveau(SkillEvaluation.Niveau.valueOf(niveau));
        eval.setÉvaluéPar(evaluateurId);
        eval.setCommentaire(commentaire);
        eval.setUpdatedAt(LocalDateTime.now());
        return repository.save(eval);
    }

    public Map<String, Object> getMatrix(UUID membreId) {
        List<SkillEvaluation> evals = repository.findByMembreId(membreId);
        Map<String, Object> matrix = new HashMap<>();
        matrix.put("membreId", membreId);
        matrix.put("compétences", evals.stream().collect(Collectors.toMap(
                SkillEvaluation::getCompétence,
                e -> Map.of("niveau", e.getNiveau().name(), "commentaire", e.getCommentaire() != null ? e.getCommentaire() : "")
        )));
        matrix.put("totalCompétences", evals.size());
        matrix.put("niveauMoyen", evals.isEmpty() ? "N/A" : calculateAverageNiveau(evals));
        return matrix;
    }

    public Map<String, Object> getDepartmentMatrix(UUID departmentId) {
        List<SkillEvaluation> all = repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
        Map<UUID, List<SkillEvaluation>> byMember = all.stream()
                .collect(Collectors.groupingBy(SkillEvaluation::getMembreId));

        Map<String, Object> result = new HashMap<>();
        result.put("totalMembres", byMember.size());
        result.put("compétencesÉvaluées", all.size());
        result.put("gaps", findSkillGaps(all));
        return result;
    }

    private String calculateAverageNiveau(List<SkillEvaluation> evals) {
        int total = evals.stream().mapToInt(e -> e.getNiveau().ordinal()).sum();
        int avg = total / evals.size();
        return SkillEvaluation.Niveau.values()[Math.min(avg, SkillEvaluation.Niveau.values().length - 1)].name();
    }

    private List<Map<String, Object>> findSkillGaps(List<SkillEvaluation> evals) {
        Map<String, List<SkillEvaluation>> bySkill = evals.stream()
                .collect(Collectors.groupingBy(SkillEvaluation::getCompétence));

        List<Map<String, Object>> gaps = new ArrayList<>();
        for (Map.Entry<String, List<SkillEvaluation>> entry : bySkill.entrySet()) {
            long experts = entry.getValue().stream().filter(e -> e.getNiveau() == SkillEvaluation.Niveau.EXPERT).count();
            long débutants = entry.getValue().stream().filter(e -> e.getNiveau() == SkillEvaluation.Niveau.DÉBUTANT).count();
            if (débutants > experts) {
                gaps.add(Map.of("compétence", entry.getKey(), "débutants", débutants, "experts", experts, "gap", débutants - experts));
            }
        }
        return gaps;
    }
}
