package com.discipolat.modules.skillsMatrix.domain;

import com.discipolat.common.infrastructure.api.TenantContext;
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
        return repository.findByTenantId(TenantContext.getCurrentTenantId());
    }

    public List<SkillEvaluation> getByMembre(UUID membreId) {
        return repository.findByTenantIdAndMembreId(TenantContext.getCurrentTenantId(), membreId);
    }

    public SkillEvaluation create(UUID membreId, String competence, String niveau, String commentaire, UUID evalPar) {
        SkillEvaluation eval = new SkillEvaluation();
        eval.setTenantId(TenantContext.getCurrentTenantId());
        eval.setMembreId(membreId);
        eval.setCompetence(competence);
        eval.setNiveau(SkillEvaluation.Niveau.valueOf(niveau));
        eval.setCommentaire(commentaire);
        eval.setEvaluePar(evalPar);
        return repository.save(eval);
    }

    public Map<String, Object> getMatrix() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<SkillEvaluation> evaluations = repository.findByTenantId(tenantId);

        Map<String, Map<String, Long>> matrix = evaluations.stream()
                .collect(Collectors.groupingBy(
                        SkillEvaluation::getCompetence,
                        Collectors.groupingBy(e -> e.getNiveau().name(), Collectors.counting())
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("matrix", matrix);
        result.put("competences", matrix.keySet());
        result.put("totalEvaluations", evaluations.size());
        return result;
    }
}
