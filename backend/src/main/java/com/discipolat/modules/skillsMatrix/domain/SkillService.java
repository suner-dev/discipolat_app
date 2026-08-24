package com.discipolat.modules.skillsMatrix.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkillService {

    private final SkillEvaluationRepository repository;

    public SkillService(SkillEvaluationRepository repository) {
        this.repository = repository;
    }

    public List<SkillEvaluation> listAll() {
        return repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    public List<SkillEvaluation> getByMembre(UUID membreId) {
        return repository.findByMembreId(membreId);
    }

    public SkillEvaluation create(UUID membreId, String competence, String niveau, String commentaire, UUID evalPar) {
        SkillEvaluation eval = new SkillEvaluation();
        eval.setTenantId(TenantContext.getCurrentTenantId());
        eval.setMembreId(membreId);
        eval.setCompétence(competence);
        eval.setNiveau(SkillEvaluation.Niveau.valueOf(niveau));
        eval.setCommentaire(commentaire);
        eval.setÉvaluéPar(evalPar);
        return repository.save(eval);
    }

    public Map<String, Object> getMatrix() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<SkillEvaluation> evaluations = repository.findByTenantIdOrderByCreatedAtDesc(tenantId);

        Map<String, Map<String, Long>> matrix = evaluations.stream()
                .collect(Collectors.groupingBy(
                        SkillEvaluation::getCompétence,
                        Collectors.groupingBy(e -> e.getNiveau().name(), Collectors.counting())
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("matrix", matrix);
        result.put("competences", matrix.keySet());
        result.put("totalEvaluations", evaluations.size());
        return result;
    }
}
