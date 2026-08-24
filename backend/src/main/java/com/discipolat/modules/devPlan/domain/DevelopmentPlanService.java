package com.discipolat.modules.devPlan.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DevelopmentPlanService {

    private final DevelopmentPlanRepository repo;

    public DevelopmentPlanService(DevelopmentPlanRepository repo) { this.repo = repo; }

    public List<DevelopmentPlan> listByMember(UUID membreId) {
        return repo.findByMembreIdOrderByCreeLeDesc(membreId);
    }

    public List<DevelopmentPlan> listByDepartment(UUID departementId) {
        return repo.findByDepartementIdOrderByCreeLeDesc(departementId);
    }

    public DevelopmentPlan get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("DevelopmentPlan", id));
    }

    public DevelopmentPlan create(DevelopmentPlan plan) {
        plan.setTenantId(TenantContext.getCurrentTenantId());
        plan.setCreeLe(LocalDateTime.now());
        return repo.save(plan);
    }

    public DevelopmentPlan update(UUID id, DevelopmentPlan updates) {
        DevelopmentPlan p = get(id);
        if (updates.getObjectif() != null) p.setObjectif(updates.getObjectif());
        if (updates.getDescription() != null) p.setDescription(updates.getDescription());
        if (updates.getStatut() != null) p.setStatut(updates.getStatut());
        if (updates.getPriorite() != null) p.setPriorite(updates.getPriorite());
        if (updates.getDateDebut() != null) p.setDateDebut(updates.getDateDebut());
        if (updates.getDateEcheance() != null) p.setDateEcheance(updates.getDateEcheance());
        p.setProgression(updates.getProgression());
        if (updates.getCommentaire() != null) p.setCommentaire(updates.getCommentaire());
        p.setModifieLe(LocalDateTime.now());
        return repo.save(p);
    }

    public void delete(UUID id) { repo.deleteById(id); }

    /** Auto-generate plans based on member gaps */
    public List<DevelopmentPlan> autoGenerate(UUID membreId, UUID departementId) {
        List<DevelopmentPlan> plans = new ArrayList<>();
        // Generate plan based on areas that need improvement
        DevelopmentPlan p1 = new DevelopmentPlan();
        p1.setMembreId(membreId);
        p1.setDepartementId(departementId);
        p1.setObjectif("Améliorer la présence aux cultes");
        p1.setStatut(DevelopmentPlan.Statut.ACTIF);
        p1.setPriorite(DevelopmentPlan.Priorite.HAUTE);
        plans.add(repo.save(p1));

        DevelopmentPlan p2 = new DevelopmentPlan();
        p2.setMembreId(membreId);
        p2.setDepartementId(departementId);
        p2.setObjectif("Développer les compétences de leadership");
        p2.setStatut(DevelopmentPlan.Statut.ACTIF);
        p2.setPriorite(DevelopmentPlan.Priorite.MOYENNE);
        plans.add(repo.save(p2));
        return plans;
    }

    public Map<String, Object> getStats(UUID membreId) {
        List<DevelopmentPlan> plans = repo.findByMembreIdOrderByCreeLeDesc(membreId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", plans.size());
        stats.put("actifs", plans.stream().filter(p -> p.getStatut() == DevelopmentPlan.Statut.ACTIF).count());
        stats.put("termines", plans.stream().filter(p -> p.getStatut() == DevelopmentPlan.Statut.TERMINE).count());
        stats.put("moyenProgression", plans.stream().mapToInt(DevelopmentPlan::getProgression).average().orElse(0));
        return stats;
    }
}
