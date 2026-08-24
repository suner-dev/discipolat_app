package com.discipolat.modules.succession.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SuccessionPlanService {

    private final SuccessionPlanRepository repository;

    public SuccessionPlanService(SuccessionPlanRepository repository) {
        this.repository = repository;
    }

    public List<SuccessionPlan> list() {
        return repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    public SuccessionPlan getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SuccessionPlan", id));
    }

    public SuccessionPlan create(UUID candidatId, String rôleCible, UUID mentorId, String planFormation) {
        SuccessionPlan plan = new SuccessionPlan();
        plan.setTenantId(TenantContext.getCurrentTenantId());
        plan.setCandidatId(candidatId);
        plan.setRôleCible(rôleCible);
        plan.setMentorId(mentorId);
        plan.setPlanFormation(planFormation);
        return repository.save(plan);
    }

    public SuccessionPlan updateReadiness(UUID id, String readiness) {
        SuccessionPlan plan = getById(id);
        plan.setReadiness(SuccessionPlan.Readiness.valueOf(readiness));
        if (readiness.equals("PRÊT")) {
            plan.setStatut(SuccessionPlan.Statut.PRÊT);
        }
        return repository.save(plan);
    }

    public SuccessionPlan updateStatut(UUID id, String statut) {
        SuccessionPlan plan = getById(id);
        plan.setStatut(SuccessionPlan.Statut.valueOf(statut));
        return repository.save(plan);
    }

    public void delete(UUID id) {
        repository.delete(getById(id));
    }
}
